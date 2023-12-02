package com.zenith.network.client.handler.incoming.level;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.level.ClientboundChunkBatchFinishedPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.level.ServerboundChunkBatchReceivedPacket;
import com.zenith.Proxy;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.PacketHandler;

public class ChunkBatchFinishedHandler implements PacketHandler<ClientboundChunkBatchFinishedPacket, ClientSession> {
    @Override
    public ClientboundChunkBatchFinishedPacket apply(final ClientboundChunkBatchFinishedPacket packet, final ClientSession session) {
        if (!Proxy.getInstance().hasActivePlayer()) {
            session.sendAsync(new ServerboundChunkBatchReceivedPacket(64)); // max allowed by server
        }
        return packet;
    }
}
