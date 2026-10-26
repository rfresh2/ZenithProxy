package com.zenith.feature.player.tickable.block_entity.piston;

import com.zenith.feature.player.World;
import com.zenith.mc.block.*;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

public class PistonStructureResolver {
    public static final int MAX_PUSH_DEPTH = 12;
    private static final Direction[] DIRECTIONS = Direction.values();
    private final BlockPos pistonPos;
    private final boolean extending;
    private final BlockPos startPos;
    @Getter private final Direction pushDirection;
    @Getter private final List<BlockPos> toPush = new ArrayList<>();
    @Getter private final List<BlockPos> toDestroy = new ArrayList<>();
    private final Direction pistonDirection;

    public PistonStructureResolver(BlockPos pistonPos, Direction pistonDirection, boolean extending) {
        this.pistonPos = pistonPos;
        this.pistonDirection = pistonDirection;
        this.extending = extending;
        if (extending) {
            this.pushDirection = pistonDirection;
            this.startPos = pistonPos.relative(pistonDirection);
        } else {
            this.pushDirection = pistonDirection.invert();
            this.startPos = pistonPos.relative(pistonDirection, 2);
        }
    }

    public boolean resolve() {
        this.toPush.clear();
        this.toDestroy.clear();
        BlockState blockstate = World.getBlockState(this.startPos);
        if (!PistonBaseBlock.isPushable(blockstate, this.pushDirection, false, this.pistonDirection)) {
            if (this.extending && blockstate.block().pushReaction() == PushReaction.DESTROY) {
                this.toDestroy.add(this.startPos);
                return true;
            } else {
                return false;
            }
        } else if (!this.addBlockLine(this.startPos, this.pushDirection)) {
            return false;
        } else {
            for (int i = 0; i < this.toPush.size(); i++) {
                BlockPos blockpos = this.toPush.get(i);
                if (isSticky(World.getBlockState(blockpos)) && !this.addBranchingBlocks(blockpos)) {
                    return false;
                }
            }
            return true;
        }
    }

    private static boolean isSticky(BlockState state) {
        return state.is(BlockRegistry.SLIME_BLOCK) || state.is(BlockRegistry.HONEY_BLOCK);
    }

    private static boolean canStickToEachOther(BlockState state1, BlockState state2) {
        if (state1.is(BlockRegistry.HONEY_BLOCK) && state2.is(BlockRegistry.SLIME_BLOCK)) {
            return false;
        } else {
            return state1.is(BlockRegistry.SLIME_BLOCK) && state2.is(BlockRegistry.HONEY_BLOCK)
                ? false
                : isSticky(state1) || isSticky(state2);
        }
    }

    private boolean addBlockLine(BlockPos originPos, Direction direction) {
        BlockState blockstate = World.getBlockState(originPos);
        if (blockstate.isAir()) {
            return true;
        } else if (!PistonBaseBlock.isPushable(blockstate, this.pushDirection, false, direction)) {
            return true;
        } else if (originPos.equals(this.pistonPos)) {
            return true;
        } else if (this.toPush.contains(originPos)) {
            return true;
        } else {
            int i = 1;
            if (i + this.toPush.size() > MAX_PUSH_DEPTH) {
                return false;
            } else {
                while (isSticky(blockstate)) {
                    BlockPos blockpos = originPos.relative(this.pushDirection.invert(), i);
                    BlockState blockstate1 = blockstate;
                    blockstate = World.getBlockState(blockpos);
                    if (blockstate.isAir()
                        || !canStickToEachOther(blockstate1, blockstate)
                        || !PistonBaseBlock.isPushable(blockstate, this.pushDirection, false, this.pushDirection.invert())
                        || blockpos.equals(this.pistonPos)) {
                        break;
                    }

                    if (++i + this.toPush.size() > MAX_PUSH_DEPTH) {
                        return false;
                    }
                }

                int l = 0;

                for (int i1 = i - 1; i1 >= 0; i1--) {
                    this.toPush.add(originPos.relative(this.pushDirection.invert(), i1));
                    l++;
                }

                int j1 = 1;

                while (true) {
                    BlockPos blockpos1 = originPos.relative(this.pushDirection, j1);
                    int j = this.toPush.indexOf(blockpos1);
                    if (j > -1) {
                        this.reorderListAtCollision(l, j);

                        for (int k = 0; k <= j + l; k++) {
                            BlockPos blockpos2 = this.toPush.get(k);
                            if (isSticky(World.getBlockState(blockpos2)) && !this.addBranchingBlocks(blockpos2)) {
                                return false;
                            }
                        }

                        return true;
                    }

                    blockstate = World.getBlockState(blockpos1);
                    if (blockstate.isAir()) {
                        return true;
                    }

                    if (!PistonBaseBlock.isPushable(blockstate, this.pushDirection, true, this.pushDirection)
                        || blockpos1.equals(this.pistonPos)) {
                        return false;
                    }

                    if (blockstate.block().pushReaction() == PushReaction.DESTROY) {
                        this.toDestroy.add(blockpos1);
                        return true;
                    }

                    if (this.toPush.size() >= MAX_PUSH_DEPTH) {
                        return false;
                    }

                    this.toPush.add(blockpos1);
                    l++;
                    j1++;
                }
            }
        }
    }

    private void reorderListAtCollision(int offsets, int index) {
        List<BlockPos> list = new ArrayList<>(this.toPush.subList(0, index));
        List<BlockPos> list1 = new ArrayList<>(this.toPush.subList(this.toPush.size() - offsets, this.toPush.size()));
        List<BlockPos> list2 = new ArrayList<>(this.toPush.subList(index, this.toPush.size() - offsets));
        this.toPush.clear();
        this.toPush.addAll(list);
        this.toPush.addAll(list1);
        this.toPush.addAll(list2);
    }

    private boolean addBranchingBlocks(BlockPos fromPos) {
        BlockState blockstate = World.getBlockState(fromPos);

        for (Direction direction : DIRECTIONS) {
            if (direction.getAxis() != this.pushDirection.getAxis()) {
                BlockPos blockpos = fromPos.relative(direction);
                BlockState blockstate1 = World.getBlockState(blockpos);
                if (canStickToEachOther(blockstate1, blockstate) && !this.addBlockLine(blockpos, direction)) {
                    return false;
                }
            }
        }

        return true;
    }
}
