package com.zenith;

import ch.qos.logback.classic.LoggerContext;
import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.mc.protocol.MinecraftConstants;
import com.github.steveice10.mc.protocol.MinecraftProtocol;
import com.github.steveice10.mc.protocol.data.game.level.sound.BuiltinSound;
import com.github.steveice10.mc.protocol.data.game.level.sound.SoundCategory;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundSystemChatPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundTabListPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.level.ClientboundSoundPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.title.ClientboundSetActionBarTextPacket;
import com.github.steveice10.packetlib.BuiltinFlags;
import com.github.steveice10.packetlib.tcp.TcpServer;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.zenith.cache.data.PlayerCache;
import com.zenith.event.Subscription;
import com.zenith.event.proxy.*;
import com.zenith.feature.autoupdater.AutoUpdater;
import com.zenith.feature.autoupdater.GitAutoUpdater;
import com.zenith.feature.autoupdater.RestAutoUpdater;
import com.zenith.feature.queue.Queue;
import com.zenith.network.client.Authenticator;
import com.zenith.network.client.ClientSession;
import com.zenith.network.server.CustomServerInfoBuilder;
import com.zenith.network.server.ProxyServerListener;
import com.zenith.network.server.ServerConnection;
import com.zenith.network.server.handler.ProxyServerLoginHandler;
import com.zenith.util.ComponentSerializer;
import com.zenith.util.Config;
import com.zenith.util.Wait;
import com.zenith.via.ProtocolVersionDetector;
import com.zenith.via.ZViaServerProxyPlatform;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;
import reactor.netty.http.client.HttpClient;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static com.zenith.Shared.*;
import static com.zenith.event.SimpleEventBus.pair;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;


@Getter
public class Proxy {
    @Getter
    protected static Proxy instance;
    protected MinecraftProtocol protocol;
    protected ClientSession client;
    protected TcpServer server;
    protected Authenticator authenticator;
    @Setter
    protected byte[] serverIcon;
    protected final AtomicReference<ServerConnection> currentPlayer = new AtomicReference<>();
    protected final CopyOnWriteArraySet<ServerConnection> activeConnections = new CopyOnWriteArraySet<>();
    private int reconnectCounter;
    private boolean inQueue = false;
    private int queuePosition = 0;
    private Instant connectTime;
    private Instant disconnectTime = Instant.now();
    private Optional<Boolean> isPrio = Optional.empty();
    private Optional<Boolean> isPrioBanned = Optional.empty();
    volatile private Optional<Future<?>> autoReconnectFuture = Optional.empty();
    private Instant lastActiveHoursConnect = Instant.EPOCH;
    private AtomicBoolean loggingIn = new AtomicBoolean(false);
    @Getter
    @Setter
    private AutoUpdater autoUpdater;
    private Subscription eventSubscription;

    public static void main(String... args) {
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();
        instance = new Proxy();
        instance.start();
    }

    @SuppressWarnings("unchecked")
    public void initEventHandlers() {
        if (eventSubscription != null) throw new RuntimeException("Event handlers already initialized");
        eventSubscription = EVENT_BUS.subscribe(
            pair(DisconnectEvent.class, this::handleDisconnectEvent),
            pair(ConnectEvent.class, this::handleConnectEvent),
            pair(StartQueueEvent.class, this::handleStartQueueEvent),
            pair(QueuePositionUpdateEvent.class, this::handleQueuePositionUpdateEvent),
            pair(QueueCompleteEvent.class, this::handleQueueCompleteEvent),
            pair(PlayerOnlineEvent.class, this::handlePlayerOnlineEvent),
            pair(ProxyClientDisconnectedEvent.class, this::handleProxyClientDisconnectedEvent),
            pair(ServerRestartingEvent.class, this::handleServerRestartingEvent),
            pair(PrioStatusEvent.class, this::handlePrioStatusEvent),
            pair(ServerPlayerConnectedEvent.class, this::handleServerPlayerConnectedEvent),
            pair(ServerPlayerDisconnectedEvent.class, this::handleServerPlayerDisconnectedEvent)
        );
    }

    public void start() {
        loadConfig();
        loadLaunchConfig();
        DEFAULT_LOG.info("Starting ZenithProxy-{}", LAUNCH_CONFIG.version);
        initEventHandlers();
        try {
            if (CONFIG.interactiveTerminal.enable) {
                TERMINAL_MANAGER.start();
            }
            if (CONFIG.database.enabled) {
                DATABASE_MANAGER.start();
                DEFAULT_LOG.info("Started Databases");
            }
            if (CONFIG.discord.enable) {
                boolean err = false;
                try {
                    DISCORD_BOT.start();
                } catch (final Throwable e) {
                    err = true;
                    DISCORD_LOG.error("Failed starting discord bot", e);
                }
                if (!err) DISCORD_LOG.info("Started Discord Bot");
            }
            MODULE_MANAGER.init();
            Queue.start();
            saveConfigAsync();
            this.startServer();
            CACHE.reset(true);
            SCHEDULED_EXECUTOR_SERVICE.scheduleAtFixedRate(this::handleActiveHoursTick, 1L, 1L, TimeUnit.MINUTES);
            SCHEDULED_EXECUTOR_SERVICE.scheduleAtFixedRate(this::serverHealthCheck, 1L, 5L, TimeUnit.MINUTES);
            SCHEDULED_EXECUTOR_SERVICE.scheduleAtFixedRate(this::tablistUpdate, 20L, 3L, TimeUnit.SECONDS);
            SCHEDULED_EXECUTOR_SERVICE.scheduleAtFixedRate(this::updatePrioBanStatus, 0L, 1L, TimeUnit.DAYS);
            SCHEDULED_EXECUTOR_SERVICE.scheduleAtFixedRate(this::sixHourKickWarningTick, 350L, 1L, TimeUnit.MINUTES);
            SCHEDULED_EXECUTOR_SERVICE.scheduleAtFixedRate(this::maxPlaytimeTick, CONFIG.client.maxPlaytimeReconnectMins, 1L, TimeUnit.MINUTES);
            if (CONFIG.server.enabled && CONFIG.server.ping.favicon) {
                SCHEDULED_EXECUTOR_SERVICE.submit(this::updateFavicon);
            }
            if (CONFIG.client.autoConnect && !this.isConnected()) {
                this.connectAndCatchExceptions();
            }
            if (CONFIG.autoUpdater.shouldReconnectAfterAutoUpdate) {
                CONFIG.autoUpdater.shouldReconnectAfterAutoUpdate = false;
                saveConfigAsync();
                if (!CONFIG.client.extra.utility.actions.autoDisconnect.autoClientDisconnect && !this.isConnected()) {
                    this.connectAndCatchExceptions();
                }
            }
            if (CONFIG.autoUpdater.autoUpdate) {
                if (LAUNCH_CONFIG.release_channel.equals("git")) autoUpdater = new GitAutoUpdater();
                else autoUpdater = new RestAutoUpdater();
                autoUpdater.start();
                DEFAULT_LOG.info("Started {} AutoUpdater...", LAUNCH_CONFIG.release_channel);
            }
            DEFAULT_LOG.info("ZenithProxy started!");
            Wait.waitSpinLoop();
        } catch (Exception e) {
            DEFAULT_LOG.error("", e);
        } finally {
            DEFAULT_LOG.info("Shutting down...");
            if (this.server != null) {
                this.server.close(true);
            }
            saveConfig();
        }
    }

    private void serverHealthCheck() {
        if (!CONFIG.server.enabled || !CONFIG.server.healthCheck) return;
        if (server != null && server.isListening()) return;
        SERVER_LOG.error("Server is not listening! Is another service on this port?");
        this.startServer();
        SCHEDULED_EXECUTOR_SERVICE.schedule(() -> {
            if (server == null || !server.isListening()) {
                SERVER_LOG.error("Server is not listening and unable to quick restart, performing full restart...");
                CONFIG.autoUpdater.shouldReconnectAfterAutoUpdate = true;
                stop();
            }
        }, 30, TimeUnit.SECONDS);
    }

    private void maxPlaytimeTick() {
        if (CONFIG.client.maxPlaytimeReconnect && isOnlineForAtLeastDuration(Duration.ofMinutes(CONFIG.client.maxPlaytimeReconnectMins))) {
            CLIENT_LOG.info("Max playtime minutes reached: {}, reconnecting...", CONFIG.client.maxPlaytimeReconnectMins);
            disconnect(SYSTEM_DISCONNECT);
            cancelAutoReconnect();
            connect();
        }
    }

    private void tablistUpdate() {
        if (!this.isConnected() || currentPlayer.get() == null) return;
        long lastUpdate = CACHE.getTabListCache().getLastUpdate();
        if (lastUpdate < System.currentTimeMillis() - 3000) {
            currentPlayer.get().sendAsync(new ClientboundTabListPacket(CACHE.getTabListCache().getHeader(), CACHE.getTabListCache().getFooter()));
            CACHE.getTabListCache().setLastUpdate(System.currentTimeMillis());
        }
    }

    public void stop() {
        DEFAULT_LOG.info("Shutting Down...");
        try {
            CompletableFuture.runAsync(() -> {
                if (nonNull(this.client)) {
                    this.client.disconnect(MinecraftConstants.SERVER_CLOSING_MESSAGE);
                }
                if (nonNull(this.server)) {
                    this.server.close(true);
                }
                saveConfig();
                int count = 0;
                while (!DISCORD_BOT.isMessageQueueEmpty() && count++ < 10) {
                    Wait.waitALittleMs(100);
                }
                DISCORD_BOT.stop(true);
            }).get(10L, TimeUnit.SECONDS);
        } catch (final Exception e) {
            DEFAULT_LOG.error("Error shutting down gracefully", e);
        } finally {
            try {
                ((LoggerContext) LoggerFactory.getILoggerFactory()).stop();
            } finally {
                System.exit(0);
            }
        }
    }

    public void disconnect() {
        disconnect(MANUAL_DISCONNECT);
    }

    public void disconnect(final String reason, final Throwable cause) {
        if (this.isConnected()) {
            this.client.disconnect(reason, cause);
        }
        CACHE.reset(true);
    }

    public void disconnect(final String reason) {
        if (this.isConnected()) {
            this.client.disconnect(reason);
        }
        CACHE.reset(true);
    }

    public void connectAndCatchExceptions() {
        try {
            this.connect();
        } catch (final Exception e) {
            DEFAULT_LOG.error("Error connecting", e);
        }
    }

    /**
     * @throws IllegalStateException if already connected
     */
    public synchronized void connect() {
        try {
            EVENT_BUS.postAsync(new StartConnectEvent());
            this.logIn();
        } catch (final RuntimeException e) {
            EVENT_BUS.post(new ProxyLoginFailedEvent());
            getActiveConnections().forEach(connection -> connection.disconnect("Login failed"));
            SCHEDULED_EXECUTOR_SERVICE.schedule(() -> {
                EVENT_BUS.post(new DisconnectEvent(LOGIN_FAILED));
            }, 1L, TimeUnit.SECONDS);
            return;
        }

        if (this.isConnected()) {
            throw new IllegalStateException("Already connected!");
        }

        CLIENT_LOG.info("Connecting to {}:{}...", CONFIG.client.server.address, CONFIG.client.server.port);
        this.client = new ClientSession(CONFIG.client.server.address, CONFIG.client.server.port, CONFIG.client.bindAddress, this.protocol, this);
        if (Objects.equals(CONFIG.client.server.address, "connect.2b2t.org")) {
            this.client.setFlag(BuiltinFlags.ATTEMPT_SRV_RESOLVE, false);
        }
        if (CONFIG.server.extra.timeout.enable)
            this.client.setReadTimeout(CONFIG.server.extra.timeout.seconds);
        else
            this.client.setReadTimeout(0);
        this.client.setFlag(BuiltinFlags.PRINT_DEBUG, true);
        if (CONFIG.client.viaversion.enabled) {
            if (CONFIG.client.viaversion.autoProtocolVersion)
                updateViaProtocolVersion();
            if (CONFIG.client.viaversion.protocolVersion == ProtocolVersion.v1_20_3.getVersion()) {
                CLIENT_LOG.warn("ViaVersion enabled but server protocol is 1.20, connecting without ViaVersion");
                this.client.connect(true);
            } else if (CONFIG.client.server.address.toLowerCase().endsWith("2b2t.org")) {
                CLIENT_LOG.warn("ViaVersion enabled but server set to 2b2t.org, connecting without ViaVersion");
                this.client.connect(true);
            } else {
                ChannelInitializer<Channel> originalChannelInitializer = this.client.buildChannelInitializer();
                final ZViaServerProxyPlatform viaProxy = new ZViaServerProxyPlatform(this.client);
                viaProxy.init();
                ChannelInitializer<Channel> viaChannelInitializer = viaProxy.inject(originalChannelInitializer);
                Bootstrap bootstrap = this.client.buildBootstrap(viaChannelInitializer);
                this.client.connect(true, bootstrap);
            }
        } else {
            this.client.connect(true);
        }
    }

    private void updateViaProtocolVersion() {
        try {
            final int detectedVersion = ProtocolVersionDetector.getProtocolVersion(CONFIG.client.server.address,
                                                                                   CONFIG.client.server.port);
            if (!ProtocolVersion.isRegistered(detectedVersion)) {
                CLIENT_LOG.error("Unknown protocol version {} detected for server: {}:{}",
                                 detectedVersion,
                                 CONFIG.client.server.address,
                                 CONFIG.client.server.port);
                return;
            }
            CLIENT_LOG.info("Updating detected protocol version {} for server: {}:{}",
                            detectedVersion,
                            CONFIG.client.server.address,
                            CONFIG.client.server.port);
            CONFIG.client.viaversion.protocolVersion = detectedVersion;
            saveConfigAsync();
        } catch (final Exception e) {
            CLIENT_LOG.error("Failed to detect protocol version for server: {}:{}",
                             CONFIG.client.server.address,
                             CONFIG.client.server.port,
                             e);
        }
    }

    public boolean isConnected() {
        return this.client != null && this.client.isConnected();
    }

    public void startServer() {
        synchronized (this) {
            if (this.server != null && this.server.isListening()) {
                throw new IllegalStateException("Server already started!");
            }
            if (CONFIG.server.enabled) {
                try (InputStream is = new ByteArrayInputStream(Base64.getDecoder().decode("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"))) {
                    this.serverIcon = is.readAllBytes();
                } catch (IOException e) {
                    //impossible
                    throw new AssertionError();
                }

                String address = CONFIG.server.bind.address;
                int port = CONFIG.server.bind.port;

                SERVER_LOG.info("Starting server on {}:{}...", address, port);
                MinecraftProtocol minecraftProtocol = new MinecraftProtocol();
                minecraftProtocol.setUseDefaultListeners(false); // very important
                this.server = new TcpServer(address, port, () -> minecraftProtocol);
                this.server.setGlobalFlag(MinecraftConstants.VERIFY_USERS_KEY, CONFIG.server.verifyUsers);
                this.server.setGlobalFlag(MinecraftConstants.SERVER_INFO_BUILDER_KEY, new CustomServerInfoBuilder(this));
                this.server.setGlobalFlag(MinecraftConstants.SERVER_LOGIN_HANDLER_KEY, new ProxyServerLoginHandler(this));
                this.server.setGlobalFlag(MinecraftConstants.SERVER_COMPRESSION_THRESHOLD, CONFIG.server.compressionThreshold);
                this.server.setGlobalFlag(MinecraftConstants.AUTOMATIC_KEEP_ALIVE_MANAGEMENT, true);
                this.server.addListener(new ProxyServerListener());
                this.server.bind(false);
            }
        }
    }

    public void logIn() {
        loggingIn.set(true);
        AUTH_LOG.info("Logging in {}...", CONFIG.authentication.username);
        if (this.authenticator == null) {
            this.authenticator = new Authenticator();
        }
        int tries = 0;
        while (tries < 3 && !retrieveLoginTaskResult(loginTask())) {
            if (!loggingIn.get()) break;
            tries++;
            AUTH_LOG.warn("Failed login attempt " + tries);
            // wait random time between 3 and 10 seconds
            Wait.waitALittle((int) (3 + (Math.random() * 7.0)));
        }
        if (!loggingIn.get()) throw new RuntimeException("Login Cancelled");
        loggingIn.set(false);
        if (tries == 3) {
            throw new RuntimeException("Auth failed");
        }
        CACHE.getProfileCache().setProfile(this.protocol.getProfile());
        AUTH_LOG.info("Logged in as {} [{}].", this.protocol.getProfile().getName(), this.protocol.getProfile().getId());
        SCHEDULED_EXECUTOR_SERVICE.submit(this::updateFavicon);
    }

    public Future<Boolean> loginTask() {
        return SCHEDULED_EXECUTOR_SERVICE.submit(() -> {
            try {
                this.protocol = this.authenticator.handleRelog();
                this.protocol.setUseDefaultListeners(false); // very important
                return true;
            } catch (final Exception e) {
                CLIENT_LOG.error("", e);
                return false;
            }
        });
    }

    public boolean retrieveLoginTaskResult(Future<Boolean> loginTask) {
        try {
            long maxWait = CONFIG.authentication.accountType == Config.Authentication.AccountType.DEVICE_CODE ? 300L : 10L;
            long currentWait = 0;
            while (!loginTask.isDone() && currentWait < maxWait) {
                if (!loggingIn.get()) { // allow login to be cancelled
                    loginTask.cancel(true);
                    return false;
                }
                Wait.waitALittle(1);
                currentWait++;
            }
            return loginTask.get(1L, TimeUnit.SECONDS);
        } catch (Exception e) {
            loginTask.cancel(true);
            return false;
        }
    }

    public URL getAvatarURL(UUID uuid) {
        return getAvatarURL(uuid.toString().replace("-", ""));
    }

    public URL getAvatarURL(String playerName) {
        try {
            return URI.create(String.format("https://minotar.net/helm/%s/64", playerName)).toURL();
        } catch (MalformedURLException e) {
            SERVER_LOG.error("Failed to get avatar");
            throw new UncheckedIOException(e);
        }
    }

    public boolean cancelAutoReconnect() {
        synchronized (this.autoReconnectFuture) {
            if (autoReconnectIsInProgress()) {
                Future<?> future = this.autoReconnectFuture.get();
                this.autoReconnectFuture = Optional.empty();
                future.cancel(true);
                return true;
            }
        }
        return false;
    }

    public boolean autoReconnectIsInProgress() {
        return this.autoReconnectFuture.isPresent();
    }

    // returns true if we were previously trying to log in
    public boolean cancelLogin() {
        this.reconnectCounter = 0;
        return this.loggingIn.getAndSet(false);
    }

    public Stream<ServerConnection> getSpectatorConnections() {
        if (getActiveConnections().isEmpty()) return Stream.empty();
        if (getActiveConnections().size() == 1 && getCurrentPlayer().get() != null) return Stream.empty();
        return getActiveConnections().stream()
                .filter(ServerConnection::isSpectator);
    }

    public void delayBeforeReconnect() {
        try {
            final int countdown;
            countdown = CONFIG.client.extra.autoReconnect.delaySeconds
                // random jitter to help prevent multiple clients from logging in at the same time
                + ((int) (Math.random() * 5));
            reconnectCounter++;
            EVENT_BUS.postAsync(new AutoReconnectEvent(countdown));
            for (int i = countdown; SHOULD_RECONNECT && i > 0; i--) {
                if (i % 10 == 0) CLIENT_LOG.info("Reconnecting in {}", i);
                Wait.waitALittle(1);
            }
        } catch (Exception e) {
            CLIENT_LOG.info("AutoReconnect stopped");
        }
    }

    public boolean hasActivePlayer() {
        ServerConnection player = this.currentPlayer.get();
        return player != null && player.isLoggedIn();
    }

    public void updatePrioBanStatus() {
        if (!CONFIG.client.extra.prioBan2b2tCheck || !CONFIG.client.server.address.toLowerCase(Locale.ROOT).contains("2b2t.org")) return;
        this.isPrioBanned = PRIORITY_BAN_CHECKER.checkPrioBan();
        if (this.isPrioBanned.isPresent() && !this.isPrioBanned.get().equals(CONFIG.authentication.prioBanned)) {
            EVENT_BUS.postAsync(new PrioBanStatusUpdateEvent(this.isPrioBanned.get()));
            CONFIG.authentication.prioBanned = this.isPrioBanned.get();
            saveConfigAsync();
            CLIENT_LOG.info("Prio Ban Change Detected: " + this.isPrioBanned.get());
        }
    }

    public void kickNonWhitelistedPlayers() {
        Proxy.getInstance().getActiveConnections().stream()
            .filter(con -> nonNull(con.getProfileCache().getProfile()))
            .filter(con -> !PLAYER_LISTS.getWhitelist().contains(con.getProfileCache().getProfile()))
            .filter(con -> !(PLAYER_LISTS.getSpectatorWhitelist().contains(con.getProfileCache().getProfile()) && con.isSpectator()))
            .forEach(con -> con.disconnect("Not whitelisted"));
    }

    private void handleActiveHoursTick() {
        Config.Client.Extra.Utility.ActiveHours activeHoursConfig = CONFIG.client.extra.utility.actions.activeHours;
        if (activeHoursConfig.enabled
                // prevent rapid reconnects
                && this.lastActiveHoursConnect.isBefore(Instant.now().minus(1L, ChronoUnit.HOURS))
                // only force reconnect an active session if config enabled
                && ((nonNull(this.currentPlayer.get()) && this.currentPlayer.get().isConnected() && activeHoursConfig.forceReconnect)
                            || (isNull(this.currentPlayer.get()) || !this.currentPlayer.get().isConnected()))) {
            // get current queue wait time
            int queueLength = (CONFIG.authentication.prio ? Queue.getQueueStatus().prio() : Queue.getQueueStatus().regular());
            long queueWaitSeconds = Queue.getQueueWait(queueLength);
            activeHoursConfig.activeTimes.stream()
                    .flatMap(activeTime -> {
                        ZonedDateTime activeHourToday = ZonedDateTime.of(LocalDate.now(ZoneId.of(activeHoursConfig.timeZoneId)), LocalTime.of(activeTime.hour, activeTime.minute), ZoneId.of(activeHoursConfig.timeZoneId));
                        ZonedDateTime activeHourTomorrow = activeHourToday.plusDays(1L);
                        return Stream.of(activeHourToday, activeHourTomorrow);
                    })
                    .filter(activeHourDateTime -> {
                        long nowPlusQueueWaitEpoch = LocalDateTime.now(ZoneId.of(activeHoursConfig.timeZoneId)).plusSeconds((long)queueWaitSeconds).atZone(ZoneId.of(activeHoursConfig.timeZoneId)).toEpochSecond();
                        long activeHoursEpoch = activeHourDateTime.toEpochSecond();
                        // active hour within 8 mins range of now
                        return nowPlusQueueWaitEpoch > activeHoursEpoch - 240 && nowPlusQueueWaitEpoch < activeHoursEpoch + 240;
                    })
                    .findAny()
                    .ifPresent(t -> {
                        EVENT_BUS.postAsync(new ActiveHoursConnectEvent());
                        this.lastActiveHoursConnect = Instant.now();
                        disconnect(SYSTEM_DISCONNECT);
                        SCHEDULED_EXECUTOR_SERVICE.schedule(this::connectAndCatchExceptions, 1, TimeUnit.MINUTES);
                    });
        }
    }

    public boolean isOnlineOn2b2tForAtLeastDuration(Duration duration) {
        return CONFIG.client.server.address.endsWith("2b2t.org") && isOnlineForAtLeastDuration(duration);
    }

    public boolean isOnlineForAtLeastDuration(Duration duration) {
        return isConnected()
            && !isInQueue()
            && nonNull(getConnectTime())
            && getConnectTime().isBefore(Instant.now().minus(duration));
    }

    public void updateFavicon() {
        try {
            final GameProfile profile = CACHE.getProfileCache().getProfile();
            final URL avatarURL;
            if (profile != null && profile.getId() != null)
                avatarURL = getAvatarURL(profile.getId());
            else
                avatarURL = getAvatarURL(CONFIG.authentication.username.equals("Unknown") ? "odpay" : CONFIG.authentication.username);
            try (InputStream netInputStream = HttpClient.create()
                .secure()
                .followRedirect(true)
                .get()
                .uri(avatarURL.toURI())
                .responseContent()
                .aggregate()
                .asInputStream()
                .block()) {
                if (netInputStream == null) {
                    throw new IOException("Unable to download server icon for \"" + CONFIG.authentication.username + "\"");
                }
                this.serverIcon = netInputStream.readAllBytes();
                if (DISCORD_BOT.isRunning()) {
                    if (CONFIG.discord.manageProfileImage) DISCORD_BOT.updateProfileImage(this.serverIcon);
                    if (CONFIG.discord.manageNickname) DISCORD_BOT.setBotNickname(CONFIG.authentication.username + " | ZenithProxy");
                    if (CONFIG.discord.manageDescription) DISCORD_BOT.setBotDescription("ZenithProxy " + LAUNCH_CONFIG.version
                                                                                            + "\n\n**Official Discord**:\n  https://discord.gg/nJZrSaRKtb"
                                                                                            + "\n**Github**:\n  https://github.com/rfresh2/ZenithProxy");
                }
            }
        } catch (Exception e) {
            SERVER_LOG.error("Unable to download server icon for \"{}\":\n", CONFIG.authentication.username, e);
        }
    }

    public void sixHourKickWarningTick() {
        try {
            if (this.isPrio.orElse(false) // Prio players don't have 6h kick.
                || !this.hasActivePlayer() // If no player is connected, nobody to warn
                || !isOnlineOn2b2tForAtLeastDuration(Duration.ofMinutes(350)) // 6hrs - 10 mins
            ) return;
            final ServerConnection playerConnection = this.currentPlayer.get();
            final int minsUntil6Hrs = (int) ((21600 - (Instant.now().getEpochSecond() - connectTime.getEpochSecond())) / 60);
            if (minsUntil6Hrs < 0) return; // sanity check just in case 2b's plugin changes
            var actionBarPacket = new ClientboundSetActionBarTextPacket(
                ComponentSerializer.mineDownParse((minsUntil6Hrs <= 3 ? "&c" : "&9") + "6hr kick in: " + minsUntil6Hrs + "m"));
            playerConnection.sendAsync(actionBarPacket);
            // each packet will reset text render timer for 3 seconds
            for (int i = 1; i <= 7; i++) { // render the text for about 10 seconds total
                playerConnection.sendScheduledAsync(actionBarPacket, i, TimeUnit.SECONDS);
            }
            playerConnection.sendAsync(new ClientboundSoundPacket(
                BuiltinSound.BLOCK_ANVIL_PLACE,
                SoundCategory.AMBIENT,
                CACHE.getPlayerCache().getX(),
                CACHE.getPlayerCache().getY(),
                CACHE.getPlayerCache().getZ(),
                1.0f,
                1.0f + (ThreadLocalRandom.current().nextFloat() / 10f), // slight pitch variations
                0L
            ));
        } catch (final Throwable e) {
            DEFAULT_LOG.error("Error in 6 hr kick warning tick", e);
        }
    }

    public void handleDisconnectEvent(DisconnectEvent event) {
        CACHE.reset(true);
        this.disconnectTime = Instant.now();
        this.inQueue = false;
        this.queuePosition = 0;
        if (!CONFIG.client.extra.utility.actions.autoDisconnect.autoClientDisconnect) {
            // skip autoreconnect when we want to sync client disconnect
            if (CONFIG.client.extra.autoReconnect.enabled && isReconnectableDisconnect(event.reason())) {
                if (autoReconnectIsInProgress()) {
                    return;
                }
                this.autoReconnectFuture = Optional.of(SCHEDULED_EXECUTOR_SERVICE.submit(() -> {
                    delayBeforeReconnect();
                    synchronized (this.autoReconnectFuture) {
                        if (this.autoReconnectFuture.isPresent()) {
                            this.autoReconnectFuture = Optional.empty();
                            this.connect();
                        } else
                            this.autoReconnectFuture = Optional.empty();
                    }
                }));
            }
        }
        TPS_CALCULATOR.reset();
    }


    public void handleConnectEvent(ConnectEvent event) {
        this.connectTime = Instant.now();
        cancelAutoReconnect();
    }

    public void handleStartQueueEvent(StartQueueEvent event) {
        this.inQueue = true;
        this.queuePosition = 0;
        this.reconnectCounter = 0;
        updatePrioBanStatus();
    }

    public void handleQueuePositionUpdateEvent(QueuePositionUpdateEvent event) {
        this.queuePosition = event.position();
    }

    public void handleQueueCompleteEvent(QueueCompleteEvent event) {
        this.inQueue = false;
        this.connectTime = Instant.now();
    }

    public void handlePlayerOnlineEvent(PlayerOnlineEvent event) {
        if (!this.isPrio.isPresent()) {
            // assume we are prio if we skipped queuing
            EVENT_BUS.postAsync(new PrioStatusEvent(true));
        }
        this.reconnectCounter = 0;
        PlayerCache.sync();
    }

    public void handleProxyClientDisconnectedEvent(final ProxyClientDisconnectedEvent e) {
        PlayerCache.sync();
    }

    public void handleServerRestartingEvent(ServerRestartingEvent event) {
        if (!this.isPrio.orElse(false) && isNull(getCurrentPlayer().get())) {
            SCHEDULED_EXECUTOR_SERVICE.schedule(() -> {
                if (isNull(getCurrentPlayer().get()))
                    disconnect(SERVER_RESTARTING);
            }, ((int) (Math.random() * 20)), TimeUnit.SECONDS);
        }
    }

    public void handlePrioStatusEvent(PrioStatusEvent event) {
        if (CONFIG.client.server.address.toLowerCase().contains("2b2t.org")) {
            if (event.prio() == CONFIG.authentication.prio) {
                if (isPrio.isEmpty()) {
                    CLIENT_LOG.info("Prio Detected: " + event.prio());
                    this.isPrio = Optional.of(event.prio());
                }
            } else {
                CLIENT_LOG.info("Prio Change Detected: " + event.prio());
                EVENT_BUS.postAsync(new PrioStatusUpdateEvent(event.prio()));
                this.isPrio = Optional.of(event.prio());
                CONFIG.authentication.prio = event.prio();
                saveConfigAsync();
            }
        }
    }

    public void handleServerPlayerConnectedEvent(ServerPlayerConnectedEvent event) {
        if (CONFIG.client.extra.chat.showConnectionMessages) {
            ServerConnection serverConnection = getCurrentPlayer().get();
            if (nonNull(serverConnection) && serverConnection.isLoggedIn()) {
                serverConnection.sendDirect(new ClientboundSystemChatPacket(ComponentSerializer.mineDownParse("&b" + event.playerEntry().getName() + "&r&e connected"), false));
            }
        }
    }

    public void handleServerPlayerDisconnectedEvent(ServerPlayerDisconnectedEvent event) {
        if (CONFIG.client.extra.chat.showConnectionMessages) {
            ServerConnection serverConnection = getCurrentPlayer().get();
            if (nonNull(serverConnection) && serverConnection.isLoggedIn()) {
                serverConnection.sendDirect(new ClientboundSystemChatPacket(ComponentSerializer.mineDownParse("&b" + event.playerEntry().getName() + "&r&e disconnected"), false));
            }
        }
    }
}
