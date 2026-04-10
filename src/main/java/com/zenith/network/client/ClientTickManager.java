package com.zenith.network.client;

import com.zenith.Proxy;
import com.zenith.event.client.ClientBotTick;
import com.zenith.event.client.ClientDisconnectEvent;
import com.zenith.event.client.ClientOnlineEvent;
import com.zenith.event.client.ClientTickEvent;
import com.zenith.event.player.PlayerConnectedEvent;
import com.zenith.event.player.PlayerDisconnectedEvent;
import com.zenith.util.Wait;
import com.zenith.util.math.MathHelper;
import io.netty.channel.EventLoop;
import io.netty.util.concurrent.Future;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.github.rfresh2.EventConsumer.of;
import static com.zenith.Globals.*;
import static java.util.Objects.nonNull;

public class ClientTickManager {
    private final AtomicBoolean doClientTicks = new AtomicBoolean(false);
    private final AtomicBoolean doBotTicks = new AtomicBoolean(false);
    private final Thread tickManagerThread;

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
                if (!doClientTicks.get()) {
                    Wait.waitMs((int) (50.0 / CONFIG.client.tickRate));
                    continue;
                }
                var before = System.nanoTime();
                submitInEventLoop(this::tick).await();
                // now calculate how long to wait until next tick
                // we will drift slightly lower in tps as thread sleep duration is variable in scheduling
                // todo: vanilla client limits to 10 catch-up ticks, but we are not tracking this across multiple ticks
                var elapsed = System.nanoTime() - before;
                var nextTickNsDelay = (long) (50_000_000.0 / CONFIG.client.tickRate);
                var waitDuration = Duration.ofNanos(MathHelper.clamp(nextTickNsDelay - elapsed, 0L, nextTickNsDelay));
                Wait.wait(waitDuration);
            } catch (Exception e) {
                CLIENT_LOG.error("ClientTickManager loop error", e);
            }
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

    private Future<?> submitInEventLoop(Runnable task) {
        return getEventLoop().submit(task);
    }
}
