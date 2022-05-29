/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2016-2020 DaPorkchop_
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software
 * is furnished to do so, subject to the following conditions:
 *
 * Any persons and/or organizations using this software must include the above copyright notice and this permission notice,
 * provide sufficient credit to the original authors of the project (IE: DaPorkchop_), as well as provide a link to the original project.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS
 * BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

package com.zenith;

import com.collarmc.pounce.Preference;
import com.collarmc.pounce.Subscribe;
import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.mc.protocol.MinecraftConstants;
import com.github.steveice10.mc.protocol.MinecraftProtocol;
import com.github.steveice10.mc.protocol.ServerLoginHandler;
import com.github.steveice10.mc.protocol.data.SubProtocol;
import com.github.steveice10.mc.protocol.data.game.ClientRequest;
import com.github.steveice10.mc.protocol.packet.ingame.client.ClientRequestPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerJoinGamePacket;
import com.github.steveice10.packetlib.BuiltinFlags;
import com.github.steveice10.packetlib.tcp.TcpClientSession;
import com.github.steveice10.packetlib.tcp.TcpServer;
import com.zenith.client.PorkClientSession;
import com.zenith.event.module.ClientTickEvent;
import com.zenith.event.proxy.*;
import com.zenith.module.AntiAFK;
import com.zenith.module.Module;
import com.zenith.server.CustomServerInfoBuilder;
import com.zenith.server.PorkServerConnection;
import com.zenith.server.PorkServerListener;
import com.zenith.util.*;
import lombok.Getter;
import lombok.Setter;
import net.daporkchop.lib.common.util.PorkUtil;

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
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static com.zenith.util.Constants.*;
import static java.util.Arrays.asList;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

/**
 * @author DaPorkchop_
 */
@Getter
public class Proxy {
    @Getter
    protected static Proxy instance;

    //protected final Collection<PorkServerConnection> serverConnections = Collections.newSetFromMap(new ConcurrentHashMap<>());
    protected MinecraftProtocol protocol;
    protected TcpClientSession client;
    protected TcpServer server;
    protected LoggerInner loggerInner;
    @Setter
    protected BufferedImage serverIcon;
    protected final AtomicReference<PorkServerConnection> currentPlayer = new AtomicReference<>();
    protected final ScheduledExecutorService clientTickExecutorService;
    protected final ScheduledExecutorService clientTimeoutExecutorService;
    protected ScheduledExecutorService autoReconnectExecutorService;
    protected ScheduledExecutorService activeHoursExecutorService;
    protected ScheduledExecutorService reconnectExecutorService;
    protected List<Module> modules;

    private int reconnectCounter;
    private boolean inQueue = false;
    private int queuePosition = 0;
    private Instant connectTime;
    private Optional<Boolean> isPrio = Optional.empty();
    volatile private Optional<Future<?>> autoReconnectFuture = Optional.empty();
    private Instant lastActiveHoursConnect = Instant.EPOCH;

//    protected final Gui gui = new Gui();

    public static void main(String... args) {
        DEFAULT_LOG.info("Starting Proxy v%s...", VERSION);

        if (CONFIG.websocket.enable) {
            WEBSOCKET_LOG.info("Starting WebSocket server...");
            WEBSOCKET_SERVER.start();
        }

        instance = new Proxy();

        if (CONFIG.discord.enable) {
            DISCORD_LOG.info("Starting discord bot...");
            ForkJoinPool.commonPool().submit(() -> {
                try {
                    DISCORD_BOT.start(instance);
                } catch (final Throwable e) {
                    DISCORD_LOG.error(e);
                }
            });
        }

        instance.start();
    }

    public Proxy() {
        this.clientTickExecutorService = new ScheduledThreadPoolExecutor(1);
        this.clientTimeoutExecutorService = new ScheduledThreadPoolExecutor(1);
        this.autoReconnectExecutorService = new ScheduledThreadPoolExecutor(1);
        this.activeHoursExecutorService = new ScheduledThreadPoolExecutor(1);
        this.reconnectExecutorService = new ScheduledThreadPoolExecutor(1);
        EVENT_BUS.subscribe(this);
    }

    public void start() {
        try {
            saveConfig();
            registerModules();
            if (CONFIG.server.extra.timeout.enable) {
                long millis = CONFIG.server.extra.timeout.millis;
                long interval = CONFIG.server.extra.timeout.interval;
                clientTimeoutExecutorService.scheduleAtFixedRate(() -> {
                    PorkServerConnection currentPlayer = this.currentPlayer.get();
                    if (currentPlayer != null && currentPlayer.isConnected() && System.currentTimeMillis() - currentPlayer.getLastPacket() >= millis)  {
                        currentPlayer.disconnect("Timed out");
                    }
                }, 0, interval, TimeUnit.MILLISECONDS);
            }
            this.startServer();
            CACHE.reset(true);
            clientTickExecutorService.scheduleAtFixedRate(() -> {
                if (this.isConnected()
                        && ((MinecraftProtocol) this.client.getPacketProtocol()).getSubProtocol() == SubProtocol.GAME
                        && isNull(this.currentPlayer.get())) {
                    MODULE_EXECUTOR_SERVICE.execute(() -> EVENT_BUS.dispatch(new ClientTickEvent()));
                }
            }, 0, 50L, TimeUnit.MILLISECONDS);
            activeHoursExecutorService.scheduleAtFixedRate(this::handleActiveHoursTick, 1L, 1L, TimeUnit.MINUTES);
            reconnectExecutorService.scheduleAtFixedRate(() -> {
                if (this.isConnected() && !inQueue && nonNull(connectTime)) {
                    long onlineSeconds = Instant.now().getEpochSecond() - connectTime.getEpochSecond();
                    if (onlineSeconds > (21600 - 100)) {
                        this.disconnect();
                        this.cancelAutoReconnect();
                        this.connect();
                    }
                }
            }, 0, 200L, TimeUnit.MILLISECONDS);
            if (CONFIG.client.autoConnect) {
                this.connect();
            }
            Wait.waitSpinLoop();
        } catch (Exception e) {
            DEFAULT_LOG.alert(e);
        } finally {
            DEFAULT_LOG.info("Shutting down...");
            if (this.server != null) {
                this.server.close(true);
            }
            WEBSOCKET_SERVER.shutdown();
            saveConfig();
        }
    }

    public void stop() {
        DEFAULT_LOG.info("Shutting Down...");
        if (nonNull(this.server)) {
            this.server.close(true);
        }
        WEBSOCKET_SERVER.shutdown();
        saveConfig();
        System.exit(0);
    }

    public void disconnect() {
        CACHE.reset(true);
        if (this.isConnected()) {
            this.client.disconnect(MANUAL_DISCONNECT);
        }
    }

    void registerModules() {
        // todo: do some reflection magic to auto-register modules in the package
        this.modules = asList(
                new AntiAFK(this)
        );

        // todo: make this into a module
        //  too lazy to do this rn bc this is not very useful
//        if (CONFIG.client.extra.spammer.enabled) {
//            List<String> messages = CONFIG.client.extra.spammer.messages;
//            int delaySeconds = CONFIG.client.extra.spammer.delaySeconds;
//            AtomicInteger i = new AtomicInteger(0);
//            MODULE_LOG.trace("Enabling spammer with %d messages, choosing every %d seconds", messages.size(), delaySeconds);
//            modules.add(() -> {
//                if ((i.getAndIncrement() >> 1) == delaySeconds) {
//                    i.set(0);
//                    this.client.getSession().send(new ClientChatPacket(messages.get(ThreadLocalRandom.current().nextInt(messages.size()))));
//                }
//            });
//        }
    }

    public void connect() {
        synchronized (this) {
            this.logIn();
            if (this.isConnected()) {
                throw new IllegalStateException("Already connected!");
            }

            String address = CONFIG.client.server.address;
            int port = CONFIG.client.server.port;

            CLIENT_LOG.info("Connecting to %s:%d...", address, port);
            this.client = new PorkClientSession(address, port, this.protocol, this);
            this.client.setFlag(BuiltinFlags.PRINT_DEBUG, true);
            this.client.connect(true);
        }
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

                SERVER_LOG.info("Starting server on %s:%d...", address, port);
                this.server = new TcpServer(address, port, MinecraftProtocol::new);
                this.server.setGlobalFlag(MinecraftConstants.AUTH_PROXY_KEY, java.net.Proxy.NO_PROXY);
                this.server.setGlobalFlag(MinecraftConstants.VERIFY_USERS_KEY, CONFIG.server.verifyUsers);
                this.server.setGlobalFlag(MinecraftConstants.SERVER_INFO_BUILDER_KEY, new CustomServerInfoBuilder(this));
                this.server.setGlobalFlag(MinecraftConstants.SERVER_LOGIN_HANDLER_KEY, (ServerLoginHandler) session -> {
                    // todo: extract this to its own class implementing ServerLoginHandler
                    PorkServerConnection connection = ((PorkServerListener) this.server.getListeners().stream()
                            .filter(PorkServerListener.class::isInstance)
                            .findAny().orElseThrow(IllegalStateException::new))
                            .getConnections().get(session);
                    if (!this.currentPlayer.compareAndSet(null, connection)) {
                        if (CONFIG.server.kickPrevious) {
                            this.currentPlayer.get().setPlayer(false);
                            this.currentPlayer.get().disconnect("A new player has connected!");
                            this.currentPlayer.set(connection);
                        } else {
                            connection.disconnect("§cA client is already connected to this bot!");
                            return;
                        }
                    }
                    connection.setPlayer(true);
                    session.send(new ServerJoinGamePacket(
                            CACHE.getPlayerCache().getEntityId(),
                            CACHE.getPlayerCache().isHardcore(),
                            CACHE.getPlayerCache().getGameMode(),
                            CACHE.getPlayerCache().getDimension(),
                            CACHE.getPlayerCache().getDifficulty(),
                            CACHE.getPlayerCache().getMaxPlayers(),
                            CACHE.getPlayerCache().getWorldType(),
                            CACHE.getPlayerCache().isReducedDebugInfo()
                    ));
                    SERVER_LOG.info("Player connected: %s", session.getRemoteAddress());
                    if (this.currentPlayer.get() != connection) {
                        SERVER_LOG.alert("login handler fired when session wasn't set yet...");
                    }
                    GameProfile clientGameProfile = session.getFlag(MinecraftConstants.PROFILE_KEY);
                    EVENT_BUS.dispatch(new ProxyClientConnectedEvent(clientGameProfile));
                });
                this.server.setGlobalFlag(MinecraftConstants.SERVER_COMPRESSION_THRESHOLD, CONFIG.server.compressionThreshold);
                this.server.addListener(new PorkServerListener(this));
                this.server.bind(false);
            }
        }
    }

    public void logIn() {
        AUTH_LOG.info("Logging in...");
        if (this.loggerInner == null) {
            this.loggerInner = new LoggerInner();
        }
        this.protocol = this.loggerInner.handleRelog();
        if (CONFIG.server.enabled && CONFIG.server.ping.favicon) {
            ForkJoinPool.commonPool().execute(() -> {
                try {
                    this.serverIcon = ImageIO.read(getAvatarURL(this.protocol.getProfile().getId()));
                } catch (IOException e) {
                    System.err.printf("Unable to download server icon for \"%s\":\n", this.protocol.getProfile().getName());
                    e.printStackTrace();
                }
            });
        }
        CACHE.getProfileCache().setProfile(this.protocol.getProfile());
        AUTH_LOG.success("Logged in.");
    }

    public URL getAvatarURL(UUID uuid) {
        try {
            return new URL(String.format("https://crafatar.com/avatars/%s?size=64&overlay&default=MHF_Steve", uuid.toString()));
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

    @Subscribe(value = Preference.CALLER)
    public void handleDisconnectEvent(DisconnectEvent event) {
        CACHE.reset(true);
        this.inQueue = false;
        this.queuePosition = 0;
        if (CONFIG.client.extra.autoReconnect.enabled && !event.manualDisconnect) {
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

    public boolean delayBeforeReconnect() {
        try {
            final int countdown;
            if (((PorkClientSession) client).isServerProbablyOff()) {
                countdown = CONFIG.client.extra.autoReconnect.delaySecondsOffline;

                this.reconnectCounter = 0;
            } else {
                countdown = CONFIG.client.extra.autoReconnect.delaySeconds
                        + CONFIG.client.extra.autoReconnect.linearIncrease * this.reconnectCounter++;
            }
            for (int i = countdown; SHOULD_RECONNECT && i > 0; i--) {
                if (i % 10 == 0) CLIENT_LOG.info("Reconnecting in %d", i);
                Wait.waitALittle(1);
            }
            return true;
        } catch (Exception e) {
            return false;
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
            double queueWaitSeconds = Queue.getQueueWait(queueLength, queueLength);
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
                        disconnect();
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
    }

    @Subscribe
    public void handleQueuePositionUpdateEvent(QueuePositionUpdateEvent event) {
        // bounds here are mainly to catch when queue size changes very frequently
        if (event.position >= Queue.getQueueStatus().prio - 50
                && event.position <= Queue.getQueueStatus().prio + 50
                && !this.isPrio.isPresent()) {
            this.isPrio = Optional.of(true);
            CONFIG.authentication.prio = true;
        } else {
            if (!this.isPrio.isPresent()) {
                this.isPrio = Optional.of(false);
                CONFIG.authentication.prio = false;
            }
        }
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
            this.isPrio = Optional.of(true);
            CONFIG.authentication.prio = true;
        }
    }

    @Subscribe
    public void handleDeathEvent(DeathEvent event) {
        if (CONFIG.client.extra.autoRespawn.enabled)  {
            ForkJoinPool.commonPool().execute(() -> {
                PorkUtil.sleep(CONFIG.client.extra.autoRespawn.delayMillis);
                if (Proxy.getInstance().isConnected() && CACHE.getPlayerCache().getThePlayer().getHealth() <= 0)    {
                    Proxy.getInstance().getClient().send(new ClientRequestPacket(ClientRequest.RESPAWN));
                }
            });
        }
    }
}
