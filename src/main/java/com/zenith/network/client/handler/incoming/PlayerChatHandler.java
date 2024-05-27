package com.zenith.network.client.handler.incoming;

import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.ClientEventLoopPacketHandler;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundPlayerChatPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundSystemChatPacket;

public class PlayerChatHandler implements ClientEventLoopPacketHandler<ClientboundPlayerChatPacket, ClientSession> {

    @Override
    public boolean applyAsync(ClientboundPlayerChatPacket packet, ClientSession session) {
        // we shouldn't receive any of these packets on 2b or any anarchy server due to no chat reports plugins
        // zenith does not support chat signing currently
        // resend as a system chat to hit our normal handlers and pass through to connected players
        session.callPacketReceived(new ClientboundSystemChatPacket(packet.getUnsignedContent(), false));
        return true;
    }
}
