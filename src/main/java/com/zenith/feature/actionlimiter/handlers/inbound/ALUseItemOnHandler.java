package com.zenith.feature.actionlimiter.handlers.inbound;

import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundUseItemOnPacket;
import com.zenith.feature.pathing.BlockPos;
import com.zenith.feature.pathing.World;
import com.zenith.network.registry.IncomingHandler;
import com.zenith.network.server.ServerConnection;

import static com.zenith.Shared.CONFIG;

public class ALUseItemOnHandler implements IncomingHandler<ServerboundUseItemOnPacket, ServerConnection> {
    @Override
    public boolean apply(final ServerboundUseItemOnPacket packet, final ServerConnection session) {
        if (!CONFIG.client.extra.actionLimiter.allowUseItem) return false;
        if (!CONFIG.client.extra.actionLimiter.allowEnderChest) {
            var blockAtBlockPos = World.getBlockAtBlockPos(new BlockPos(packet.getPosition().getX(),
                                                                          packet.getPosition().getY(),
                                                                          packet.getPosition().getZ()));
            if (blockAtBlockPos.name().equals("ender_chest")) {
                return false;
            }
        }
        return true;
    }
}
