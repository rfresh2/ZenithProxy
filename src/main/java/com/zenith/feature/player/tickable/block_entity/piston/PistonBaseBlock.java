package com.zenith.feature.player.tickable.block_entity.piston;

import com.zenith.feature.player.World;
import com.zenith.mc.block.*;
import com.zenith.mc.block.properties.PistonType;
import com.zenith.mc.block.properties.api.BlockStateProperties;
import org.geysermc.mcprotocollib.protocol.data.game.level.block.value.PistonValue;
import org.geysermc.mcprotocollib.protocol.data.game.level.block.value.PistonValueType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.zenith.Globals.CACHE;

public final class PistonBaseBlock {
    private PistonBaseBlock() {}

    public static boolean isPiston(Block block) {
        return block == BlockRegistry.PISTON || block == BlockRegistry.STICKY_PISTON;
    }

    public static boolean triggerEvent(BlockState state, PistonValueType pistonValueType, PistonValue pistonValue) {
        Direction direction = state.getProperty(BlockStateProperties.FACING);
        if (direction == null) return false;
        boolean isSticky = state.is(BlockRegistry.STICKY_PISTON);
        PistonType pistonType = isSticky ? PistonType.STICKY : PistonType.DEFAULT;
        BlockPos pos = state.pos();
        if (pistonValueType == PistonValueType.PUSHING) {
            if (!moveBlocks(pos, direction, true, pistonType)) {
                return false;
            }
            World.setBlockState(state.withPropertyValue(BlockStateProperties.EXTENDED, true));
        } else if (pistonValueType == PistonValueType.PULLING || pistonValueType == PistonValueType.CANCELLED_MID_PUSH) {
            if (World.getTickableBlockEntity(pos.relative(direction)) instanceof PistonMovingBlockEntity pistonMovingBlockEntity) {
                pistonMovingBlockEntity.finalTick();
            }

            BlockState movingState = movingPistonState(pos, direction, pistonType);
            World.setBlockState(movingState);
            BlockState movedState = stateWith(state.block(), pos)
                .withPropertyValue(BlockStateProperties.FACING, Direction.fromMcpl(pistonValue.getDirection()))
                .withPropertyValue(BlockStateProperties.EXTENDED, false);
            World.setTickableBlockEntity(new PistonMovingBlockEntity(pos, movedState, direction, false, true));
            if (isSticky) {
                BlockPos blockpos = pos.relative(direction, 2);
                BlockState blockstate2 = World.getBlockState(blockpos);
                boolean flag1 = false;
                if (blockstate2.is(BlockRegistry.MOVING_PISTON)
                    && World.getTickableBlockEntity(blockpos) instanceof PistonMovingBlockEntity pistonMovingBlockEntity
                    && pistonMovingBlockEntity.getDirection() == direction
                    && pistonMovingBlockEntity.isExtending()) {
                    pistonMovingBlockEntity.finalTick();
                    flag1 = true;
                }

                if (!flag1) {
                    if (pistonValueType != PistonValueType.PULLING
                        || blockstate2.isAir()
                        || !isPushable(blockstate2, direction.invert(), false, direction)
                        || blockstate2.block().pushReaction() != PushReaction.NORMAL && !isPiston(blockstate2.block())) {
                        removeBlock(pos.relative(direction));
                    } else {
                        moveBlocks(pos, direction, false, pistonType);
                    }
                }
            } else {
                removeBlock(pos.relative(direction));
            }
        }
        return true;
    }

    public static boolean isPushable(BlockState state, Direction movementDirection, boolean allowDestroy, Direction pistonFacing) {
        var dimension = World.getCurrentDimension();
        int minY = dimension.minY();
        int maxY = dimension.buildHeight() - 1;
        if (state.y() < minY || state.y() > maxY || !isWithinWorldBorder(state.x(), state.z())) {
            return false;
        } else if (state.isAir()) {
            return true;
        } else if (state.is(BlockRegistry.OBSIDIAN)
            || state.is(BlockRegistry.CRYING_OBSIDIAN)
            || state.is(BlockRegistry.RESPAWN_ANCHOR)
            || state.is(BlockRegistry.REINFORCED_DEEPSLATE)) {
            return false;
        } else if (movementDirection == Direction.DOWN && state.y() == minY) {
            return false;
        } else if (movementDirection == Direction.UP && state.y() == maxY) {
            return false;
        } else {
            if (!isPiston(state.block())) {
                if (state.block().destroySpeed() == -1.0F) {
                    return false;
                }

                switch (state.block().pushReaction()) {
                    case BLOCK -> {
                        return false;
                    }
                    case DESTROY -> {
                        return allowDestroy;
                    }
                    case PUSH_ONLY -> {
                        return movementDirection == pistonFacing;
                    }
                }
            } else if (Boolean.TRUE.equals(state.getProperty(BlockStateProperties.EXTENDED))) {
                return false;
            }
            return state.block().blockEntityType() == null;
        }
    }

    private static boolean isWithinWorldBorder(int x, int z) {
        var border = CACHE.getChunkCache().getWorldBorderData();
        double halfSize = border.getSize() / 2.0;
        return x >= border.getCenterX() - halfSize
            && x < border.getCenterX() + halfSize
            && z >= border.getCenterZ() - halfSize
            && z < border.getCenterZ() + halfSize;
    }

    private static boolean moveBlocks(BlockPos pos, Direction facing, boolean extending, PistonType pistonType) {
        BlockPos blockpos = pos.relative(facing);
        if (!extending && World.getBlock(blockpos) == BlockRegistry.PISTON_HEAD) {
            World.setBlockState(blockpos.x(), blockpos.y(), blockpos.z(), BlockRegistry.AIR.minStateId());
        }

        PistonStructureResolver pistonstructureresolver = new PistonStructureResolver(pos, facing, extending);
        if (!pistonstructureresolver.resolve()) {
            return false;
        } else {
            Map<BlockPos, BlockState> map = new LinkedHashMap<>();
            List<BlockPos> list = pistonstructureresolver.getToPush();
            List<BlockState> list1 = new ArrayList<>(list.size());

            for (BlockPos blockpos1 : list) {
                BlockState blockstate = World.getBlockState(blockpos1);
                list1.add(blockstate);
                map.put(blockpos1, blockstate);
            }

            List<BlockPos> list2 = pistonstructureresolver.getToDestroy();
            Direction direction = extending ? facing : facing.invert();

            for (int j = list2.size() - 1; j >= 0; j--) {
                BlockPos blockpos2 = list2.get(j);
                World.setBlockState(blockpos2.x(), blockpos2.y(), blockpos2.z(), BlockRegistry.AIR.minStateId());
            }

            for (int k = list.size() - 1; k >= 0; k--) {
                BlockPos blockpos3 = list.get(k).relative(direction);
                map.remove(blockpos3);
                BlockState blockstate7 = movingPistonState(blockpos3, facing, PistonType.DEFAULT);
                World.setBlockState(blockstate7);
                World.setTickableBlockEntity(new PistonMovingBlockEntity(blockpos3, list1.get(k), facing, extending, false));
            }

            if (extending) {
                BlockState blockstate4 = pistonHeadState(blockpos, facing, pistonType, false);
                BlockState blockstate6 = movingPistonState(blockpos, facing, pistonType);
                map.remove(blockpos);
                World.setBlockState(blockstate6);
                World.setTickableBlockEntity(new PistonMovingBlockEntity(blockpos, blockstate4, facing, true, true));
            }

            for (BlockPos blockpos4 : map.keySet()) {
                World.setBlockState(blockpos4.x(), blockpos4.y(), blockpos4.z(), BlockRegistry.AIR.minStateId());
            }

            return true;
        }
    }

    private static void removeBlock(BlockPos pos) {
        World.setBlockState(pos.x(), pos.y(), pos.z(), BlockRegistry.AIR.minStateId());
    }

    static BlockState stateWith(Block block, BlockPos pos) {
        return new BlockState(block, block.minStateId(), pos.x(), pos.y(), pos.z());
    }

    static BlockState movingPistonState(BlockPos pos, Direction facing, PistonType pistonType) {
        return stateWith(BlockRegistry.MOVING_PISTON, pos)
            .withPropertyValue(BlockStateProperties.FACING, facing)
            .withPropertyValue(BlockStateProperties.PISTON_TYPE, pistonType);
    }

    static BlockState pistonHeadState(BlockPos pos, Direction facing, PistonType pistonType, boolean isShort) {
        return stateWith(BlockRegistry.PISTON_HEAD, pos)
            .withPropertyValue(BlockStateProperties.FACING, facing)
            .withPropertyValue(BlockStateProperties.PISTON_TYPE, pistonType)
            .withPropertyValue(BlockStateProperties.SHORT, isShort);
    }
}
