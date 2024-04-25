package com.zenith.feature.world.raycast;

import org.geysermc.mcprotocollib.protocol.data.game.entity.object.Direction;
import com.zenith.feature.world.blockdata.Block;

public record BlockRaycastResult(boolean hit, double x, double y, double z, Direction direction, Block block) {
    public static BlockRaycastResult miss() {
        return new BlockRaycastResult(false, 0, 0, 0, Direction.UP, Block.AIR);
    }
}
