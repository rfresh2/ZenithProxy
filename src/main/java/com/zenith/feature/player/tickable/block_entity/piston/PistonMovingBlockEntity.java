package com.zenith.feature.player.tickable.block_entity.piston;

import com.zenith.feature.player.World;
import com.zenith.feature.player.tickable.block_entity.TickableBlockEntity;
import com.zenith.mc.block.*;
import com.zenith.mc.block.properties.PistonType;
import com.zenith.mc.block.properties.api.BlockStateProperties;
import com.zenith.util.math.MutableVec3d;
import lombok.Getter;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static com.zenith.Globals.BOT;

public class PistonMovingBlockEntity extends TickableBlockEntity {
    private static final double PUSH_OFFSET = 0.01;
    public static final double TICK_MOVEMENT = 0.51;
    // todo: i think the threadlocal be removed and replaced with function param or normal field
    private static final ThreadLocal<@Nullable Direction> NOCLIP = new ThreadLocal<>();
    @Getter private final BlockState movedState;
    @Getter private final Direction direction;
    @Getter private final boolean extending;
    @Getter private final boolean isSourcePiston;
    private float progress;
    private float progressO;
    private int deathTicks;

    public PistonMovingBlockEntity(BlockPos pos, BlockState movedState, Direction direction, boolean extending, boolean isSourcePiston) {
        super(pos);
        this.movedState = new BlockState(movedState.block(), movedState.id(), pos.x(), pos.y(), pos.z());
        this.direction = direction;
        this.extending = extending;
        this.isSourcePiston = isSourcePiston;
    }

    @Override
    public boolean isValidBlock(Block block) {
        return block == BlockRegistry.MOVING_PISTON;
    }

    private float getExtendedProgress(float progress) {
        return this.extending ? progress - 1.0F : 1.0F - progress;
    }

    private BlockState getCollisionRelatedBlockState() {
        return !this.extending && this.isSourcePiston && PistonBaseBlock.isPiston(this.movedState.block())
            ? PistonBaseBlock.pistonHeadState(
                this.blockPos,
                this.movedState.getProperty(BlockStateProperties.FACING),
                this.movedState.is(BlockRegistry.STICKY_PISTON) ? PistonType.STICKY : PistonType.DEFAULT,
                this.progress > 0.25F)
            : this.movedState;
    }

    private static void moveCollidedEntities(BlockPos pos, float partialTick, PistonMovingBlockEntity piston) {
        Direction direction = piston.getMovementDirection();
        double d0 = partialTick - piston.progress;
        List<LocalizedCollisionBox> shape = piston.getCollisionRelatedBlockState().getLocalizedCollisionBoxes();
        if (shape.isEmpty() || !BOT.canBePushedByPistons()) return;
        LocalizedCollisionBox aabb = piston.moveByProgress(bounds(shape));
        if (!PistonMath.getMovementArea(aabb, direction, d0).minmax(aabb).intersectsStrict(BOT.getPlayerCollisionBox())) return;

        if (piston.movedState.is(BlockRegistry.SLIME_BLOCK)) {
            var velocity = BOT.getVelocity();
            switch (direction.getAxis()) {
                case X -> velocity.setX(direction.x());
                case Y -> velocity.setY(direction.y());
                case Z -> velocity.setZ(direction.z());
            }
        }

        double d4 = 0.0;
        for (LocalizedCollisionBox aabb2 : shape) {
            LocalizedCollisionBox aabb1 = PistonMath.getMovementArea(piston.moveByProgress(aabb2), direction, d0);
            LocalizedCollisionBox aabb3 = BOT.getPlayerCollisionBox();
            if (aabb1.intersectsStrict(aabb3)) {
                d4 = Math.max(d4, getMovement(aabb1, direction, aabb3));
                if (d4 >= d0) {
                    break;
                }
            }
        }

        if (!(d4 <= 0.0)) {
            d4 = Math.min(d4, d0) + PUSH_OFFSET;
            moveEntityByPiston(direction, d4, direction);
            if (!piston.extending && piston.isSourcePiston) {
                fixEntityWithinPistonBase(pos, direction, d0);
            }
        }
    }

    private static void moveEntityByPiston(Direction noClipDirection, double progress, Direction direction) {
        NOCLIP.set(noClipDirection);
        try {
            BOT.moveByPiston(new MutableVec3d(progress * direction.x(), progress * direction.y(), progress * direction.z()));
        } finally {
            NOCLIP.remove();
        }
    }

    private static void moveStuckEntities(BlockPos pos, float partialTick, PistonMovingBlockEntity piston) {
        if (!piston.isStickyForEntities()) return;
        Direction direction = piston.getMovementDirection();
        if (!direction.getAxis().isHorizontal()) return;
        if (!BOT.canBePushedByPistons()) return;
        double d0 = 0;
        for (var cb : piston.movedState.getCollisionBoxes()) {
            d0 = Math.max(d0, cb.maxY());
        }
        LocalizedCollisionBox aabb = piston.moveByProgress(new LocalizedCollisionBox(
            pos.x(), pos.x() + 1.0,
            pos.y() + d0, pos.y() + 1.5000010000000001,
            pos.z(), pos.z() + 1.0,
            pos.x(), pos.y(), pos.z()));
        double d1 = partialTick - piston.progress;
        if (BOT.getPlayerCollisionBox().intersectsStrict(aabb) && matchesStickyCriteria(aabb, pos)) {
            moveEntityByPiston(direction, d1, direction);
        }
    }

    private static boolean matchesStickyCriteria(LocalizedCollisionBox box, BlockPos pos) {
        return BOT.isOnGround()
            && (BOT.isSupportedBy(pos)
                || BOT.getX() >= box.minX()
                    && BOT.getX() <= box.maxX()
                    && BOT.getZ() >= box.minZ()
                    && BOT.getZ() <= box.maxZ());
    }

    private boolean isStickyForEntities() {
        return this.movedState.is(BlockRegistry.HONEY_BLOCK);
    }

    public Direction getMovementDirection() {
        return this.extending ? this.direction : this.direction.invert();
    }

    private static double getMovement(LocalizedCollisionBox headShape, Direction direction, LocalizedCollisionBox facing) {
        return switch (direction) {
            case EAST -> headShape.maxX() - facing.minX();
            case WEST -> facing.maxX() - headShape.minX();
            case UP -> headShape.maxY() - facing.minY();
            case DOWN -> facing.maxY() - headShape.minY();
            case SOUTH -> headShape.maxZ() - facing.minZ();
            case NORTH -> facing.maxZ() - headShape.minZ();
        };
    }

    // vanilla's moveByPositionAndProgress, our boxes are already offset by the block position
    private LocalizedCollisionBox moveByProgress(LocalizedCollisionBox aabb) {
        double d0 = this.getExtendedProgress(this.progress);
        return aabb.move(d0 * this.direction.x(), d0 * this.direction.y(), d0 * this.direction.z());
    }

    private static LocalizedCollisionBox bounds(List<LocalizedCollisionBox> boxes) {
        var bounds = boxes.getFirst();
        for (int i = 1; i < boxes.size(); i++) {
            bounds = bounds.minmax(boxes.get(i));
        }
        return bounds;
    }

    private static void fixEntityWithinPistonBase(BlockPos pos, Direction dir, double progress) {
        LocalizedCollisionBox aabb = BOT.getPlayerCollisionBox();
        LocalizedCollisionBox aabb1 = new LocalizedCollisionBox(
            pos.x(), pos.x() + 1.0,
            pos.y(), pos.y() + 1.0,
            pos.z(), pos.z() + 1.0,
            pos.x(), pos.y(), pos.z());
        if (aabb.intersectsStrict(aabb1)) {
            Direction direction = dir.invert();
            double d0 = getMovement(aabb1, direction, aabb) + 0.01;
            double d1 = getMovement(aabb1, direction, aabb.intersect(aabb1)) + 0.01;
            if (Math.abs(d0 - d1) < 0.01) {
                d0 = Math.min(d0, progress) + 0.01;
                moveEntityByPiston(dir, d0, direction);
            }
        }
    }

    public void finalTick() {
        this.progress = 1.0F;
        this.progressO = this.progress;
        World.removeTickableBlockEntity(this.blockPos);
        if (World.getBlock(this.blockPos) == BlockRegistry.MOVING_PISTON) {
            if (this.isSourcePiston) {
                World.setBlockState(this.blockPos.x(), this.blockPos.y(), this.blockPos.z(), BlockRegistry.AIR.minStateId());
            } else {
                World.setBlockState(this.movedState);
            }
        }
    }

    @Override
    public void tick() {
        this.progressO = this.progress;
        if (this.progressO >= 1.0F) {
            if (this.deathTicks < 5) {
                this.deathTicks++;
            } else {
                World.removeTickableBlockEntity(this.blockPos);
                if (World.getBlock(this.blockPos) == BlockRegistry.MOVING_PISTON) {
                    BlockState blockstate = this.movedState;
                    if (Boolean.TRUE.equals(blockstate.getProperty(BlockStateProperties.WATERLOGGED))) {
                        blockstate = blockstate.withPropertyValue(BlockStateProperties.WATERLOGGED, false);
                    }
                    World.setBlockState(blockstate);
                }
            }
        } else {
            float f = this.progress + 0.5F;
            moveCollidedEntities(this.blockPos, f, this);
            moveStuckEntities(this.blockPos, f, this);
            this.progress = Math.min(f, 1.0F);
        }
    }

    public List<LocalizedCollisionBox> getCollisionBoxes() {
        List<LocalizedCollisionBox> boxes = new ArrayList<>(4);
        if (!this.extending && this.isSourcePiston && PistonBaseBlock.isPiston(this.movedState.block())) {
            boxes.addAll(this.movedState.withPropertyValue(BlockStateProperties.EXTENDED, true).getLocalizedCollisionBoxes());
        }

        Direction noClipDirection = NOCLIP.get();
        if (this.progress < 1.0 && noClipDirection == this.getMovementDirection()) {
            return boxes;
        }
        BlockState blockstate = this.isSourcePiston
            ? PistonBaseBlock.pistonHeadState(this.blockPos, this.direction, PistonType.DEFAULT, this.extending != 1.0F - this.progress < 0.25F)
            : this.movedState;
        float f = this.getExtendedProgress(this.progress);
        double d0 = this.direction.x() * f;
        double d1 = this.direction.y() * f;
        double d2 = this.direction.z() * f;
        for (var cb : blockstate.getLocalizedCollisionBoxes()) {
            boxes.add(cb.move(d0, d1, d2));
        }
        return boxes;
    }
}
