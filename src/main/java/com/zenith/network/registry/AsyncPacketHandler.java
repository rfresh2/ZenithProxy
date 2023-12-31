package com.zenith.network.registry;

import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.packet.Packet;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static com.zenith.Shared.CLIENT_LOG;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

@FunctionalInterface
public interface AsyncPacketHandler<P extends Packet, S extends Session> extends PacketHandler<P, S> {
    static final ExecutorService ASYNC_EXECUTOR_SERVICE =
        Executors.newFixedThreadPool(1,
                                     new ThreadFactoryBuilder()
                                         .setNameFormat("ZenithProxy Async PacketHandler #%d")
                                         .setDaemon(true)
                                         .build());
    static final ScheduledExecutorService RETRY_EXECUTOR_SERVICE =
        Executors.newScheduledThreadPool(1,
                                         new ThreadFactoryBuilder()
                                             .setNameFormat("ZenithProxy Async PacketHandler Retry #%d")
                                             .setDaemon(true)
                                             .build());

    boolean applyAsync(P packet, S session);

    default P apply(P packet, S session) {
        if (packet == null) return null;
        ASYNC_EXECUTOR_SERVICE.execute(() -> {
            applyWithRetries(packet, session, 0);
        });
        return packet;
    }

    private void applyWithRetries(P packet, S session, final int tryCount) {
        try {
            if (!applyAsync(packet, session)) {
                if (tryCount < 0 || tryCount > 1) {
                    CLIENT_LOG.debug("Unable to apply async handler for packet: " + packet.getClass().getSimpleName());
                    return;
                }
                RETRY_EXECUTOR_SERVICE.schedule(() -> {
                    applyWithRetries(packet, session, tryCount + 1);
                }, 250, MILLISECONDS);
            }
        } catch (final Throwable e) {
            CLIENT_LOG.error("Async handler error", e);
        }
    }
}
