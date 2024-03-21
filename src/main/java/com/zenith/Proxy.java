package com.zenith;

import ch.qos.logback.classic.LoggerContext;
import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.mc.protocol.MinecraftConstants;
import com.github.steveice10.mc.protocol.MinecraftProtocol;
import com.github.steveice10.mc.protocol.codec.MinecraftCodecHelper;
import com.github.steveice10.mc.protocol.data.game.level.sound.BuiltinSound;
import com.github.steveice10.mc.protocol.data.game.level.sound.SoundCategory;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundSystemChatPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundTabListPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.level.ClientboundSoundPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.title.ClientboundSetActionBarTextPacket;
import com.github.steveice10.packetlib.BuiltinFlags;
import com.github.steveice10.packetlib.ProxyInfo;
import com.github.steveice10.packetlib.tcp.TcpServer;
import com.zenith.event.proxy.*;
import com.zenith.feature.autoupdater.AutoUpdater;
import com.zenith.feature.autoupdater.GitAutoUpdater;
import com.zenith.feature.autoupdater.RestAutoUpdater;
import com.zenith.feature.queue.Queue;
import com.zenith.network.client.Authenticator;
import com.zenith.network.client.ClientSession;
import com.zenith.network.server.CustomServerInfoBuilder;
import com.zenith.network.server.LanBroadcaster;
import com.zenith.network.server.ProxyServerListener;
import com.zenith.network.server.ServerConnection;
import com.zenith.network.server.handler.ProxyServerLoginHandler;
import com.zenith.util.ComponentSerializer;
import com.zenith.util.Config;
import com.zenith.util.Wait;
import com.zenith.via.ZenithViaInitializer;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.SneakyThrows;
import net.raphimc.minecraftauth.responsehandler.exception.MinecraftRequestException;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.*;
import java.time.chrono.ChronoZonedDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static com.github.rfresh2.EventConsumer.of;
import static com.zenith.Shared.*;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;


@Getter
public class Proxy {
    @Getter protected static Proxy instance;
    protected ClientSession client;
    protected TcpServer server;
    protected final Authenticator authenticator = new Authenticator();
    protected byte[] serverIcon;
    protected final AtomicReference<ServerConnection> currentPlayer = new AtomicReference<>();
    protected final CopyOnWriteArraySet<ServerConnection> activeConnections = new CopyOnWriteArraySet<>();
    private boolean inQueue = false;
    private int queuePosition = 0;
    @Setter private Instant connectTime;
    private Instant disconnectTime = Instant.now();
    private Optional<Boolean> isPrio = Optional.empty();
    private Optional<Boolean> isPrioBanned = Optional.empty();
    private Optional<Future<?>> autoReconnectFuture = Optional.empty();
    private Instant lastActiveHoursConnect = Instant.EPOCH;
    private final AtomicBoolean loggingIn = new AtomicBoolean(false);
    @Setter private AutoUpdater autoUpdater;
    private LanBroadcaster lanBroadcaster;
    // might move to config and make the user deal with it when it changes
    private static final Duration twoB2tTimeLimit = Duration.ofHours(6);
    private final ZenithViaInitializer viaInitializer = new ZenithViaInitializer();

    public static void main(String... args) {
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();
        instance = new Proxy();
        instance.start();
    }

    public void initEventHandlers() {
        EVENT_BUS.subscribe(this,
                            of(DisconnectEvent.class, this::handleDisconnectEvent),
                            of(ConnectEvent.class, this::handleConnectEvent),
                            of(StartQueueEvent.class, this::handleStartQueueEvent),
                            of(QueuePositionUpdateEvent.class, this::handleQueuePositionUpdateEvent),
                            of(QueueCompleteEvent.class, this::handleQueueCompleteEvent),
                            of(PlayerOnlineEvent.class, this::handlePlayerOnlineEvent),
                            of(ServerRestartingEvent.class, this::handleServerRestartingEvent),
                            of(PrioStatusEvent.class, this::handlePrioStatusEvent),
                            of(ServerPlayerConnectedEvent.class, this::handleServerPlayerConnectedEvent),
                            of(ServerPlayerDisconnectedEvent.class, this::handleServerPlayerDisconnectedEvent)
        );
    }

    public void start() {
        loadConfig();
        loadLaunchConfig();
        DEFAULT_LOG.info("Starting ZenithProxy-{}", LAUNCH_CONFIG.version);
        initEventHandlers();
        try {
            if (CONFIG.debug.clearOldLogs) clearOldLogs();
            if (CONFIG.interactiveTerminal.enable) TERMINAL.start();
            if (CONFIG.database.enabled) {
                DATABASE.start();
                DEFAULT_LOG.info("Started Databases");
            }
            if (CONFIG.discord.enable) {
                boolean err = false;
                try {
                    DISCORD.start();
                } catch (final Throwable e) {
                    err = true;
                    DISCORD_LOG.error("Failed starting discord bot", e);
                }
                if (!err) DISCORD_LOG.info("Started Discord Bot");
            }
            MODULE.init();
            Queue.start();
            saveConfigAsync();
            MinecraftCodecHelper.useBinaryNbtComponentSerializer = CONFIG.debug.binaryNbtComponentSerializer;
            this.startServer();
            CACHE.reset(true);
            EXECUTOR.scheduleAtFixedRate(this::handleActiveHoursTick, 0L, 1L, TimeUnit.MINUTES);
            EXECUTOR.scheduleAtFixedRate(this::serverHealthCheck, 1L, 5L, TimeUnit.MINUTES);
            EXECUTOR.scheduleAtFixedRate(this::tablistUpdate, 20L, 3L, TimeUnit.SECONDS);
            EXECUTOR.scheduleAtFixedRate(this::updatePrioBanStatus, 0L, 1L, TimeUnit.DAYS);
            EXECUTOR.scheduleAtFixedRate(this::twoB2tTimeLimitKickWarningTick, twoB2tTimeLimit.minusMinutes(10L).toMinutes(), 1L, TimeUnit.MINUTES);
            EXECUTOR.scheduleAtFixedRate(this::maxPlaytimeTick, CONFIG.client.maxPlaytimeReconnectMins, 1L, TimeUnit.MINUTES);
            if (CONFIG.server.enabled && CONFIG.server.ping.favicon)
                EXECUTOR.submit(this::updateFavicon);
            boolean connected = false;
            if (CONFIG.client.autoConnect && !this.isConnected()) {
                this.connectAndCatchExceptions();
                connected = true;
            }
            if (!connected && CONFIG.autoUpdater.shouldReconnectAfterAutoUpdate) {
                CONFIG.autoUpdater.shouldReconnectAfterAutoUpdate = false;
                saveConfigAsync();
                if (!CONFIG.client.extra.utility.actions.autoDisconnect.autoClientDisconnect && !this.isConnected()) {
                    this.connectAndCatchExceptions();
                    connected = true;
                }
            }
            if (LAUNCH_CONFIG.auto_update) {
                if (LAUNCH_CONFIG.release_channel.equals("git")) autoUpdater = new GitAutoUpdater();
                else autoUpdater = new RestAutoUpdater();
                autoUpdater.start();
                DEFAULT_LOG.info("Started AutoUpdater");
            }
            DEFAULT_LOG.info("ZenithProxy started!");
            if (!connected)
                DEFAULT_LOG.info("Use the `connect` command to log in!");
            Wait.waitSpinLoop();
        } catch (Exception e) {
            DEFAULT_LOG.error("", e);
        } finally {
            DEFAULT_LOG.info("Shutting down...");
            if (this.server != null) this.server.close(true);
            saveConfig();
        }
    }

    private static void clearOldLogs() {
        try (Stream<Path> walk = Files.walk(Path.of("log/"))) {
            walk.filter(path -> path.toString().endsWith(".zip")).forEach(path -> {
                try {
                    Files.delete(path);
                } catch (final IOException e) {
                    DEFAULT_LOG.error("Error deleting old log file", e);
                }
            });
        } catch (final IOException e) {
            DEFAULT_LOG.error("Error deleting old log file", e);
        }
    }

    private void serverHealthCheck() {
        if (!CONFIG.server.enabled || !CONFIG.server.healthCheck) return;
        if (server != null && server.isListening()) return;
        SERVER_LOG.error("Server is not listening! Is another service on this port?");
        this.startServer();
        EXECUTOR.schedule(() -> {
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
        var playerConnection = currentPlayer.get();
        if (!this.isConnected() || playerConnection == null) return;
        if (!playerConnection.isLoggedIn()) return;
        long lastUpdate = CACHE.getTabListCache().getLastUpdate();
        if (lastUpdate < System.currentTimeMillis() - 3000) {
            playerConnection.sendAsync(new ClientboundTabListPacket(CACHE.getTabListCache().getHeader(), CACHE.getTabListCache().getFooter()));
            CACHE.getTabListCache().setLastUpdate(System.currentTimeMillis());
        }
    }

    public void stop() {
        DEFAULT_LOG.info("Shutting Down...");
        try {
            CompletableFuture.runAsync(() -> {
                if (nonNull(this.client)) this.client.disconnect(MinecraftConstants.SERVER_CLOSING_MESSAGE);
                stopServer();
                saveConfig();
                int count = 0;
                while (!DISCORD.isMessageQueueEmpty() && count++ < 10) {
                    Wait.waitMs(100);
                }
                DISCORD.stop(true);
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
        if (this.isConnected()) this.client.disconnect(reason, cause);
        CACHE.reset(true);
    }

    public void disconnect(final String reason) {
        if (this.isConnected()) this.client.disconnect(reason);
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
        final MinecraftProtocol minecraftProtocol;
        try {
            EVENT_BUS.postAsync(new StartConnectEvent());
            minecraftProtocol = this.logIn();
        } catch (final Exception e) {
            EVENT_BUS.post(new ProxyLoginFailedEvent());
            getActiveConnections().forEach(connection -> connection.disconnect("Login failed"));
            EXECUTOR.schedule(() -> {
                EVENT_BUS.post(new DisconnectEvent(LOGIN_FAILED));
            }, 1L, TimeUnit.SECONDS);
            return;
        }
        if (this.isConnected()) throw new IllegalStateException("Already connected!");
        CLIENT_LOG.info("Connecting to {}:{}...", CONFIG.client.server.address, CONFIG.client.server.port);
        this.client = new ClientSession(CONFIG.client.server.address, CONFIG.client.server.port, CONFIG.client.bindAddress, minecraftProtocol, getClientProxyInfo());
        if (Objects.equals(CONFIG.client.server.address, "connect.2b2t.org"))
            this.client.setFlag(BuiltinFlags.ATTEMPT_SRV_RESOLVE, false);
        this.client.setReadTimeout(CONFIG.server.extra.timeout.enable ? CONFIG.server.extra.timeout.seconds : 0);
        this.client.setFlag(BuiltinFlags.PRINT_DEBUG, true);
        this.client.setInitChannelConsumer(channel -> viaInitializer.clientViaChannelInitializer(channel, this.client));
        this.client.connect(true);
    }

    @Nullable
    private static ProxyInfo getClientProxyInfo() {
        ProxyInfo proxyInfo = null;
        if (CONFIG.client.connectionProxy.enabled) {
            if (!CONFIG.client.connectionProxy.user.isEmpty() || !CONFIG.client.connectionProxy.password.isEmpty())
                proxyInfo = new ProxyInfo(CONFIG.client.connectionProxy.type,
                                          new InetSocketAddress(CONFIG.client.connectionProxy.host,
                                                                CONFIG.client.connectionProxy.port),
                                          CONFIG.client.connectionProxy.user,
                                          CONFIG.client.connectionProxy.password);
            else proxyInfo = new ProxyInfo(CONFIG.client.connectionProxy.type,
                                           new InetSocketAddress(CONFIG.client.connectionProxy.host,
                                                                 CONFIG.client.connectionProxy.port));
        }
        return proxyInfo;
    }

    public boolean isConnected() {
        return this.client != null && this.client.isConnected();
    }

    @SneakyThrows
    public synchronized void startServer() {
        if (this.server != null && this.server.isListening())
            throw new IllegalStateException("Server already started!");
        if (!CONFIG.server.enabled) return;
        this.serverIcon = getClass().getClassLoader().getResourceAsStream("servericon.png").readAllBytes();
        var address = CONFIG.server.bind.address;
        var port = CONFIG.server.bind.port;
        SERVER_LOG.info("Starting server on {}:{}...", address, port);
        this.server = new TcpServer(address, port, MinecraftProtocol::new);
        this.server.setInitChannelConsumer(viaInitializer::serverViaChannelInitializer);
        this.server.setGlobalFlag(MinecraftConstants.VERIFY_USERS_KEY, CONFIG.server.verifyUsers);
        var serverInfoBuilder = new CustomServerInfoBuilder();
        this.server.setGlobalFlag(MinecraftConstants.SERVER_INFO_BUILDER_KEY, serverInfoBuilder);
        if (this.lanBroadcaster == null && CONFIG.server.ping.lanBroadcast) {
            this.lanBroadcaster = new LanBroadcaster(serverInfoBuilder);
            lanBroadcaster.start();
        }
        this.server.setGlobalFlag(MinecraftConstants.SERVER_LOGIN_HANDLER_KEY, new ProxyServerLoginHandler());
        this.server.setGlobalFlag(MinecraftConstants.AUTOMATIC_KEEP_ALIVE_MANAGEMENT, true);
        this.server.addListener(new ProxyServerListener());
        this.server.bind(false);
    }

    public synchronized void stopServer() {
        SERVER_LOG.info("Stopping server...");
        if (this.server != null && this.server.isListening()) this.server.close(true);
        if (this.lanBroadcaster != null) {
            this.lanBroadcaster.stop();
            this.lanBroadcaster = null;
        }
    }

    public @NonNull MinecraftProtocol logIn() {
        loggingIn.set(true);
        AUTH_LOG.info("Logging in {}...", CONFIG.authentication.username);
        MinecraftProtocol minecraftProtocol = null;
        for (int tries = 0; tries < 3; tries++) {
            minecraftProtocol = retrieveLoginTaskResult(loginTask());
            if (minecraftProtocol != null || !loggingIn.get()) break;
            AUTH_LOG.warn("Failed login attempt " + (tries + 1));
            Wait.wait((int) (3 + (Math.random() * 7.0)));
        }
        if (!loggingIn.get()) throw new RuntimeException("Login Cancelled");
        loggingIn.set(false);
        if (minecraftProtocol == null) throw new RuntimeException("Auth failed");
        var username = minecraftProtocol.getProfile().getName();
        var uuid = minecraftProtocol.getProfile().getId();
        AUTH_LOG.info("Logged in as {} [{}].", username, uuid);
        if (CONFIG.server.extra.whitelist.autoAddClient)
            if (PLAYER_LISTS.getWhitelist().add(username, uuid))
                SERVER_LOG.info("Auto added {} [{}] to whitelist", username, uuid);
        EXECUTOR.execute(this::updateFavicon);
        return minecraftProtocol;
    }

    public Future<MinecraftProtocol> loginTask() {
        return EXECUTOR.submit(() -> {
            try {
                return this.authenticator.login();
            } catch (final Exception e) {
                if (e instanceof InterruptedException) {
                    return null;
                }
                CLIENT_LOG.error("Login failed", e);
                if (e instanceof MinecraftRequestException mre) {
                    if (mre.getResponse().getStatusCode() == 404) {
                        AUTH_LOG.error("[Help] Log into the account with the vanilla MC launcher and join a server. Then try again with ZenithProxy.");
                    }
                }
                return null;
            }
        });
    }

    public MinecraftProtocol retrieveLoginTaskResult(Future<MinecraftProtocol> loginTask) {
        try {
            var maxWait = CONFIG.authentication.accountType == Config.Authentication.AccountType.MSA ? 10 : 300;
            for (int currentWait = 0; currentWait < maxWait; currentWait++) {
                if (loginTask.isDone()) break;
                if (!loggingIn.get()) {
                    loginTask.cancel(true);
                    return null;
                }
                Wait.wait(1);
            }
            return loginTask.get(1L, TimeUnit.SECONDS);
        } catch (Exception e) {
            loginTask.cancel(true);
            return null;
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
        if (autoReconnectIsInProgress()) {
            Future<?> future = this.autoReconnectFuture.get();
            this.autoReconnectFuture = Optional.empty();
            future.cancel(true);
            return true;
        }
        return false;
    }

    public boolean autoReconnectIsInProgress() {
        return this.autoReconnectFuture.isPresent();
    }

    // returns true if we were previously trying to log in
    public boolean cancelLogin() {
        return this.loggingIn.getAndSet(false);
    }

    public List<ServerConnection> getSpectatorConnections() {
        if (getActiveConnections().isEmpty()) return Collections.emptyList();
        if (getActiveConnections().size() == 1 && hasActivePlayer()) return Collections.emptyList();
        return getActiveConnections().stream()
            .filter(ServerConnection::isSpectator)
            .toList();
    }

    public void delayBeforeReconnect() {
        final int countdown = CONFIG.client.extra.autoReconnect.delaySeconds;
        EVENT_BUS.postAsync(new AutoReconnectEvent(countdown));
        // random jitter to help prevent multiple clients from logging in at the same time
        Wait.wait((((int) (Math.random() * 5))) % 10);
        for (int i = countdown; i > 0; i-=10) {
            CLIENT_LOG.info("Reconnecting in {}s", i);
            Wait.wait(10);
        }
    }

    public boolean hasActivePlayer() {
        ServerConnection player = this.currentPlayer.get();
        return player != null && player.isLoggedIn();
    }

    public boolean isPrio() {
        return this.isPrio.orElse(CONFIG.authentication.prio);
    }

    public void updatePrioBanStatus() {
        if (!CONFIG.client.extra.prioBan2b2tCheck || !isOn2b2t()) return;
        this.isPrioBanned = PRIOBAN.checkPrioBan();
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
        var activeHoursConfig = CONFIG.client.extra.utility.actions.activeHours;
        if (!activeHoursConfig.enabled) return;
        if (isOn2b2t() && (this.isPrio() && isConnected())) return;
        if (hasActivePlayer() && !activeHoursConfig.forceReconnect) return;
        if (this.lastActiveHoursConnect.isAfter(Instant.now().minus(Duration.ofHours(1)))) return;

        var queueLength = isOn2b2t()
            ? this.isPrio()
                ? Queue.getQueueStatus().prio()
                : Queue.getQueueStatus().regular()
            : 0;
        var queueWaitSeconds = activeHoursConfig.queueEtaCalc ? Queue.getQueueWait(queueLength) : 0;
        var nowPlusQueueWait = LocalDateTime.now(ZoneId.of(activeHoursConfig.timeZoneId))
            .plusSeconds(queueWaitSeconds)
            .atZone(ZoneId.of(activeHoursConfig.timeZoneId))
            .toInstant();
        var activeTimes = activeHoursConfig.activeTimes.stream()
            .flatMap(activeTime -> {
                var activeHourToday = ZonedDateTime.of(LocalDate.now(ZoneId.of(activeHoursConfig.timeZoneId)), LocalTime.of(activeTime.hour, activeTime.minute), ZoneId.of(activeHoursConfig.timeZoneId));
                var activeHourTomorrow = activeHourToday.plusDays(1L);
                return Stream.of(activeHourToday, activeHourTomorrow);
            })
            .map(ChronoZonedDateTime::toInstant)
            .toList();
        // active hour within 10 mins range of now
        var timeRange = Duration.ofMinutes(5); // x2
        for (Instant activeTime : activeTimes) {
            if (nowPlusQueueWait.isAfter(activeTime.minus(timeRange))
                && nowPlusQueueWait.isBefore(activeTime.plus(timeRange))) {
                MODULE_LOG.info("ActiveHours triggered for time: {}", activeTime);
                EVENT_BUS.postAsync(new ActiveHoursConnectEvent());
                this.lastActiveHoursConnect = Instant.now();
                disconnect(SYSTEM_DISCONNECT);
                EXECUTOR.schedule(this::connectAndCatchExceptions, 1, TimeUnit.MINUTES);
                break;
            }
        }
    }

    public boolean isOnlineOn2b2tForAtLeastDuration(Duration duration) {
        return isOn2b2t() && isOnlineForAtLeastDuration(duration);
    }

    public boolean isOnlineForAtLeastDuration(Duration duration) {
        return isConnected()
            && !isInQueue()
            && nonNull(getConnectTime())
            && getConnectTime().isBefore(Instant.now().minus(duration));
    }

    public void updateFavicon() {
        if (!CONFIG.authentication.username.equals("Unknown")) { // else use default icon
            try (HttpClient httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .connectTimeout(Duration.ofSeconds(5))
                .build()) {
                final GameProfile profile = CACHE.getProfileCache().getProfile();
                final URL avatarURL;
                if (profile != null && profile.getId() != null)
                    avatarURL = getAvatarURL(profile.getId());
                else
                    avatarURL = getAvatarURL(CONFIG.authentication.username);
                final HttpRequest request = HttpRequest.newBuilder()
                    .uri(avatarURL.toURI())
                    .GET()
                    .timeout(Duration.ofSeconds(5))
                    .build();
                final HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
                if (response.statusCode() != 200)
                    throw new IOException("Unable to download server icon for \"" + CONFIG.authentication.username + "\"");
                try (InputStream inputStream = response.body()) {
                    this.serverIcon = inputStream.readAllBytes();
                }
                if (DISCORD.isRunning()) {
                    if (CONFIG.discord.manageNickname)
                        DISCORD.setBotNickname(CONFIG.authentication.username + " | ZenithProxy");
                    if (CONFIG.discord.manageDescription) DISCORD.setBotDescription(
                        """
                        ZenithProxy %s
                        **Official Discord**:
                          https://discord.gg/nJZrSaRKtb"
                        **Github**:
                          https://github.com/rfresh2/ZenithProxy""".formatted(LAUNCH_CONFIG.version));
                }
            } catch (Exception e) {
                SERVER_LOG.error("Unable to download server icon for \"{}\":\n", CONFIG.authentication.username, e);
            }
        }
        if (DISCORD.isRunning())
            if (CONFIG.discord.manageProfileImage) DISCORD.updateProfileImage(this.serverIcon);
    }

    public void twoB2tTimeLimitKickWarningTick() {
        try {
            if (this.isPrio() // Prio players don't get kicked
                || !this.hasActivePlayer() // If no player is connected, nobody to warn
                || !isOnlineOn2b2tForAtLeastDuration(twoB2tTimeLimit.minusMinutes(10L))
            ) return;
            final ServerConnection playerConnection = this.currentPlayer.get();
            final Duration durationUntilKick = twoB2tTimeLimit.minus(Duration.between(this.connectTime, Instant.now()));
            if (durationUntilKick.isNegative()) return; // sanity check just in case 2b's plugin changes
            var actionBarPacket = new ClientboundSetActionBarTextPacket(
                ComponentSerializer.minedown((durationUntilKick.toMinutes() <= 3 ? "&c" : "&9") + twoB2tTimeLimit.toHours() + "hr kick in: " + durationUntilKick.toMinutes() + "m"));
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
            DEFAULT_LOG.error("Error in 2b2t time limit kick warning tick", e);
        }
    }

    public boolean isOn2b2t() {
        return CONFIG.client.server.address.toLowerCase().endsWith("2b2t.org");
    }

    public void handleDisconnectEvent(DisconnectEvent event) {
        CACHE.reset(true);
        this.disconnectTime = Instant.now();
        this.inQueue = false;
        this.queuePosition = 0;
        if (!CONFIG.client.extra.utility.actions.autoDisconnect.autoClientDisconnect) {
            // skip autoreconnect when we want to sync client disconnect
            if (CONFIG.client.extra.autoReconnect.enabled && isReconnectableDisconnect(event.reason())) {
                if (autoReconnectIsInProgress()) return;
                this.autoReconnectFuture = Optional.of(EXECUTOR.submit(() -> {
                    try {
                        delayBeforeReconnect();
                        if (Thread.currentThread().isInterrupted()) return;
                        connect();
                        this.autoReconnectFuture = Optional.empty();
                    } catch (final Exception e) {
                        CLIENT_LOG.info("AutoReconnect stopped");
                    }
                }));
            }
        }
        TPS.reset();
        if (!DISCORD.isRunning()
            && Proxy.getInstance().isOn2b2t()
            && !Proxy.getInstance().isPrio()
            && event.reason().startsWith("You have lost connection")
            && event.onlineDuration().toSeconds() >= 0L
            && event.onlineDuration().toSeconds() <= 1L) {
            CLIENT_LOG.warn("You have likely been kicked for reaching the 2b2t non-prio account IP limit."
                                  + "\nConsider configuring a connection proxy with the `clientConnection` command."
                                  + "\nOr migrate ZenithProxy instances to multiple hosts/IP's.");
        }
    }

    public void handleConnectEvent(ConnectEvent event) {
        this.connectTime = Instant.now();
        cancelAutoReconnect();
    }

    public void handleStartQueueEvent(StartQueueEvent event) {
        this.inQueue = true;
        this.queuePosition = 0;
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
        if (this.isPrio.isEmpty())
            // assume we are prio if we skipped queuing
            EVENT_BUS.postAsync(new PrioStatusEvent(true));
    }

    public void handleServerRestartingEvent(ServerRestartingEvent event) {
        if (!this.isPrio() && isNull(getCurrentPlayer().get())) {
            EXECUTOR.schedule(() -> {
                if (isNull(getCurrentPlayer().get()))
                    disconnect(SERVER_RESTARTING);
            }, ((int) (Math.random() * 20)), TimeUnit.SECONDS);
        }
    }

    public void handlePrioStatusEvent(PrioStatusEvent event) {
        if (!isOn2b2t()) return;
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

    public void handleServerPlayerConnectedEvent(ServerPlayerConnectedEvent event) {
        if (!CONFIG.client.extra.chat.showConnectionMessages) return;
        var serverConnection = getCurrentPlayer().get();
        if (nonNull(serverConnection) && serverConnection.isLoggedIn())
            serverConnection.send(new ClientboundSystemChatPacket(ComponentSerializer.minedown("&b" + event.playerEntry().getName() + "&r&e connected"), false));
    }

    public void handleServerPlayerDisconnectedEvent(ServerPlayerDisconnectedEvent event) {
        if (!CONFIG.client.extra.chat.showConnectionMessages) return;
        var serverConnection = getCurrentPlayer().get();
        if (nonNull(serverConnection) && serverConnection.isLoggedIn())
            serverConnection.send(new ClientboundSystemChatPacket(ComponentSerializer.minedown("&b" + event.playerEntry().getName() + "&r&e disconnected"), false));
    }
}
