package com.zenith.network.client.handler.incoming;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundPlayerChatPacket;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.AsyncIncomingHandler;

import static com.zenith.Shared.CLIENT_LOG;

public class PlayerChatHandler implements AsyncIncomingHandler<ClientboundPlayerChatPacket, ClientSession> {

        @Override
        public boolean applyAsync(ClientboundPlayerChatPacket packet, ClientSession session) {
            CLIENT_LOG.info("Player Chat: {}", packet.getContent());
            // todo: handle this like system chats
            // todo: does the content use text components?
            // todo: do we need to care about the chat reporting shit?
            // we shouldn't receive any of these packets on 2b or any anarchy server due to no chat reports plugins
            return true;
        }

        @Override
        public Class<ClientboundPlayerChatPacket> getPacketClass() {
            return ClientboundPlayerChatPacket.class;
        }
}
