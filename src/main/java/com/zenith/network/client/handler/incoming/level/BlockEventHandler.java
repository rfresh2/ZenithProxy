package com.zenith.network.client.handler.incoming.level;

import com.zenith.feature.player.World;
import com.zenith.feature.player.tickable.block_entity.piston.PistonBaseBlock;
import com.zenith.network.client.ClientSession;
import com.zenith.network.codec.ClientEventLoopPacketHandler;
import org.geysermc.mcprotocollib.protocol.data.game.level.block.value.PistonValue;
import org.geysermc.mcprotocollib.protocol.data.game.level.block.value.PistonValueType;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundBlockEventPacket;

public class BlockEventHandler implements ClientEventLoopPacketHandler<ClientboundBlockEventPacket, ClientSession> {
    @Override
    public boolean applyAsync(final ClientboundBlockEventPacket packet, final ClientSession session) {
        if (PistonBaseBlock.isPiston(World.getBlock(packet.getX(), packet.getY(), packet.getZ()))
            && packet.getType() instanceof PistonValueType pistonValueType
            && packet.getValue() instanceof PistonValue pistonValue
        ) {
            PistonBaseBlock.triggerEvent(World.getBlockState(packet.getX(), packet.getY(), packet.getZ()), pistonValueType, pistonValue);
        }
        return true;
    }
}
