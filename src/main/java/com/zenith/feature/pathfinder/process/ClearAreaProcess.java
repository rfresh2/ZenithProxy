package com.zenith.feature.pathfinder.process;

import com.zenith.feature.pathfinder.Baritone;
import com.zenith.feature.pathfinder.PathingCommand;
import com.zenith.feature.pathfinder.PathingCommandType;
import com.zenith.feature.pathfinder.PathingRequestFuture;
import com.zenith.feature.pathfinder.goals.Goal;
import com.zenith.feature.player.World;
import com.zenith.mc.block.Block;
import com.zenith.mc.block.BlockPos;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntComparator;
import it.unimi.dsi.fastutil.ints.IntList;
import lombok.Data;

import static com.zenith.Globals.BARITONE;
import static com.zenith.Globals.BLOCK_DATA;

public class ClearAreaProcess extends BaritoneProcessHelper {
    PathingRequestFuture future;
    Area area = null;

    public ClearAreaProcess(final Baritone baritone) {
        super(baritone);
    }

    public PathingRequestFuture clearArea(final BlockPos pos1, final BlockPos pos2) {
        stop();
        area = new Area(pos1, pos2);
        future = new PathingRequestFuture();
        return future;
    }

    @Override
    public boolean isActive() {
        return area != null;
    }

    @Override
    public PathingCommand onTick(final boolean calcFailed, final boolean isSafeToCancel, final Goal currentGoal, final PathingCommand prevCommand) {
        if (area == null) {
            stop();
            return null;
        }
        BlockPos playerPos = BARITONE.getPlayerContext().playerFeet();
        // todo: make isInside check allow for near margin
//        if (!area.isInside(playerPos)) {
//            BlockPos nearest = new BlockPos(
//                MathHelper.clamp(playerPos.x(), area.pos1.x(), area.pos2.x()),
//                MathHelper.clamp(playerPos.y(), area.pos1.y(), area.pos2.y()),
//                MathHelper.clamp(playerPos.z(), area.pos1.z(), area.pos2.z())
//            );
//            return new PathingCommand(new GoalNear(nearest, 4 * 4), PathingCommandType.REVALIDATE_GOAL_AND_PATH);
//        }
        area.sort(playerPos.x(), playerPos.z());
        for (int y = area.minY(); y <= area.maxY(); y++) {
            for (int x : area.xList) {
                for (int z : area.zList) {
                    Block block = World.getBlock(x, y, z);
                    if (block.isAir()) continue;
                    if (targetValid(x, y, z)) {
                        BARITONE.breakBlock(x, y, z, true);
                        return new PathingCommand(null, PathingCommandType.DEFER);
                    }
                    // todo: handle placing block onto nearby fluids before breaking
                }
            }
        }
        // todo: handle case where area is outside loaded chunks
        future.complete(true);
        future.notifyListeners();
        stop();
        return null;
    }

    public boolean targetValid(int x, int y, int z) {
        if (World.isChunkLoadedBlockPos(x, z)) {
            Block block = World.getBlock(x, y, z);
            if (block.isAir()) return false;
            if (World.isFluid(block)) return false;
            if (block.destroySpeed() < 0) return false;
            var cbs = BLOCK_DATA.getInteractionBoxesFromBlockStateId(World.getBlockStateId(x, y, z));
            if (cbs.isEmpty()) return false;
        }
        return true;
    }

    @Override
    public void onLostControl() {

    }

    @Override
    public void stop() {
        area = null;
        if (future != null && !future.isCompleted()) {
            future.complete(false);
        }
        future = null;
    }

    @Override
    public String displayName0() {
        return "Clear Area";
    }

    @Override
    public double priority() {
        return DEFAULT_PRIORITY - 1;
    }

    @Data
    public static class Area {
        private final BlockPos pos1;
        private final BlockPos pos2;

        private final IntList xList;
        private final IntList zList;

        public Area(BlockPos pos1, BlockPos pos2) {
            this.pos1 = pos1;
            this.pos2 = pos2;
            this.xList = new IntArrayList();
            this.zList = new IntArrayList();
            for (int x = minX(); x <= maxX(); x++) {
                xList.add(x);
            }
            for (int z = minZ(); z <= maxZ(); z++) {
                zList.add(z);
            }
        }

        public void sort(int px, int pz) {
            xList.sort(IntComparator.comparingInt(x -> Math.abs(x - px)));
            zList.sort(IntComparator.comparingInt(z -> Math.abs(z - pz)));
        }

        public int minX() {
            return Math.min(pos1.x(), pos2.x());
        }

        public int maxX() {
            return Math.max(pos1.x(), pos2.x());
        }

        public int minY() {
            return Math.min(pos1.y(), pos2.y());
        }

        public int maxY() {
            return Math.max(pos1.y(), pos2.y());
        }

        public int minZ() {
            return Math.min(pos1.z(), pos2.z());
        }

        public int maxZ() {
            return Math.max(pos1.z(), pos2.z());
        }

        public boolean isInside(BlockPos pos) {
            return pos.x() >= minX() && pos.x() <= maxX()
                && pos.y() >= minY() && pos.y() <= maxY()
                && pos.z() >= minZ() && pos.z() <= maxZ();
        }
    }
}
