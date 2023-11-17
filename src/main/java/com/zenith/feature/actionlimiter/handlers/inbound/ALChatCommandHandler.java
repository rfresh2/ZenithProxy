package com.zenith.feature.actionlimiter.handlers.inbound;

import com.github.steveice10.mc.protocol.packet.ingame.serverbound.ServerboundChatCommandPacket;
import com.zenith.network.registry.IncomingHandler;
import com.zenith.network.server.ServerConnection;

import static com.zenith.Shared.CONFIG;

public class ALChatCommandHandler implements IncomingHandler<ServerboundChatCommandPacket, ServerConnection> {
    @Override
    public boolean apply(final ServerboundChatCommandPacket packet, final ServerConnection session) {
        return CONFIG.client.extra.actionLimiter.allowChat;
    }
}
