package com.zenith.feature.pathfinder.process;

import com.zenith.cache.data.entity.EntityLiving;
import com.zenith.feature.inventory.InventoryActionRequest;
import com.zenith.feature.inventory.actions.MoveToHotbarSlot;
import com.zenith.feature.inventory.actions.SetHeldItem;
import com.zenith.feature.inventory.actions.WaitAction;
import com.zenith.feature.inventory.util.InventoryUtil;
import com.zenith.feature.pathfinder.Baritone;
import com.zenith.feature.pathfinder.PathingCommand;
import com.zenith.feature.pathfinder.PathingCommandType;
import com.zenith.feature.pathfinder.PathingRequestFuture;
import com.zenith.feature.pathfinder.goals.Goal;
import com.zenith.feature.pathfinder.goals.GoalNear;
import com.zenith.feature.pathfinder.movement.MovementHelper;
import com.zenith.feature.player.*;
import com.zenith.feature.player.raycast.RaycastHelper;
import com.zenith.mc.block.*;
import com.zenith.mc.item.ItemData;
import com.zenith.util.math.MathHelper;
import lombok.Data;
import org.cloudburstmc.math.vector.Vector2f;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.Hand;
import org.geysermc.mcprotocollib.protocol.data.game.inventory.MoveToHotbarAction;
import org.jspecify.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import static com.zenith.Globals.*;

// todo: raycast setting for choosing path goals
//  ie. if raycast to target is blocked after near goal, find a new pathing goal that has direct line of sight
public class InteractWithProcess extends BaritoneProcessHelper {

    private @Nullable PathingRequestFuture future;
    private @Nullable InteractTarget target = null;
    private int tries = 0;

    public InteractWithProcess(final Baritone baritone) {
        super(baritone);
    }

    public PathingRequestFuture rightClickBlock(int x, int y, int z) {
        return interact(new InteractWithBlock(x, y, z, false));
    }

    public PathingRequestFuture leftClickBlock(int x, int y, int z) {
        return interact(new InteractWithBlock(x, y, z, true));
    }

    public PathingRequestFuture rightClickEntity(EntityLiving entity) {
        return interact(new InteractWithEntity(new WeakReference<>(entity), false));
    }

    public PathingRequestFuture leftClickEntity(EntityLiving entity) {
        return interact(new InteractWithEntity(new WeakReference<>(entity), true));
    }

    public PathingRequestFuture breakBlock(int x, int y, int z, boolean autoTool) {
        return interact(new BreakBlock(x, y, z, autoTool));
    }

    public PathingRequestFuture placeBlock(int x, int y, int z, ItemData placeItem) {
        return interact(new PlaceBlock(x, y, z, placeItem));
    }

    public PathingRequestFuture interact(InteractTarget target) {
        onLostControl();
        this.target = target;
        this.future = new PathingRequestFuture();
        return future;
    }

    @Override
    public boolean isActive() {
        return target != null;
    }

    @Override
    public PathingCommand onTick(final boolean calcFailed, final boolean isSafeToCancel, final Goal currentGoal, final PathingCommand prevCommand) {
        var t = target;
        if (t == null) {
            onLostControl();
            return null;
        }
        PathingCommand pathingCommand = t.pathingCommand();
        if (pathingCommand == null) {
            if (t.succeeded() && future != null) {
                future.complete(true);
                future.notifyListeners();
            }
            onLostControl();
            return new PathingCommand(null, PathingCommandType.DEFER);
        }
        if (calcFailed) {
            if (++tries > CONFIG.client.extra.pathfinder.interactWithProcessMaxPathTries) {
                onLostControl();
                return null;
            }
        }
        return pathingCommand;
    }

    @Override
    public void onLostControl() {
        target = null;
        if (future != null && !future.isCompleted()) {
            future.complete(false);
        }
        future = null;
        tries = 0;
    }

    @Override
    public String displayName0() {
        return "InteractWith: " + target;
    }

    public interface InteractTarget extends ProcessUtil {
        PathingCommand pathingCommand();
        boolean succeeded();
    }

    @Data
    public static class PlaceBlock implements InteractTarget {
        private final int x;
        private final int y;
        private final int z;
        private final ItemData placeItem;
        private boolean succeeded = false;

        public PlaceBlock(
            int x,
            int y,
            int z,
            ItemData placeItem
        ) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.placeItem = placeItem;
        }

        public void interact(Hand hand, PlaceTarget placeTarget, Rotation rotation) {
            var in = Input.builder()
                .hand(hand)
                .clickRequiresRotation(true)
                .clickTarget(new ClickTarget.BlockPosition(placeTarget.supportingBlockState().x(), placeTarget.supportingBlockState().y(), placeTarget.supportingBlockState().z()))
                .rightClick(true);
            // will often need a second tick to place with rotation
            INPUTS.submit(
                InputRequest.builder()
                    .owner(this)
                    .input(in.build())
                    .yaw(rotation.yaw())
                    .pitch(rotation.pitch())
                    .priority(Baritone.getPriority() + 1)
                    .build()
            ).addInputExecutedListener(f -> {
                if (futureSucceeded(f, placeTarget)) {
                    PATH_LOG.info("Placed block at: [{}, {}, {}] with item: {}", x, y, z, placeItem);
                    succeeded = true;
                }
            });
        }

        @Override
        public PathingCommand pathingCommand() {
            if (succeeded || !targetValid()) return null;
            var distToTarget = MathHelper.distance3d(
                CACHE.getPlayerCache().getX(), CACHE.getPlayerCache().getY(), CACHE.getPlayerCache().getZ(),
                x, y, z
            );
//            if (selfInPlaceTarget()) {
//                // todo: try to path away a block
//                info("We are blocking the place position [{}, {}, {}], stopping", x, y, z);
//                return null;
//            }
            if (distToTarget <= BOT.getBlockReachDistance() + 8) {
                if (CONFIG.client.extra.pathfinder.placeBlockVerifyAbleToPlace && entityInPlaceTarget()) {
                    info("An entity is blocking the place position [{}, {}, {}], stopping", x, y, z);
                    return null;
                }
                var placeTargets = findPlaceTargets();
                if (placeTargets.isEmpty()) {
                    info("No valid blocks to place against, stopping");
                    return null;
                }
                for (int i = 0; i < placeTargets.size(); i++) {
                    final var placeTarget = placeTargets.get(i);
                    var rotation = rotationToPlaceTarget(placeTarget);
                    if (rotation == null) {
                        continue; // no valid rotation found
                    }
                    Hand hand = Hand.MAIN_HAND;
                    // will never be -1 (missing) as it's checked in targetValid
                    var itemSlot = InventoryUtil.searchPlayerInventory(item -> item.getId() == placeItem.id());
                    if (itemSlot >= 36 && itemSlot <= 44) { // in hotbar
                        INVENTORY.submit(InventoryActionRequest.builder()
                            .owner(this)
                            .priority(Baritone.getPriority() + 1)
                            .actions(new SetHeldItem(itemSlot - 36))
                            .build());
                    } else if (itemSlot >= 9 && itemSlot <= 36) { // in main inv
                        INVENTORY.submit(InventoryActionRequest.builder()
                            .owner(this)
                            .priority(Baritone.getPriority() + 1)
                            .actions(
                                new MoveToHotbarSlot(itemSlot, MoveToHotbarAction.SLOT_6),
                                new SetHeldItem(6))
                            .build());
                    } else if (itemSlot == 45) { // in offhand
                        INVENTORY.submit(InventoryActionRequest.builder()
                            .owner(this)
                            .priority(Baritone.getPriority() + 1)
                            .actions(new WaitAction())
                            .build());
                        hand = Hand.OFF_HAND;
                    }
                    if (hand == Hand.MAIN_HAND) { // item will be in hotbar
                        if (CACHE.getPlayerCache().getHeldItemSlot() == itemSlot - 36) {
                            interact(Hand.MAIN_HAND, placeTarget, rotation);
                        }
                    } else {
                        interact(Hand.OFF_HAND, placeTarget, rotation);
                    }
                    return new PathingCommand(null, PathingCommandType.REQUEST_PAUSE);
                }
            }
            // todo: some antistuck func here
            int rangeSq = Math.max(2, ((int) Math.pow(BOT.getBlockReachDistance() - 1, 2)));
            return new PathingCommand(new GoalNear(x, y, z, rangeSq), PathingCommandType.REVALIDATE_GOAL_AND_PATH);
        }

        public boolean targetValid() {
            if (InventoryUtil.searchPlayerInventory(i -> i.getId() == placeItem.id()) == -1) {
                // item not in inventory
                info("Item: {} not in inventory, stopping", placeItem.name());
                return false;
            }
            if (World.isChunkLoadedBlockPos(x, z)) {
                Block block = World.getBlock(x, y, z);
                if (CONFIG.client.extra.pathfinder.placeBlockVerifyAbleToPlace && !block.isAir()) {
                    info("A block: {} is already at [{}, {}, {}], stopping", block.name(), x, y, z);
                    return false;
                }
            }
            return true;
        }

        public @Nullable Rotation rotationToPlaceTarget(PlaceTarget placeTarget) {
            int placeX = placeTarget.supportingBlockState().x();
            int placeY = placeTarget.supportingBlockState().y();
            int placeZ = placeTarget.supportingBlockState().z();
            Position center = World.blockInteractionCenter(placeX, placeY, placeZ);
            Vector2f centerRotation = RotationHelper.rotationTo(center.x(), center.y(), center.z());
            var centerRaycastResult = RaycastHelper.playerEyeRaycastThroughToBlockTarget(placeX, placeY, placeZ, centerRotation.getX(), centerRotation.getY());
            if (centerRaycastResult.hit() && centerRaycastResult.x() == placeX && centerRaycastResult.y() == placeY && centerRaycastResult.z() == placeZ && centerRaycastResult.direction() == placeTarget.direction()) {
                return new Rotation(centerRotation.getX(), centerRotation.getY());
            }
            // iterate to find a valid place target
            // basically just guess a bunch of rotations around the center
            // there might be a better solution than brute forcing idk math is hard
            double step = 0.1;
            double maxStep = 0.5;
            // todo: refactor to an iterator instead of list
            var posList = rotationStepList(center.x(), center.y(), center.z(), step, maxStep);
            for (var pos : posList) {
                Vector2f rotation = RotationHelper.rotationTo(pos.x(), pos.y(), pos.z());
                var raycastResult = RaycastHelper.playerEyeRaycastThroughToBlockTarget(placeX, placeY, placeZ, rotation.getX(), rotation.getY());
                if (raycastResult.hit() && raycastResult.x() == placeX && raycastResult.y() == placeY && raycastResult.z() == placeZ && raycastResult.direction() == placeTarget.direction()) {
                    return new Rotation(rotation.getX(), rotation.getY());
                }
            }
            return null; // no valid rotation found
        }

        public List<Position> rotationStepList(double x, double y, double z, double step, double maxStep) {
            var result = new ArrayList<Position>();
            for (double d = step; d <= maxStep; d += step) {
                for (int ddx = -1; ddx <= 1; ddx++) {
                    for (int ddy = -1; ddy <= 1; ddy++) {
                        for (int ddz = -1; ddz <= 1; ddz++) {
                            if (ddx == 0 && ddy == 0 && ddz == 0) continue; // skip the center point
                            double newX = x + (ddx * d);
                            double newY = y + (ddy * d);
                            double newZ = z + (ddz * d);
                            result.add(new Position(newX, newY, newZ));
                        }
                    }
                }
            }
            return result;
        }

        public record PlaceTarget(
            // supporting block position
            BlockState supportingBlockState,
            // face direction we must intersect the raycast with
            Direction direction
        ) { }

        public boolean entityInPlaceTarget() {
            var entityCbs = new ArrayList<LocalizedCollisionBox>();
            var blockCb = new LocalizedCollisionBox(new CollisionBox(0, 1, 0, 1, 0, 1), x, y, z);
            World.getEntityCollisionBoxes(blockCb, entityCbs, entity -> entity.getEntityData().blocksBuilding());
            if (!entityCbs.isEmpty()) {
                return true;
            }
            return false;
        }

        public boolean selfInPlaceTarget() {
            var blockCb = new LocalizedCollisionBox(new CollisionBox(0, 1, 0, 1, 0, 1), x, y, z);
            if (BOT.getPlayerCollisionBox().intersects(blockCb)) {
                return true;
            }
            return false;
        }

        // ordered by priority
        static final Direction[] placeDirections = new Direction[]{
            // some blocks like shulkers should prefer to be placed on the floor face
            Direction.DOWN,
            Direction.SOUTH,
            Direction.EAST,
            Direction.NORTH,
            Direction.WEST,
            Direction.UP
        };

        public List<PlaceTarget> findPlaceTargets() {
            ArrayList<PlaceTarget> validPlaces = new ArrayList<>();
            for (var faceVec : placeDirections) {
                int dx = x + faceVec.x();
                int dy = y + faceVec.y();
                int dz = z + faceVec.z();
                int blockStateId = World.getBlockStateId(dx, dy, dz);
                if (!MovementHelper.canPlaceAgainst(blockStateId)) continue;
                var blockState = World.getBlockState(dx, dy, dz);
                validPlaces.add(new PlaceTarget(blockState, faceVec.invert()));
            }
            return validPlaces;
        }

        @Override
        public boolean succeeded() {
            return succeeded;
        }

        public boolean futureSucceeded(InputRequestFuture future, PlaceTarget placeTarget) {
            if (!future.getNow()) return false;
            if (!(future.getClickResult() instanceof ClickResult.RightClickResult rightClickResult)) return false;
            return rightClickResult.getBlockX() == placeTarget.supportingBlockState().x()
                && rightClickResult.getBlockY() == placeTarget.supportingBlockState().y()
                && rightClickResult.getBlockZ() == placeTarget.supportingBlockState().z();
        }
    }

    @Data
    public static class BreakBlock implements InteractTarget {
        private final int x;
        private final int y;
        private final int z;
        private final boolean autoTool;
        private boolean isBreaking = false;
        private boolean succeeded = false;

        @Override
        public PathingCommand pathingCommand() {
            if (succeeded || !targetValid()) return null;
            isBreaking = BOT.getInteractions().isDestroying(x, y, z);
            if (canInteract()) {
                Hand hand = Hand.MAIN_HAND;
                if (autoTool) {
                    int toolSlot = InventoryUtil.bestToolAgainst(BlockRegistry.STONE);
                    if (toolSlot >= 9) {
                        if (toolSlot < 36) { // in main inv
                            INVENTORY.submit(InventoryActionRequest.builder()
                                .owner(this)
                                .actions(new MoveToHotbarSlot(toolSlot, MoveToHotbarAction.from(0)))
                                .priority(Baritone.getPriority())
                                .build());
                        } else if (toolSlot <= 44) { // in hotbar
                            INVENTORY.submit(InventoryActionRequest.builder()
                                .owner(this)
                                .actions(new SetHeldItem(toolSlot - 36))
                                .priority(Baritone.getPriority())
                                .build());
                        } else if (toolSlot == 45) { // in offhand
                            hand = Hand.OFF_HAND;
                        }
                    }
                }
                interact(hand);
                return new PathingCommand(null, PathingCommandType.REQUEST_PAUSE);
            }
            // todo: some antistuck func here
            int rangeSq = Math.max(2, ((int) Math.pow(BOT.getBlockReachDistance() - 1, 2)));
            return new PathingCommand(new GoalNear(x, y, z, rangeSq), PathingCommandType.REVALIDATE_GOAL_AND_PATH);
        }

        @Override
        public boolean succeeded() {
            return this.succeeded;
        }

        public boolean targetValid() {
            if (World.isChunkLoadedBlockPos(x, z)) {
                Block block = World.getBlock(x, y, z);
                if (block.isAir()) {
                    if (isBreaking) {
                        succeeded = true;
                        info("Block [{}, {}, {}] broken!", x, y, z);
                        return false;
                    }
                    info("No block is at [{}, {}, {}], stopping", x, y, z);
                    return false;
                }
                if (World.isFluid(block)) {
                    info("A fluid {} is at [{}, {}, {}], stopping", block.name(), x, y, z);
                    return false;
                }
                if (block.destroySpeed() < 0) {
                    info("An unbreakable block {} is at [{}, {}, {}], stopping", block.name(), x, y, z);
                    return false;
                }
                var cbs = BLOCK_DATA.getInteractionBoxesFromBlockStateId(World.getBlockStateId(x, y, z));
                if (cbs.isEmpty()) {
                    info("A block without interaction boxes is at target position, stopping");
                    return false;
                }
            }
            return true;
        }

        public boolean canInteract() {
            Position center = World.blockInteractionCenter(x, y, z);
            Vector2f rotation = RotationHelper.rotationTo(center.x(), center.y(), center.z());
            var blockRaycastResult = RaycastHelper.playerEyeRaycastThroughToBlockTarget(x, y, z, rotation.getX(), rotation.getY());
            if (!blockRaycastResult.hit()) return false;
            if (blockRaycastResult.x() != x || blockRaycastResult.y() != y || blockRaycastResult.z() != z) return false;
            return true;
        }

        public void interact(Hand hand) {
            var in = Input.builder()
                .hand(hand)
                .clickRequiresRotation(true)
                .clickTarget(new ClickTarget.BlockPosition(x, y, z))
                .leftClick(true);
            Position center = World.blockInteractionCenter(x, y, z);
            Vector2f rot = RotationHelper.rotationTo(center.x(), center.y(), center.z());
            INPUTS.submit(
                InputRequest.builder()
                    .owner(this)
                    .input(in.build())
                    .yaw(rot.getX())
                    .pitch(rot.getY())
                    .priority(Baritone.getPriority() + 1)
                    .build()
            ).addInputExecutedListener(f -> {
                if (futureSucceeded(f)) {
                    if (!isBreaking) {
                        info("Started breaking block {} at [{}, {}, {}]", World.getBlock(x, y, z).name(), x, y, z);
                    }
                    isBreaking = true;
                }
            });
        }

        public boolean futureSucceeded(InputRequestFuture future) {
            if (!future.getNow()) return false;
            if (!(future.getClickResult() instanceof ClickResult.LeftClickResult leftClickResult)) return false;
            return leftClickResult.getBlockX() == x && leftClickResult.getBlockY() == y && leftClickResult.getBlockZ() == z;
        }
    }

    @Data
    public static class InteractWithBlock implements InteractTarget {
        private final int x;
        private final int y;
        private final int z;
        private final boolean leftClick;
        private boolean succeeded = false;

        @Override
        public PathingCommand pathingCommand() {
            if (succeeded || !targetValid()) return null;
            if (canInteract()) {
                interact();
                return new PathingCommand(null, PathingCommandType.REQUEST_PAUSE);
            }
            // todo: some antistuck func here
            int rangeSq = Math.max(2, ((int) Math.pow(BOT.getBlockReachDistance() - 1, 2)));
            return new PathingCommand(new GoalNear(x, y, z, rangeSq), PathingCommandType.REVALIDATE_GOAL_AND_PATH);
        }

        @Override
        public boolean succeeded() {
            return this.succeeded;
        }

        public boolean targetValid() {
            if (World.isChunkLoadedBlockPos(x, z)) {
                Block block = World.getBlock(x, y, z);
                if (block.isAir()) {
                    info("No block is at [{}, {}, {}], stopping", x, y, z);
                    return false;
                }
                var cbs = BLOCK_DATA.getInteractionBoxesFromBlockStateId(World.getBlockStateId(x, y, z));
                if (cbs.isEmpty()) {
                    info("A block {} without interaction boxes is at [{}, {}, {}], stopping", block.name(), x, y, z);
                    return false;
                }
            }
            return true;
        }

        public boolean canInteract() {
            Position center = World.blockInteractionCenter(x, y, z);
            Vector2f rotation = RotationHelper.rotationTo(center.x(), center.y(), center.z());
            var blockRaycastResult = RaycastHelper.playerEyeRaycastThroughToBlockTarget(x, y, z, rotation.getX(), rotation.getY());
            if (!blockRaycastResult.hit()) return false;
            if (blockRaycastResult.x() != x || blockRaycastResult.y() != y || blockRaycastResult.z() != z) return false;
            return true;
        }

        public void interact() {
            var in = Input.builder()
                .hand(Hand.MAIN_HAND)
                .clickRequiresRotation(true)
                .clickTarget(new ClickTarget.BlockPosition(x, y, z));
            if (leftClick) {
                in.leftClick(true);
            } else {
                in.rightClick(true);
            }
            Position center = World.blockInteractionCenter(x, y, z);
            Vector2f rot = RotationHelper.rotationTo(center.x(), center.y(), center.z());
            INPUTS.submit(
                InputRequest.builder()
                    .owner(this)
                    .input(in.build())
                    .yaw(rot.getX())
                    .pitch(rot.getY())
                    .priority(Baritone.getPriority() + 1)
                    .build())
                .addInputExecutedListener(future -> {
                    if (futureSucceeded(future)) {
                        info("{} clicked block: {} at: [{}, {}, {}]", leftClick ? "left" : "right", World.getBlock(x, y, z).name(), x, y, z);
                        succeeded = true;
                    }
                });
        }

        public boolean futureSucceeded(InputRequestFuture future) {
            if (!future.getNow()) return false;
            if (leftClick) {
                if (!(future.getClickResult() instanceof ClickResult.LeftClickResult leftClickResult)) return false;
                return leftClickResult.getBlockX() == x && leftClickResult.getBlockY() == y && leftClickResult.getBlockZ() == z;
            } else {
                if (!(future.getClickResult() instanceof ClickResult.RightClickResult rightClickResult)) return false;
                return rightClickResult.getBlockX() == x && rightClickResult.getBlockY() == y && rightClickResult.getBlockZ() == z;
            }
        }
    }

    @Data
    public static class InteractWithEntity implements InteractTarget {
        private final WeakReference<EntityLiving> entityRef;
        private final boolean leftClick;
        private boolean succeeded = false;

        @Override
        public String toString() {
            return "InteractWithEntity{" +
                "entityRef=" + entityRef.get() +
                ", leftClick=" + leftClick +
                ", succeeded=" + succeeded +
                '}';
        }

        @Override
        public PathingCommand pathingCommand() {
            if (succeeded || !targetValid()) return null;
            var entity = entityRef.get();
            if (entity == null) {
                info("Target entity not loaded, stopping");
                return null;
            }
            if (canInteract()) {
                interact();
                return new PathingCommand(null, PathingCommandType.REQUEST_PAUSE);
            }
            // todo: some antistuck func here
            int rangeSq = Math.max(2, ((int) Math.pow(BOT.getEntityInteractDistance() - 1, 2)));
            BlockPos entityPos = new BlockPos(MathHelper.floorI(entity.getX()), Math.round(entity.getY()), MathHelper.floorI(entity.getZ()));
            return new PathingCommand(new GoalNear(entityPos, rangeSq), PathingCommandType.REVALIDATE_GOAL_AND_PATH);
        }

        @Override
        public boolean succeeded() {
            return this.succeeded;
        }

        public boolean targetValid() {
            var entity = entityRef.get();
            if (entity == null) {
                info("Target entity not loaded, stopping");
                return false;
            }
            if (CACHE.getEntityCache().get(entity.getEntityId()) != entity) {
                info("Target entity {} [{}] not loaded, stopping (2)", entity.getEntityId(), entity.getEntityType());
                return false;
            }
            LocalizedCollisionBox cb = ENTITY_DATA.getCollisionBox(entity);
            if (cb == null) {
                info("Target entity {} [{}] does not have collision boxes, stopping", entity.getEntityId(), entity.getEntityType());
                return false;
            }
            return true;
        }

        public boolean canInteract() {
            var entity = entityRef.get();
            if (entity == null) return false;
            Vector2f rotation = RotationHelper.shortestRotationTo(entity);
            var raycastResult = RaycastHelper.playerEyeRaycastThroughToTarget(entity, rotation.getX(), rotation.getY());
            if (!raycastResult.hit()) return false;
            if (raycastResult.entity() != entity) return false;
            return true;
        }

        public void interact() {
            var entity = entityRef.get();
            if (entity == null) return;
            var in = Input.builder()
                .hand(Hand.MAIN_HAND)
                .clickRequiresRotation(true)
                .clickTarget(new ClickTarget.EntityInstance(entity));
            if (leftClick) {
                in.leftClick(true);
            } else {
                in.rightClick(true);
            }
            Vector2f rot = RotationHelper.shortestRotationTo(entity);
            INPUTS.submit(
                InputRequest.builder()
                    .owner(this)
                    .input(in.build())
                    .yaw(rot.getX())
                    .pitch(rot.getY())
                    .priority(Baritone.getPriority() + 1)
                    .build())
                .addInputExecutedListener(future -> {
                    if (futureSucceeded(future)) {
                        var pos = entity.blockPos();
                        info("{} clicked entity: {} at: [{}, {}, {}]", leftClick ? "left" : "right", entity.getEntityType(), pos.x(), pos.y(), pos.z());
                        succeeded = true;
                    }
                });
        }

        public boolean futureSucceeded(InputRequestFuture future) {
            if (!future.getNow()) return false;
            if (leftClick) {
                if (!(future.getClickResult() instanceof ClickResult.LeftClickResult leftClickResult)) return false;
                return leftClickResult.getEntity() == entityRef.get();
            } else {
                if (!(future.getClickResult() instanceof ClickResult.RightClickResult rightClickResult)) return false;
                return rightClickResult.getEntity() == entityRef.get();
            }
        }
    }
}
