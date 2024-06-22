package com.zenith;

import ch.qos.logback.classic.LoggerContext;
import com.zenith.cache.CacheResetType;
import com.zenith.event.proxy.*;
import com.zenith.feature.api.crafthead.CraftheadApi;
import com.zenith.feature.api.mcstatus.MCStatusApi;
import com.zenith.feature.api.minotar.MinotarApi;
import com.zenith.feature.api.prioban.PriobanApi;
import com.zenith.feature.autoupdater.AutoUpdater;
import com.zenith.feature.autoupdater.NoOpAutoUpdater;
import com.zenith.feature.autoupdater.RestAutoUpdater;
import com.zenith.feature.queue.Queue;
import com.zenith.module.impl.AutoReconnect;
import com.zenith.network.client.Authenticator;
import com.zenith.network.client.ClientSession;
import com.zenith.network.server.CustomServerInfoBuilder;
import com.zenith.network.server.LanBroadcaster;
import com.zenith.network.server.ProxyServerListener;
import com.zenith.network.server.ServerConnection;
import com.zenith.network.server.handler.ProxyServerLoginHandler;
import com.zenith.util.ComponentSerializer;
import com.zenith.util.Config;
import com.zenith.util.FastArrayList;
import com.zenith.util.Wait;
import com.zenith.via.ZenithClientChannelInitializer;
import com.zenith.via.ZenithServerChannelInitializer;
import io.netty.util.ResourceLeakDetector;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.SneakyThrows;
import net.raphimc.minecraftauth.responsehandler.exception.MinecraftRequestException;
import org.geysermc.mcprotocollib.auth.GameProfile;
import org.geysermc.mcprotocollib.network.BuiltinFlags;
import org.geysermc.mcprotocollib.network.ProxyInfo;
import org.geysermc.mcprotocollib.network.tcp.TcpConnectionManager;
import org.geysermc.mcprotocollib.network.tcp.TcpServer;
import org.geysermc.mcprotocollib.protocol.MinecraftConstants;
import org.geysermc.mcprotocollib.protocol.MinecraftProtocol;
import org.geysermc.mcprotocollib.protocol.codec.MinecraftCodecHelper;
import org.geysermc.mcprotocollib.protocol.data.game.level.sound.BuiltinSound;
import org.geysermc.mcprotocollib.protocol.data.game.level.sound.SoundCategory;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundSystemChatPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundTabListPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundSoundPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.title.ClientboundSetActionBarTextPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundChatPacket;
import org.jetbrains.annotations.NotNull;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
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
    protected final FastArrayList<ServerConnection> activeConnections = new FastArrayList<>(ServerConnection.class);
    private boolean inQueue = false;
    private int queuePosition = 0;
    @Setter @Nullable private Instant connectTime;
    private Instant disconnectTime = Instant.now();
    private Optional<Boolean> isPrio = Optional.empty();
    private Optional<Boolean> isPrioBanned = Optional.empty();
    @Getter private final AtomicBoolean loggingIn = new AtomicBoolean(false);
    @Setter @NotNull private AutoUpdater autoUpdater = NoOpAutoUpdater.INSTANCE;
    private LanBroadcaster lanBroadcaster;
    // might move to config and make the user deal with it when it changes
    private static final Duration twoB2tTimeLimit = Duration.ofHours(6);
    private TcpConnectionManager tcpManager;

    public static void main(String... args) {
        Locale.setDefault(Locale.ENGLISH);
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();
        if (System.getProperty("io.netty.leakDetection.level") == null)
            ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.DISABLED);
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
        @Nullable String exeReleaseVersion = getExecutableReleaseVersion();
        if (exeReleaseVersion == null) {
            DEFAULT_LOG.warn("Detected unofficial ZenithProxy development build!");
        } else if (!LAUNCH_CONFIG.version.equals(exeReleaseVersion)) {
            DEFAULT_LOG.error("launch_config.json version and actual ZenithProxy version do not match!");
            if (LAUNCH_CONFIG.auto_update)
                DEFAULT_LOG.error("AutoUpdater is enabled but will break!");
            DEFAULT_LOG.error("Use the official launcher: https://github.com/rfresh2/ZenithProxy/releases/tag/launcher-v3");
        }
        initEventHandlers();
        try {
            if (CONFIG.debug.clearOldLogs) EXECUTOR.schedule(Proxy::clearOldLogs, 10L, TimeUnit.SECONDS);
            if (CONFIG.interactiveTerminal.enable) TERMINAL.start();
            MODULE.init();
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
            Queue.start();
            saveConfigAsync();
            MinecraftCodecHelper.useBinaryNbtComponentSerializer = CONFIG.debug.binaryNbtComponentSerializer;
            MinecraftConstants.CHUNK_SECTION_COUNT_PROVIDER = CACHE.getSectionCountProvider();
            this.tcpManager = new TcpConnectionManager();
            startServer();
            CACHE.reset(CacheResetType.FULL);
            EXECUTOR.scheduleAtFixedRate(this::serverHealthCheck, 1L, 5L, TimeUnit.MINUTES);
            EXECUTOR.scheduleAtFixedRate(this::tablistUpdate, 20L, 3L, TimeUnit.SECONDS);
            EXECUTOR.scheduleAtFixedRate(this::updatePrioBanStatus, 0L, 1L, TimeUnit.DAYS);
            EXECUTOR.scheduleAtFixedRate(this::twoB2tTimeLimitKickWarningTick, twoB2tTimeLimit.minusMinutes(10L).toMinutes(), 1L, TimeUnit.MINUTES);
            EXECUTOR.scheduleAtFixedRate(this::maxPlaytimeTick, CONFIG.client.maxPlaytimeReconnectMins, 1L, TimeUnit.MINUTES);
            EXECUTOR.schedule(this::serverConnectionTest, 10L, TimeUnit.SECONDS);
            if (CONFIG.server.enabled && CONFIG.server.ping.favicon)
                EXECUTOR.submit(this::updateFavicon);
            boolean connected = false;
            if (CONFIG.client.autoConnect && !isConnected()) {
                connectAndCatchExceptions();
                connected = true;
            }
            if (!connected && CONFIG.autoUpdater.shouldReconnectAfterAutoUpdate) {
                CONFIG.autoUpdater.shouldReconnectAfterAutoUpdate = false;
                saveConfigAsync();
                if (!CONFIG.client.extra.utility.actions.autoDisconnect.autoClientDisconnect && !isConnected()) {
                    connectAndCatchExceptions();
                    connected = true;
                }
            }
            if (LAUNCH_CONFIG.auto_update) {
                autoUpdater = LAUNCH_CONFIG.release_channel.equals("git")
                    ? NoOpAutoUpdater.INSTANCE
                    : new RestAutoUpdater();
                autoUpdater.start();
                DEFAULT_LOG.info("Started AutoUpdater");
            }
            DEFAULT_LOG.info("ZenithProxy started!");
            if (!DISCORD.isRunning() && LAUNCH_CONFIG.release_channel.endsWith(".pre")) {
                DEFAULT_LOG.warn("You are currently using a ZenithProxy prerelease");
                DEFAULT_LOG.warn("Prereleases include experiments that may contain bugs and are not always updated with fixes");
                DEFAULT_LOG.warn("Switch to a stable release with the `channel` command");
            }
            if (!connected) {
                DEFAULT_LOG.info("Proxy IP: {}", CONFIG.server.getProxyAddress());
                DEFAULT_LOG.info("Use the `connect` command to log in!");
            }
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

    private void serverConnectionTest() {
        if (!CONFIG.server.enabled) return;
        if (server == null || !server.isListening()) return;
        if (!CONFIG.server.ping.enabled) return;
        var address = CONFIG.server.getProxyAddress();
        if (address.startsWith("localhost")) {
            SERVER_LOG.debug("Proxy IP is set to localhost, skipping connection test");
            return;
        }
        MCStatusApi.INSTANCE.getMCServerStatus(CONFIG.server.getProxyAddress())
            .ifPresentOrElse(response -> {
                if (response.online()) {
                    SERVER_LOG.debug("Connection test successful: {}", address);
                } else {
                    SERVER_LOG.error(
                        """
                        Unable to connect to configured `proxyIP`: {}
                        
                        This test is most likely failing due to your firewall needing to be disabled.
                        
                        For instructions on how to disable the firewall consult with your VPS provider. Each provider varies in steps.
                        """, address);
                }
            }, () -> {
                SERVER_LOG.debug("Failed trying to perform connection test");
                // reschedule another attempt?
            });
    }

    private void maxPlaytimeTick() {
        if (CONFIG.client.maxPlaytimeReconnect && isOnlineForAtLeastDuration(Duration.ofMinutes(CONFIG.client.maxPlaytimeReconnectMins))) {
            CLIENT_LOG.info("Max playtime minutes reached: {}, reconnecting...", CONFIG.client.maxPlaytimeReconnectMins);
            disconnect(SYSTEM_DISCONNECT);
            MODULE.get(AutoReconnect.class).cancelAutoReconnect();
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
                MODULE.get(AutoReconnect.class).cancelAutoReconnect();
                stopServer();
                tcpManager.close();
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
        if (this.isConnected()) {
            if (CONFIG.debug.kickDisconnect) this.kickDisconnect(reason, cause);
            else this.client.disconnect(reason, cause);
        }
    }

    public void disconnect(final String reason) {
        if (this.isConnected()) {
            if (CONFIG.debug.kickDisconnect) this.kickDisconnect(reason, null);
            else this.client.disconnect(reason);
        }
    }

    public void kickDisconnect(final String reason, final Throwable cause) {
        if (!isConnected()) return;
        var client = this.client;
        try {
            // out of order timestamp causes server to kick us
            // must send direct to avoid our mitigation in the outgoing packet handler
            client.sendDirect(new ServerboundChatPacket("", -1L, 0L, null, 0, BitSet.valueOf(new byte[20])))
                .get();
        } catch (final Exception e) {
            CLIENT_LOG.error("Error performing kick disconnect", e);
        }
        // note: this will occur before the server sends us back a disconnect packet, but before our channel close is received by the server
        client.disconnect(reason, cause);
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
        connect(CONFIG.client.server.address, CONFIG.client.server.port);
    }

    public synchronized void connect(final String address, final int port) {
        if (this.isConnected()) throw new IllegalStateException("Already connected!");
        this.connectTime = Instant.now();
        final MinecraftProtocol minecraftProtocol;
        try {
            EVENT_BUS.postAsync(new StartConnectEvent());
            minecraftProtocol = this.logIn();
        } catch (final Exception e) {
            EVENT_BUS.post(new ProxyLoginFailedEvent());
            var connections = getActiveConnections().getArray();
            for (int i = 0; i < connections.length; i++) {
                var connection = connections[i];
                connection.disconnect("Login failed");
            }
            EXECUTOR.schedule(() -> EVENT_BUS.post(new DisconnectEvent(LOGIN_FAILED)), 1L, TimeUnit.SECONDS);
            return;
        }
        CLIENT_LOG.info("Connecting to {}:{}...", address, port);
        this.client = new ClientSession(address, port, CONFIG.client.bindAddress, minecraftProtocol, getClientProxyInfo(), tcpManager);
        if (Objects.equals(address, "connect.2b2t.org"))
            this.client.setFlag(BuiltinFlags.ATTEMPT_SRV_RESOLVE, false);
        this.client.setReadTimeout(CONFIG.client.timeout.enable ? CONFIG.client.timeout.seconds : 0);
        this.client.setFlag(MinecraftConstants.CLIENT_CHANNEL_INITIALIZER, ZenithClientChannelInitializer.FACTORY);
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
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("servericon.png")) {
            this.serverIcon = in.readAllBytes();
        }
        var address = CONFIG.server.bind.address;
        var port = CONFIG.server.bind.port;
        SERVER_LOG.info("Starting server on {}:{}...", address, port);
        this.server = new TcpServer(address, port, MinecraftProtocol::new, tcpManager);
        this.server.setGlobalFlag(MinecraftConstants.SERVER_CHANNEL_INITIALIZER, ZenithServerChannelInitializer.FACTORY);
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
            SERVER_LOG.error("Failed to get avatar URL for player: " + playerName, e);
            throw new UncheckedIOException(e);
        }
    }

    // returns true if we were previously trying to log in
    public boolean cancelLogin() {
        return this.loggingIn.getAndSet(false);
    }

    public List<ServerConnection> getSpectatorConnections() {
        var connections = getActiveConnections().getArray();
        // optimize most frequent cases as fast-paths to avoid list alloc
        if (connections.length == 0) return Collections.emptyList();
        if (connections.length == 1 && hasActivePlayer()) return Collections.emptyList();
        final List<ServerConnection> result = new ArrayList<>(hasActivePlayer() ? connections.length - 1 : connections.length);
        for (int i = 0; i < connections.length; i++) {
            var connection = connections[i];
            if (connection.isSpectator()) {
                result.add(connection);
            }
        }
        return result;
    }

    public boolean hasActivePlayer() {
        ServerConnection player = this.currentPlayer.get();
        return player != null && player.isLoggedIn();
    }

    public @Nullable ServerConnection getActivePlayer() {
        ServerConnection player = this.currentPlayer.get();
        if (player != null && player.isLoggedIn()) return player;
        else return null;
    }

    public boolean isPrio() {
        return this.isPrio.orElse(CONFIG.authentication.prio);
    }

    public void updatePrioBanStatus() {
        if (!CONFIG.client.extra.prioBan2b2tCheck || !isOn2b2t()) return;
        this.isPrioBanned = PriobanApi.INSTANCE.checkPrioBan();
        if (this.isPrioBanned.isPresent() && !this.isPrioBanned.get().equals(CONFIG.authentication.prioBanned)) {
            EVENT_BUS.postAsync(new PrioBanStatusUpdateEvent(this.isPrioBanned.get()));
            CONFIG.authentication.prioBanned = this.isPrioBanned.get();
            saveConfigAsync();
            CLIENT_LOG.info("Prio Ban Change Detected: " + this.isPrioBanned.get());
        }
    }

    public void kickNonWhitelistedPlayers() {
        var connections = Proxy.getInstance().getActiveConnections().getArray();
        for (int i = 0; i < connections.length; i++) {
            var connection = connections[i];
            if (connection.getProfileCache().getProfile() == null) continue;
            if (PLAYER_LISTS.getWhitelist().contains(connection.getProfileCache().getProfile())) continue;
            if (PLAYER_LISTS.getSpectatorWhitelist().contains(connection.getProfileCache().getProfile()) && connection.isSpectator()) continue;
            connection.disconnect("Not whitelisted");
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
            try {
                final GameProfile profile = CACHE.getProfileCache().getProfile();
                if (profile != null && profile.getId() != null) {
                    // do uuid lookup
                    final UUID uuid = profile.getId();
                    this.serverIcon = MinotarApi.INSTANCE.getAvatar(uuid).or(() -> CraftheadApi.INSTANCE.getAvatar(uuid))
                        .orElseThrow(() -> new IOException("Unable to download server icon for \"" + uuid + "\""));
                } else {
                    // do username lookup
                    final String username = CONFIG.authentication.username;
                    this.serverIcon = MinotarApi.INSTANCE.getAvatar(username).or(() -> CraftheadApi.INSTANCE.getAvatar(username))
                        .orElseThrow(() -> new IOException("Unable to download server icon for \"" + username + "\""));
                }
                if (DISCORD.isRunning()) {
                    if (CONFIG.discord.manageNickname)
                        DISCORD.setBotNickname(CONFIG.authentication.username + " | ZenithProxy");
                    if (CONFIG.discord.manageDescription) DISCORD.setBotDescription(
                        """
                        ZenithProxy %s
                        **Official Discord**:
                          https://discord.gg/nJZrSaRKtb
                        **Github**:
                          https://github.com/rfresh2/ZenithProxy
                        """.formatted(LAUNCH_CONFIG.version));
                }
            } catch (final Throwable e) {
                SERVER_LOG.error("Failed updating favicon");
                SERVER_LOG.debug("Failed updating favicon", e);
            }
        }
        if (DISCORD.isRunning() && this.serverIcon != null)
            if (CONFIG.discord.manageProfileImage) DISCORD.updateProfileImage(this.serverIcon);
    }

    public void twoB2tTimeLimitKickWarningTick() {
        try {
            if (this.isPrio() // Prio players don't get kicked
                || !this.hasActivePlayer() // If no player is connected, nobody to warn
                || !isOnlineOn2b2tForAtLeastDuration(twoB2tTimeLimit.minusMinutes(10L))
            ) return;
            final ServerConnection playerConnection = this.currentPlayer.get();
            final Duration durationUntilKick = twoB2tTimeLimit.minus(Duration.ofSeconds(Proxy.getInstance().getOnlineTimeSeconds()));
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

    public long getOnlineTimeSeconds() {
        var proxyConnectTime = this.connectTime;
        return proxyConnectTime != null
            ? TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()) - proxyConnectTime.getEpochSecond()
            : 0L;
    }

    public String getOnlineTimeString() {
        return Queue.getEtaStringFromSeconds(getOnlineTimeSeconds());
    }

    public void handleDisconnectEvent(DisconnectEvent event) {
        CACHE.reset(CacheResetType.FULL);
        this.disconnectTime = Instant.now();
        this.inQueue = false;
        this.queuePosition = 0;
        TPS.reset();
        if (!DISCORD.isRunning()
            && isOn2b2t()
            && !isPrio()
            && event.reason().startsWith("You have lost connection")) {
            if (event.onlineDuration().toSeconds() >= 0L
                && event.onlineDuration().toSeconds() <= 1L) {
                CLIENT_LOG.warn("""
                                You have likely been kicked for reaching the 2b2t non-prio account IP limit.
                                Consider configuring a connection proxy with the `clientConnection` command.
                                Or migrate ZenithProxy instances to multiple hosts/IP's.
                                """);
            } else if (event.wasInQueue() && event.queuePosition() <= 1) {
                CLIENT_LOG.warn("""
                                You have likely been kicked due to being IP banned by 2b2t.
                                                              
                                To check, try connecting and waiting through queue with the same account from a different IP.
                                """);
            }
        }
    }

    public void handleConnectEvent(ConnectEvent event) {
        this.connectTime = Instant.now();
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
