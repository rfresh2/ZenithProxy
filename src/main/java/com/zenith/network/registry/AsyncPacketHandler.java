package com.zenith.network.registry;

import io.netty.channel.DefaultEventLoop;
import io.netty.channel.EventLoop;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.geysermc.mcprotocollib.network.Session;
import org.geysermc.mcprotocollib.network.packet.Packet;

import static com.zenith.Shared.SERVER_LOG;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

@FunctionalInterface
public interface AsyncPacketHandler<P extends Packet, S extends Session> extends PacketHandler<P, S> {
    EventLoop EVENT_LOOP = new DefaultEventLoop(new DefaultThreadFactory("ZenithProxy Async Packet Handler", true));

    boolean applyAsync(P packet, S session);

    default P apply(P packet, S session) {
        if (packet == null) return null;
        EVENT_LOOP.execute(() -> applyWithRetries(packet, session, 0));
        return packet;
    }

    private void applyWithRetries(P packet, S session, final int tryCount) {
        try {
            if (!applyAsync(packet, session)) {
                if (tryCount > 1) {
                    SERVER_LOG.debug("Unable to apply async handler for packet: {}", packet.getClass().getSimpleName());
                    return;
                }
                EVENT_LOOP.schedule(() -> applyWithRetries(packet, session, tryCount + 1), 250, MILLISECONDS);
            }
        } catch (final Throwable e) {
            SERVER_LOG.error("Async handler error", e);
        }
    }
}
