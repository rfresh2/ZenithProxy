package com.zenith.network.client.handler.incoming;

import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.PacketHandler;
import com.zenith.util.ComponentSerializer;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundDisguisedChatPacket;

import static com.zenith.Shared.CHAT_LOG;

public class DisguisedChatHandler implements PacketHandler<ClientboundDisguisedChatPacket, ClientSession> {
    @Override
    public ClientboundDisguisedChatPacket apply(final ClientboundDisguisedChatPacket packet, final ClientSession session) {
        // we shouldn't receive any of these packets on 2b or any anarchy server due to no chat reports plugins
        // and this does not pass these packets through to our normal chat handlers
        // so no chat relay, chat events, and other stuff
        if (packet.getChatType().id() == 2) { // incoming whisper
            CHAT_LOG.info("Received Disguised whisper: {}", ComponentSerializer.serializePlain(packet.getMessage()));
        } else if (packet.getChatType().id() == 3) { // outgoing whisper
            CHAT_LOG.info("Sent Disguised whisper: {}", ComponentSerializer.serializePlain(packet.getMessage()));
        }
        return packet;
    }
}
