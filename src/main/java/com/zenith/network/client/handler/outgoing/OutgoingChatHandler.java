package com.zenith.network.client.handler.outgoing;

import com.zenith.event.module.OutboundChatEvent;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.PacketHandler;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundChatCommandPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundChatPacket;

import static com.zenith.Shared.CACHE;
import static com.zenith.Shared.EVENT_BUS;

public class OutgoingChatHandler implements PacketHandler<ServerboundChatPacket, ClientSession> {
    @Override
    public ServerboundChatPacket apply(final ServerboundChatPacket packet, final ClientSession session) {
        // allow us to dispatch commands just with chat packets
        if (!packet.getMessage().isEmpty() && packet.getMessage().charAt(0) == '/') {
            String message = packet.getMessage();
            session.send(new ServerboundChatCommandPacket(message.substring(1, (Math.min(message.length(), 257)))));
            return null;
        }
        final OutboundChatEvent outboundChatEvent = new OutboundChatEvent(packet);
        EVENT_BUS.post(outboundChatEvent);
        if (outboundChatEvent.isCancelled()) return null;
        final var cacheTimestamp = CACHE.getChatCache().getLastChatTimestamp();
        if (packet.getTimeStamp() < cacheTimestamp) packet.setTimeStamp(cacheTimestamp);
        CACHE.getChatCache().setLastChatTimestamp(packet.getTimeStamp());
        return packet;
    }
}
