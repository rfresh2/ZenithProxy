package com.zenith.feature.actionlimiter.handlers.inbound;

import com.github.steveice10.mc.protocol.packet.ingame.serverbound.ServerboundChatCommandPacket;
import com.zenith.module.impl.ActionLimiter;
import com.zenith.network.registry.PacketHandler;
import com.zenith.network.server.ServerConnection;

import static com.zenith.Shared.CONFIG;
import static com.zenith.Shared.MODULE_MANAGER;

public class ALChatCommandHandler implements PacketHandler<ServerboundChatCommandPacket, ServerConnection> {
    @Override
    public ServerboundChatCommandPacket apply(final ServerboundChatCommandPacket packet, final ServerConnection session) {
        if (MODULE_MANAGER.get(ActionLimiter.class).bypassesLimits(session)) return packet;
        if (CONFIG.client.extra.actionLimiter.allowChat) return packet;
        else return null;
    }
}
