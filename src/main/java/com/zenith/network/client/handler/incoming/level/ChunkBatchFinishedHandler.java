package com.zenith.network.client.handler.incoming.level;

import com.zenith.Proxy;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.PacketHandler;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundChunkBatchFinishedPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.level.ServerboundChunkBatchReceivedPacket;

public class ChunkBatchFinishedHandler implements PacketHandler<ClientboundChunkBatchFinishedPacket, ClientSession> {
    @Override
    public ClientboundChunkBatchFinishedPacket apply(final ClientboundChunkBatchFinishedPacket packet, final ClientSession session) {
        if (!Proxy.getInstance().hasActivePlayer()) {
            session.sendAsync(new ServerboundChunkBatchReceivedPacket(64)); // max allowed by server
        }
        return packet;
    }
}
