package com.zenith.network.client;

import com.zenith.Proxy;
import com.zenith.event.client.ClientBotTick;
import com.zenith.event.client.ClientDisconnectEvent;
import com.zenith.event.client.ClientOnlineEvent;
import com.zenith.event.client.ClientTickEvent;
import com.zenith.event.player.PlayerConnectedEvent;
import com.zenith.event.player.PlayerDisconnectedEvent;
import com.zenith.util.Wait;
import io.netty.channel.EventLoop;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.github.rfresh2.EventConsumer.of;
import static com.zenith.Globals.*;
import static java.util.Objects.nonNull;

public class ClientTickManager {
    private final AtomicBoolean doClientTicks = new AtomicBoolean(false);
    private final AtomicBoolean doBotTicks = new AtomicBoolean(false);
    private final Thread tickManagerThread;
    private final DeltaTracker deltaTracker = new DeltaTracker(System.nanoTime());

    public ClientTickManager() {
        EVENT_BUS.subscribe(
            this,
            of(ClientOnlineEvent.class, this::handlePlayerOnlineEvent),
            of(PlayerConnectedEvent.class, this::handleProxyClientConnectedEvent),
            of(PlayerDisconnectedEvent.class, this::handleProxyClientDisconnectedEvent),
            of(ClientDisconnectEvent.class, this::handleDisconnectEvent)
        );
        tickManagerThread = Thread.ofPlatform()
            .name("ZenithProxy Client Tick Manager")
            .daemon(true)
            .uncaughtExceptionHandler((t, e) -> CLIENT_LOG.error("ClientTickManager Error", e))
            .start(this::tickManagerLoop);
    }

    private void tickManagerLoop() {
        while (true) {
            try {
                var tickRate = CONFIG.client.tickRate;
                var client = Proxy.getInstance().getClient();
                if (tickRate <= 0 || client == null || !doClientTicks.get() || client.getClientEventLoop().isShuttingDown()) {
                    deltaTracker.reset(System.nanoTime());
                    Wait.waitMs(50);
                    continue;
                }
                var eventLoop = client.getClientEventLoop();
                var ticksToDo = deltaTracker.advanceTime(System.nanoTime(), tickRate);
                if (ticksToDo > 0) {
                    eventLoop.submit(() -> {
                        for (var i = 0; i < ticksToDo && doClientTicks.get() && !eventLoop.isShuttingDown(); i++) {
                            tick();
                        }
                    }).await();
                }
                var waitNs = deltaTracker.nanosUntilNextTick(System.nanoTime(), tickRate);
                if (waitNs > 0) {
                    Wait.waitMs((int) TimeUnit.NANOSECONDS.toMillis(waitNs));
                }
            } catch (RejectedExecutionException e) {
                CLIENT_LOG.debug("ClientTickManager eventloop rejected execution", e);
            } catch (Exception e) {
                CLIENT_LOG.error("ClientTickManager loop error", e);
            }
        }
    }

    static final class DeltaTracker {
        private final double nsPerTick = TimeUnit.MILLISECONDS.toNanos(50);
        private long lastTimeNs;
        private double deltaTickResidual;

        DeltaTracker(long currentTimeNs) {
            reset(currentTimeNs);
        }

        void reset(long currentTimeNs) {
            lastTimeNs = currentTimeNs;
            deltaTickResidual = 0;
        }

        int advanceTime(long currentTimeNs, double tickRate) {
            deltaTickResidual += (currentTimeNs - lastTimeNs) / (nsPerTick / tickRate);
            lastTimeNs = currentTimeNs;
            var ticks = Math.floor(deltaTickResidual);
            deltaTickResidual -= ticks;
            return (int) Math.min(10, ticks);
        }

        long nanosUntilNextTick(long currentTimeNs, double tickRate) {
            return Math.max(0L, (long) Math.ceil((1 - deltaTickResidual) * (nsPerTick / tickRate) - (currentTimeNs - lastTimeNs)));
        }
    }

    public void handlePlayerOnlineEvent(final ClientOnlineEvent event) {
        executeInEventLoop(() -> {
            if (!Proxy.getInstance().hasActivePlayer()) {
                startBotTicks();
            }
        });
    }

    public void handleDisconnectEvent(final ClientDisconnectEvent event) {
        executeInEventLoop(this::stopBotTicks);
    }

    public void handleProxyClientConnectedEvent(final PlayerConnectedEvent event) {
        executeInEventLoop(this::stopBotTicks);
    }

    public void handleProxyClientDisconnectedEvent(final PlayerDisconnectedEvent event) {
        executeInEventLoop(() -> {
            if (nonNull(Proxy.getInstance().getClient()) && Proxy.getInstance().getClient().isOnline()) {
                startBotTicks();
            }
        });
    }

    public synchronized void startClientTicks() {
        if (doClientTicks.compareAndSet(false, true)) {
            executeInEventLoop(() -> {
                CLIENT_LOG.debug("Starting Client Ticks");
                EVENT_BUS.post(ClientTickEvent.Starting.INSTANCE);
            });
        }
    }

    private static final long LONG_TICK_THRESHOLD_MS = 100L;

    private void tick() {
        try {
            long before = System.currentTimeMillis();
            EVENT_BUS.post(ClientTickEvent.INSTANCE);
            if (doBotTicks.get()) {
                EVENT_BUS.post(ClientBotTick.INSTANCE);
            }
            EVENT_BUS.post(ClientTickEvent.End.INSTANCE);
            long after = System.currentTimeMillis();
            long elapsedMs = after - before;
            if (elapsedMs > LONG_TICK_THRESHOLD_MS) {
                CLIENT_LOG.debug("Slow Client Tick: {}ms", elapsedMs);
            }
        } catch (final Throwable e) {
            CLIENT_LOG.error("Error during client tick", e);
        }
    };

    public synchronized void stopClientTicks() {
        stopBotTicks();
        if (doClientTicks.compareAndSet(true, false)) {
            executeInEventLoop(() -> {
                CLIENT_LOG.debug("Stopped Client Ticks");
                EVENT_BUS.post(ClientTickEvent.Stopped.INSTANCE);
            });
        }
    }

    public void startBotTicks() {
        if (doBotTicks.compareAndSet(false, true)) {
            executeInEventLoop(() -> {
                CLIENT_LOG.debug("Starting Bot Ticks");
                EVENT_BUS.post(ClientBotTick.Starting.INSTANCE);
            });
        }
    }

    public void stopBotTicks() {
        if (doBotTicks.compareAndSet(true, false)) {
            executeInEventLoop(() -> {
                CLIENT_LOG.debug("Stopped Bot Ticks");
                EVENT_BUS.post(ClientBotTick.Stopped.INSTANCE);
            });
        }
    }

    private EventLoop getEventLoop() {
        return Proxy.getInstance().getClient().getClientEventLoop();
    }

    private void executeInEventLoop(Runnable runnable) {
        var eventLoop = getEventLoop();
        if (eventLoop.inEventLoop() || eventLoop.isShuttingDown()) {
            runnable.run();
        } else {
            eventLoop.execute(runnable);
        }
    }
}
