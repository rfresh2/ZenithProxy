package com.zenith.network.client.handler.outgoing;

import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.PacketHandler;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundChatCommandSignedPacket;

import static com.zenith.Shared.CACHE;

public class OutgoingSignedChatCommandHandler implements PacketHandler<ServerboundChatCommandSignedPacket, ClientSession> {
    @Override
    public ServerboundChatCommandSignedPacket apply(final ServerboundChatCommandSignedPacket packet, final ClientSession session) {
        var cacheTimestamp= CACHE.getChatCache().getLastChatTimestamp();
        long timeStamp = packet.getTimeStamp();
        if (timeStamp < cacheTimestamp) packet.setTimestamp(cacheTimestamp);
        CACHE.getChatCache().setLastChatTimestamp(packet.getTimeStamp());
        return packet;
    }
}
