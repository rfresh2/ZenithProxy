package com.zenith.network.client.handler.outgoing;

import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.PacketHandler;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundChatCommandPacket;

import static com.zenith.Shared.CACHE;

public class OutgoingChatCommandHandler implements PacketHandler<ServerboundChatCommandPacket, ClientSession> {
    @Override
    public ServerboundChatCommandPacket apply(final ServerboundChatCommandPacket packet, final ClientSession session) {
        final var cacheTimestamp = CACHE.getChatCache().getLastChatTimestamp();
        if (packet.getTimeStamp() < cacheTimestamp) packet.setTimeStamp(cacheTimestamp);
        CACHE.getChatCache().setLastChatTimestamp(packet.getTimeStamp());
        return packet;
    }
}
