package com.zenith.feature.world.raycast;

import com.zenith.feature.world.blockdata.Block;
import com.zenith.feature.world.blockdata.BlockRegistry;
import org.geysermc.mcprotocollib.protocol.data.game.entity.object.Direction;

public record BlockRaycastResult(boolean hit, double x, double y, double z, Direction direction, Block block) {
    public static BlockRaycastResult miss() {
        return new BlockRaycastResult(false, 0, 0, 0, Direction.UP, BlockRegistry.AIR);
    }
}
