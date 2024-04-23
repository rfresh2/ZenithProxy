package com.zenith.feature.actionlimiter.handlers.inbound;

import com.github.steveice10.mc.protocol.packet.ingame.serverbound.ServerboundChatCommandSignedPacket;
import com.zenith.network.registry.PacketHandler;
import com.zenith.network.server.ServerConnection;

import static com.zenith.Shared.CONFIG;

public class ALSignedChatCommandHandler implements PacketHandler<ServerboundChatCommandSignedPacket, ServerConnection> {
    @Override
    public ServerboundChatCommandSignedPacket apply(final ServerboundChatCommandSignedPacket packet, final ServerConnection session) {
        if (CONFIG.client.extra.actionLimiter.allowChat) return packet;
        else return null;
    }
}
