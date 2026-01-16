package com.zenith.feature.pathfinder;

import com.zenith.cache.data.entity.EntityLiving;
import com.zenith.feature.pathfinder.goals.Goal;
import com.zenith.mc.block.Block;
import com.zenith.mc.block.BlockPos;
import com.zenith.mc.item.ItemData;
import org.jspecify.annotations.Nullable;

import java.util.function.Predicate;

public interface Pathfinder {
    boolean isActive();
    PathingRequestFuture pathTo(Goal goal);
    PathingRequestFuture pathTo(int x, int z);
    PathingRequestFuture pathTo(int x, int y, int z);
    PathingRequestFuture thisWay(int dist);
    PathingRequestFuture getTo(Block block);
    PathingRequestFuture getTo(Block block, boolean rightClickContainerOnArrival);
    PathingRequestFuture mine(Block... blocks);
    PathingRequestFuture follow(Predicate<EntityLiving> entityPredicate);
    PathingRequestFuture follow(EntityLiving entity);
    PathingRequestFuture pickup(ItemData... items);
    PathingRequestFuture pickup();
    PathingRequestFuture leftClickBlock(int x, int y, int z);
    PathingRequestFuture rightClickBlock(int x, int y, int z);
    PathingRequestFuture breakBlock(int x, int y, int z, boolean autoTool);
    PathingRequestFuture placeBlock(int x, int y, int z, ItemData placeItem);
    PathingRequestFuture leftClickEntity(EntityLiving entity);
    PathingRequestFuture rightClickEntity(EntityLiving entity);
    PathingRequestFuture clearArea(BlockPos pos1, BlockPos pos2);
    void stop();
    @Nullable Goal currentGoal();
}
