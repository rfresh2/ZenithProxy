package com.zenith.mc.block;

import com.zenith.util.math.MathHelper;
import com.zenith.util.math.MutableVec3d;
import lombok.Getter;

@Getter
public enum BlockOffsetType {
    NONE((blockState, x, y, z) -> MutableVec3d.ZERO),
    XZ((blockState, x, y, z) -> {
        long seed = MathHelper.getSeed(x, y, z);
        float maxHorizontalOffset = blockState.block().maxHorizontalOffset();
        double xOffset = MathHelper.clamp(((double)((float)(seed & 15L) / 15.0F) - 0.5) * 0.5, -maxHorizontalOffset, maxHorizontalOffset);
        double zOffset = MathHelper.clamp(((double)((float)(seed >> 8 & 15L) / 15.0F) - 0.5) * 0.5, -maxHorizontalOffset, maxHorizontalOffset);
        return new MutableVec3d(xOffset, 0.0, zOffset);
    }),
    XYZ((blockState, x, y, z) -> {
        long seed = MathHelper.getSeed(x, 0, z);
        double yOffset = ((double)((float)(seed >> 4 & 15L) / 15.0F) - 1.0) * (double)blockState.block().maxVerticalOffset();
        float maxHorizontalOffset = blockState.block().maxHorizontalOffset();
        double xOffset = MathHelper.clamp(((double)((float)(seed & 15L) / 15.0F) - 0.5) * 0.5, -maxHorizontalOffset, maxHorizontalOffset);
        double zOffset = MathHelper.clamp(((double)((float)(seed >> 8 & 15L) / 15.0F) - 0.5) * 0.5, -maxHorizontalOffset, maxHorizontalOffset);
        return new MutableVec3d(xOffset, yOffset, zOffset);
    });
    private final OffsetFunction offsetFunction;

    BlockOffsetType(OffsetFunction offsetFunction) {
        this.offsetFunction = offsetFunction;
    }

    @FunctionalInterface
    public interface OffsetFunction {
        MutableVec3d offset(BlockState blockState, int x, int y, int z);
    }
}
