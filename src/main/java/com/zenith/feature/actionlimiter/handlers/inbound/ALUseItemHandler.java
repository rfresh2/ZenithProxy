package com.zenith.feature.actionlimiter.handlers.inbound;

import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundUseItemPacket;
import com.zenith.network.registry.IncomingHandler;
import com.zenith.network.server.ServerConnection;

import static com.zenith.Shared.CONFIG;

public class ALUseItemHandler implements IncomingHandler<ServerboundUseItemPacket, ServerConnection> {
    @Override
    public boolean apply(final ServerboundUseItemPacket packet, final ServerConnection session) {
        if (!CONFIG.client.extra.actionLimiter.allowUseItem) return false;
        return true;
    }
}
