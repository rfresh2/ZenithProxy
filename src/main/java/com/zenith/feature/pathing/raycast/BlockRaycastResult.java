package com.zenith.feature.pathing.raycast;

import com.zenith.feature.pathing.blockdata.Block;
import org.geysermc.mcprotocollib.protocol.data.game.entity.object.Direction;

public record BlockRaycastResult(boolean hit, double x, double y, double z, Direction direction, Block block) {
    public static BlockRaycastResult miss() {
        return new BlockRaycastResult(false, 0, 0, 0, Direction.UP, Block.AIR);
    }
}
