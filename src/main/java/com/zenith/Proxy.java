package com.zenith;

import com.collarmc.pounce.Preference;
import com.collarmc.pounce.Subscribe;
import com.github.steveice10.mc.protocol.MinecraftConstants;
import com.github.steveice10.mc.protocol.MinecraftProtocol;
import com.github.steveice10.packetlib.BuiltinFlags;
import com.github.steveice10.packetlib.tcp.TcpServer;
import com.zenith.client.ClientSession;
import com.zenith.database.ConnectionPool;
import com.zenith.database.Database;
import com.zenith.database.QueueWaitDatabase;
import com.zenith.event.proxy.*;
import com.zenith.module.ModuleManager;
import com.zenith.pathing.World;
import com.zenith.pathing.blockdata.BlockDataManager;
import com.zenith.server.CustomServerInfoBuilder;
import com.zenith.server.ProxyServerListener;
import com.zenith.server.ServerConnection;
import com.zenith.server.handler.ProxyServerLoginHandler;
import com.zenith.util.Queue;
import com.zenith.util.*;
import lombok.Getter;
import lombok.Setter;
import reactor.netty.http.client.HttpClient;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.zenith.util.Constants.*;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;


@Getter
public class Proxy {
    @Getter
    protected static Proxy instance;

    protected MinecraftProtocol protocol;
    protected ClientSession client;
    protected TcpServer server;
    protected LoggerInner loggerInner;
    @Setter
    protected BufferedImage serverIcon;
    protected final AtomicReference<ServerConnection> currentPlayer = new AtomicReference<>();
    protected final CopyOnWriteArrayList<ServerConnection> activeConnections = new CopyOnWriteArrayList<>();
    protected final ScheduledExecutorService clientTimeoutExecutorService;
    protected ScheduledExecutorService autoReconnectExecutorService;
    protected ScheduledExecutorService activeHoursExecutorService;
    protected ScheduledExecutorService reconnectExecutorService;
    protected ScheduledExecutorService whitelistUpdateExecutorService;
    protected Database queueWait; // nullable
    protected final TPSCalculator tpsCalculator;

    private int reconnectCounter;
    private boolean inQueue = false;
    private int queuePosition = 0;
    private Instant connectTime;
    private Instant disconnectTime = Instant.now();
    private Optional<Boolean> isPrio = Optional.empty();
    private Optional<Boolean> isPrioBanned = Optional.empty();
    volatile private Optional<Future<?>> autoReconnectFuture = Optional.empty();
    private Instant lastActiveHoursConnect = Instant.EPOCH;
    private final PriorityBanChecker priorityBanChecker;
    public static AutoUpdater autoUpdater;
    public final World world;
    private final ModuleManager moduleManager;

    public static void main(String... args) {
        DEFAULT_LOG.info("Starting Proxy...");

        instance = new Proxy();
        autoUpdater = new AutoUpdater(instance);
        if (CONFIG.discord.enable) {
            DISCORD_LOG.info("Starting discord bot...");
            try {
                DISCORD_BOT.start(instance);
            } catch (final Throwable e) {
                DISCORD_LOG.error("", e);
            }
        }

        instance.start();
    }

    public Proxy() {
        this.clientTimeoutExecutorService = Executors.newSingleThreadScheduledExecutor();
        this.autoReconnectExecutorService = Executors.newSingleThreadScheduledExecutor();
        this.activeHoursExecutorService = Executors.newSingleThreadScheduledExecutor();
        this.reconnectExecutorService = Executors.newSingleThreadScheduledExecutor();
        this.whitelistUpdateExecutorService = Executors.newSingleThreadScheduledExecutor();
        this.priorityBanChecker = new PriorityBanChecker();
        this.world = new World(new BlockDataManager());
        EVENT_BUS.subscribe(this);
        if (CONFIG.database.queueWait.enabled) {
            // if we add more databases, ensure the connectionpool is shared
            this.queueWait = new QueueWaitDatabase(new ConnectionPool(), this);
        }
        this.tpsCalculator = new TPSCalculator();
        this.moduleManager = new ModuleManager(this);
    }

    public void start() {
        try {
            saveConfig();
            if (CONFIG.server.extra.timeout.enable) {
                long millis = CONFIG.server.extra.timeout.millis;
                long interval = CONFIG.server.extra.timeout.interval;
                clientTimeoutExecutorService.scheduleAtFixedRate(() -> {
                    ServerConnection currentPlayer = this.currentPlayer.get();
                    if (currentPlayer != null && currentPlayer.isConnected() && System.currentTimeMillis() - currentPlayer.getLastPacket() >= millis)  {
                        currentPlayer.disconnect("Timed out");
                    }
                }, 0, interval, TimeUnit.MILLISECONDS);
            }
            this.startServer();
            CACHE.reset(true);
            activeHoursExecutorService.scheduleAtFixedRate(this::handleActiveHoursTick, 1L, 1L, TimeUnit.MINUTES);
            if (CONFIG.client.extra.sixHourReconnect) {
                reconnectExecutorService.scheduleAtFixedRate(() -> {
                    try {
                        if (this.isConnected() && !inQueue && nonNull(connectTime)) {
                            long onlineSeconds = Instant.now().getEpochSecond() - connectTime.getEpochSecond();
                            if (onlineSeconds > (21600 - 60)) { // 6 hrs - 60 seconds padding
                                this.disconnect(SYSTEM_DISCONNECT);
                                this.cancelAutoReconnect();
                                this.connect();
                            }
                        }
                    } catch (final Throwable e) {
                        DEFAULT_LOG.error("Error in reconnect executor service", e);
                    }
                }, 0, 10L, TimeUnit.SECONDS);
            }
            updatePrioBanStatus();
            if (CONFIG.server.enabled && CONFIG.server.ping.favicon) {
                try {
                    InputStream netInputStream = HttpClient.create()
                            .secure()
                            .get()
                            .uri(getAvatarURL(CONFIG.authentication.username).toURI())
                            .responseContent()
                            .aggregate()
                            .asInputStream()
                            .block();
                    this.serverIcon = ImageIO.read(netInputStream);
                    if (nonNull(DISCORD_BOT)) {
                        DISCORD_BOT.updateProfileImage(this.serverIcon);
                    }
                } catch (Exception e) {
                    SERVER_LOG.error("Unable to download server icon for \"{}\":\n", CONFIG.authentication.username, e);
                }
            }
            if (CONFIG.client.autoConnect) {
                this.connect();
            }
            if (CONFIG.autoUpdate) {
                autoUpdater.start();
            }
            if (CONFIG.shouldReconnectAfterAutoUpdate) {
                CONFIG.shouldReconnectAfterAutoUpdate = false;
                saveConfig();
                if (!CONFIG.client.extra.utility.actions.autoDisconnect.autoClientDisconnect) {
                    this.connect();
                }
            }
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

    public void stop() {
        DEFAULT_LOG.info("Shutting Down...");
        if (nonNull(this.server)) {
            this.server.close(true);
        }
        saveConfig();
        while (!DISCORD_BOT.isMessageQueueEmpty()) {
            Wait.waitALittleMs(100);
        }
        System.exit(0);
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

    public synchronized void connect() {
        try {
            EVENT_BUS.dispatch(new StartConnectEvent());
            this.logIn();
        } catch (final RuntimeException e) {
            EVENT_BUS.dispatch(new ProxyLoginFailedEvent());
            getActiveConnections().forEach(connection -> connection.disconnect("Login failed"));
            ForkJoinPool.commonPool().submit(() -> {
                // mitigate possible race condition in autoreconnect
                Wait.waitALittle(1);
                EVENT_BUS.dispatch(new DisconnectEvent("Login Failed"));
            });
            return;
        }

        if (this.isConnected()) {
            throw new IllegalStateException("Already connected!");
        }

        String address = CONFIG.client.server.address;
        int port = CONFIG.client.server.port;

        CLIENT_LOG.info("Connecting to {}:{}...", address, port);
        this.client = new ClientSession(address, port, this.protocol, this);
        if (Objects.equals(CONFIG.client.server.address, "connect.2b2t.org")) {
            this.client.setFlag(BuiltinFlags.ATTEMPT_SRV_RESOLVE, false);
        }
        this.client.setFlag(BuiltinFlags.PRINT_DEBUG, true);
        this.client.connect(true);
    }


    public boolean isConnected() {
        return this.client != null && this.client.isConnected();
    }

    public void startServer() {
        synchronized (this) {
            if (this.server != null) {
                throw new IllegalStateException("Server already started!");
            }
            if (CONFIG.server.enabled) {
                try (InputStream is = new ByteArrayInputStream(Base64.getDecoder().decode("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"))) {
                    this.serverIcon = ImageIO.read(is);
                } catch (IOException e) {
                    //impossible
                    throw new AssertionError();
                }

                String address = CONFIG.server.bind.address;
                int port = CONFIG.server.bind.port;

                SERVER_LOG.info("Starting server on {}:{}...", address, port);
                this.server = new TcpServer(address, port, MinecraftProtocol::new);
                this.server.setGlobalFlag(MinecraftConstants.AUTH_PROXY_KEY, java.net.Proxy.NO_PROXY);
                this.server.setGlobalFlag(MinecraftConstants.VERIFY_USERS_KEY, CONFIG.server.verifyUsers);
                this.server.setGlobalFlag(MinecraftConstants.SERVER_INFO_BUILDER_KEY, new CustomServerInfoBuilder(this));
                this.server.setGlobalFlag(MinecraftConstants.SERVER_LOGIN_HANDLER_KEY, new ProxyServerLoginHandler(this));
                this.server.setGlobalFlag(MinecraftConstants.SERVER_COMPRESSION_THRESHOLD, CONFIG.server.compressionThreshold);
                this.server.setGlobalFlag(MinecraftConstants.SERVER_KEEPALIVE_INTERVAL_MS, 10000);
                this.server.addListener(new ProxyServerListener(this));
                this.server.bind(false);
            }
        }
    }

    public void logIn() {
        AUTH_LOG.info("Logging in...");
        if (this.loggerInner == null) {
            this.loggerInner = new LoggerInner();
        }
        int tries = 0;
        while (tries < 3 && !retrieveLoginTaskResult(loginTask())) {
            tries++;
            AUTH_LOG.warn("Failed login attempt " + tries);
            // wait random time between 3 and 10 seconds
            Wait.waitALittle((int) (3 + (Math.random() * 7.0)));
        }
        if (tries == 3) {
            throw new RuntimeException("Auth failed");
        }
        CACHE.getProfileCache().setProfile(this.protocol.getProfile());
        AUTH_LOG.info("Logged in.");
    }

    public Future<Boolean> loginTask() {
        return ForkJoinPool.commonPool().submit(() -> {
            try {
                this.protocol = this.loggerInner.handleRelog();
                return true;
            } catch (final Exception e) {
                CLIENT_LOG.error("", e);
                return false;
            }
        });
    }

    public boolean retrieveLoginTaskResult(Future<Boolean> loginTask) {
        try {
            return loginTask.get(10L, TimeUnit.SECONDS);
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
            return new URL(String.format("https://minotar.net/helm/%s/64", playerName));
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

    private boolean autoReconnectIsInProgress() {
        return this.autoReconnectFuture.isPresent();
    }

    public List<ServerConnection> getSpectatorConnections() {
        return getActiveConnections().stream()
                .filter(ServerConnection::isSpectator)
                .collect(Collectors.toList());
    }

    @Subscribe(value = Preference.CALLER)
    public void handleDisconnectEvent(DisconnectEvent event) {
        CACHE.reset(true);
        this.disconnectTime = Instant.now();
        this.inQueue = false;
        this.queuePosition = 0;
        if (!CONFIG.client.extra.utility.actions.autoDisconnect.autoClientDisconnect) {
            // skip autoreconnect when we want to sync client disconnect
            if (CONFIG.client.extra.autoReconnect.enabled && isReconnectableDisconnect(event.reason)) {
                if (autoReconnectIsInProgress()) {
                    return;
                }
                this.autoReconnectFuture = Optional.of(this.autoReconnectExecutorService.submit(() -> {
                    DISCORD_BOT.sendAutoReconnectMessage();
                    delayBeforeReconnect();
                    synchronized (this.autoReconnectFuture) {
                        if (this.autoReconnectFuture.isPresent()) this.connect();
                        this.autoReconnectFuture = Optional.empty();
                    }
                }));
            }
        }
        this.tpsCalculator.reset();
    }

    public void delayBeforeReconnect() {
        try {
            final int countdown;
            if (nonNull(client) && ((ClientSession) client).isServerProbablyOff()) {
                countdown = CONFIG.client.extra.autoReconnect.delaySecondsOffline;
                this.reconnectCounter = 0;
            } else {
                countdown = CONFIG.client.extra.autoReconnect.delaySeconds
                        + CONFIG.client.extra.autoReconnect.linearIncrease * this.reconnectCounter++;
            }
            for (int i = countdown; SHOULD_RECONNECT && i > 0; i--) {
                if (i % 10 == 0) CLIENT_LOG.info("Reconnecting in {}", i);
                Wait.waitALittle(1);
            }
        } catch (Exception e) {
            CLIENT_LOG.error("AutoReconnect delay failure", e);
        }
    }

    public void updatePrioBanStatus() {
        if (!CONFIG.client.server.address.toLowerCase(Locale.ROOT).contains("2b2t.org")) return;
        this.isPrioBanned = this.priorityBanChecker.checkPrioBan();
        if (this.isPrioBanned.isPresent() && !this.isPrioBanned.get().equals(CONFIG.authentication.prioBanned)) {
            EVENT_BUS.dispatch(new PrioBanStatusUpdateEvent(this.isPrioBanned.get()));
            CONFIG.authentication.prioBanned = this.isPrioBanned.get();
            saveConfig();
            CLIENT_LOG.info("Prio Ban Change Detected: " + this.isPrioBanned.get());
        }
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
            Integer queueLength = (CONFIG.authentication.prio ? Queue.getQueueStatus().prio : Queue.getQueueStatus().regular);
            double queueWaitSeconds = Queue.getQueueWait(queueLength);
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
                        EVENT_BUS.dispatch(new ActiveHoursConnectEvent());
                        this.lastActiveHoursConnect = Instant.now();
                        disconnect(SYSTEM_DISCONNECT);
                        connect();
                    });
        }
    }

    @Subscribe
    public void handleConnectEvent(ConnectEvent event) {
        this.connectTime = Instant.now();
        cancelAutoReconnect();
    }

    @Subscribe
    public void handleStartQueueEvent(StartQueueEvent event) {
        this.inQueue = true;
        this.queuePosition = 0;
        updatePrioBanStatus();
    }

    @Subscribe
    public void handleQueuePositionUpdateEvent(QueuePositionUpdateEvent event) {
        this.queuePosition = event.position;
    }

    @Subscribe
    public void handleQueueCompleteEvent(QueueCompleteEvent event) {
        this.inQueue = false;
        this.connectTime = Instant.now();
    }

    @Subscribe
    public void handlePlayerOnlineEvent(PlayerOnlineEvent event) {
        if (!this.isPrio.isPresent()) {
            // assume we are prio if we skipped queuing
            EVENT_BUS.dispatch(new PrioStatusEvent(true));
        }
    }

    @Subscribe
    public void handleProxyClientDisconnectedEvent(ProxyClientDisconnectedEvent event) {
        if (CONFIG.client.extra.utility.actions.autoDisconnect.autoClientDisconnect) {
            ServerConnection currentConnection = this.getCurrentPlayer().get();
            if (nonNull(currentConnection) && currentConnection.getProfileCache().getProfile().equals(event.clientGameProfile))
            {
                disconnect();
            }
        }
    }

    @Subscribe
    public void handleServerRestartingEvent(ServerRestartingEvent event) {
        if (!CONFIG.authentication.prio && isNull(getCurrentPlayer().get())) {
            disconnect(SERVER_RESTARTING, new Exception());
        }
    }

    @Subscribe
    public void handlePrioStatusEvent(PrioStatusEvent event) {
        if (CONFIG.client.server.address.toLowerCase().contains("2b2t.org")) {
            if (event.prio == CONFIG.authentication.prio) {
                if (!isPrio.isPresent()) {
                    CLIENT_LOG.info("Prio Detected: " + event.prio);
                    this.isPrio = Optional.of(event.prio);
                }
            } else {
                CLIENT_LOG.info("Prio Change Detected: " + event.prio);
                EVENT_BUS.dispatch(new PrioStatusUpdateEvent(event.prio));
                CONFIG.authentication.prio = event.prio;
                saveConfig();
            }
        }
    }
}
