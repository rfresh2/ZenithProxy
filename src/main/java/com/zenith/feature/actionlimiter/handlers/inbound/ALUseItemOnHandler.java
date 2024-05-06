package com.zenith.feature.actionlimiter.handlers.inbound;

import com.zenith.feature.world.BlockPos;
import com.zenith.feature.world.World;
import com.zenith.network.registry.PacketHandler;
import com.zenith.network.server.ServerConnection;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundUseItemOnPacket;

import static com.zenith.Shared.CONFIG;

public class ALUseItemOnHandler implements PacketHandler<ServerboundUseItemOnPacket, ServerConnection> {
    @Override
    public ServerboundUseItemOnPacket apply(final ServerboundUseItemOnPacket packet, final ServerConnection session) {
        if (!CONFIG.client.extra.actionLimiter.allowUseItem) return null;
        if (!CONFIG.client.extra.actionLimiter.allowEnderChest) {
            var blockAtBlockPos = World.getBlockAtBlockPos(new BlockPos(packet.getX(), packet.getY(), packet.getZ()));
            if (blockAtBlockPos.name().equals("ender_chest")) {
                return null;
            }
        }
        return packet;
    }
}
