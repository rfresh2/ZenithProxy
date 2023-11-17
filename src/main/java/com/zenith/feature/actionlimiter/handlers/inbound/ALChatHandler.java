package com.zenith.feature.actionlimiter.handlers.inbound;

import com.github.steveice10.mc.protocol.packet.ingame.serverbound.ServerboundChatPacket;
import com.zenith.network.registry.IncomingHandler;
import com.zenith.network.server.ServerConnection;

import static com.zenith.Shared.CONFIG;

public class ALChatHandler implements IncomingHandler<ServerboundChatPacket, ServerConnection> {
    @Override
    public boolean apply(final ServerboundChatPacket packet, final ServerConnection session) {
        return CONFIG.client.extra.actionLimiter.allowChat;
    }
}
