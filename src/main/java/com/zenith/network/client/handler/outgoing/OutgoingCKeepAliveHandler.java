package com.zenith.network.client.handler.outgoing;

import com.zenith.network.client.ClientSession;
import com.zenith.network.codec.PacketHandler;
import org.geysermc.mcprotocollib.protocol.packet.common.serverbound.ServerboundKeepAlivePacket;

import java.util.NoSuchElementException;

import static com.zenith.Globals.CACHE;
import static com.zenith.Globals.CLIENT_LOG;

public class OutgoingCKeepAliveHandler implements PacketHandler<ServerboundKeepAlivePacket, ClientSession> {
    public static final OutgoingCKeepAliveHandler INSTANCE = new OutgoingCKeepAliveHandler();
    @Override
    public ServerboundKeepAlivePacket apply(final ServerboundKeepAlivePacket packet, final ClientSession session) {
        try {
            var keepAliveQueue = CACHE.getPlayerCache().getKeepAliveQueue();
            var expectedKeepAlive = keepAliveQueue.peek();
            if (expectedKeepAlive != null) {
                var expectedKeepAliveId = expectedKeepAlive.id();
                if (packet.getPingId() == expectedKeepAliveId) {
                    CACHE.getPlayerCache().getKeepAliveQueue().poll();
                    return packet;
                } else {
                    CLIENT_LOG.debug("Out-of-order KeepAlive ID: expected: {}, actual: {}", expectedKeepAlive, packet.getPingId());
                }
            } else {
                CLIENT_LOG.debug("Out-of-order KeepAlive ID: actual: {}", packet.getPingId());
            }
        } catch (NoSuchElementException e) {
            CLIENT_LOG.debug("Unqueued KeepAlive ID: {}", packet.getPingId());
        }
        return null;
    }
}
