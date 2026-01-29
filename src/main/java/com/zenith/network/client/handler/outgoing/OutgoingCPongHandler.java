package com.zenith.network.client.handler.outgoing;

import com.zenith.network.client.ClientSession;
import com.zenith.network.codec.PacketHandler;
import org.geysermc.mcprotocollib.protocol.packet.common.serverbound.ServerboundPongPacket;

import java.util.NoSuchElementException;

import static com.zenith.Globals.CACHE;
import static com.zenith.Globals.CLIENT_LOG;

public class OutgoingCPongHandler implements PacketHandler<ServerboundPongPacket, ClientSession> {
    public static final OutgoingCPongHandler INSTANCE = new OutgoingCPongHandler();
    @Override
    public ServerboundPongPacket apply(final ServerboundPongPacket packet, final ClientSession session) {
        try {
            var pingQueue = CACHE.getPlayerCache().getPingQueue();
            var expectedPing = pingQueue.peek();
            if (expectedPing != null) {
                var expectedPingId = expectedPing.id();
                if (packet.getId() == expectedPingId) {
                    CACHE.getPlayerCache().getPingQueue().poll();
                    return packet;
                } else {
                    CLIENT_LOG.debug("Out-of-order Pong ID: expected: {}, actual: {}", expectedPingId, packet.getId());
                }
            } else {
                CLIENT_LOG.debug("Out-of-order Pong ID: actual: {}", packet.getId());
            }
        } catch (NoSuchElementException e) {
            CLIENT_LOG.debug("Unqueued Pong ID: {}", packet.getId());
        }
        return null;
    }
}
