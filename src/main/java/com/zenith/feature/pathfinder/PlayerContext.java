package com.zenith.feature.pathfinder;

import com.zenith.cache.data.entity.EntityLiving;
import com.zenith.feature.player.Bot;
import com.zenith.feature.player.Rotation;
import com.zenith.feature.player.World;
import com.zenith.feature.player.raycast.BlockRaycastResult;
import com.zenith.feature.player.raycast.RaycastHelper;
import com.zenith.mc.block.BlockPos;
import org.cloudburstmc.math.vector.Vector3d;

import java.util.Optional;
import java.util.stream.Stream;

import static com.zenith.Globals.*;

public final class PlayerContext {
    public static final PlayerContext INSTANCE = new PlayerContext();
    private PlayerContext() {}

    public static double eyeHeight(boolean ifSneaking) {
        return ifSneaking ? 1.27 : 1.62;
    }

    public Bot player() {
        return BOT;
    }

    public Stream<EntityLiving> entitiesStream() {
        return CACHE.getEntityCache().getEntities().values().stream()
            .filter(e -> e instanceof EntityLiving)
            .map(e -> (EntityLiving) e);
    }

    //
    //
    //    IWorldData worldData();
    //
    //    HitResult objectMouseOver();
    //
    public BlockPos playerFeet() {
        var feet = new BlockPos(player().getX(), player().getY(), player().getZ());
        if (player().getY() - feet.y() < 0.25) {
            return feet;
        }
        // check if we could be on a short block
        if (BLOCK_DATA.getCollisionBoxesFromBlockStateId(World.getBlockStateId(feet)).isEmpty()) {
            return feet;
        }
        var localizedFeetCbs = World.getBlockState(feet).getLocalizedCollisionBoxes();
        // check if we're inside a solid block
        var playerCb = BOT.getPlayerCollisionBox()
            .inflate(-1.0E-7, -1.0E-7, -1.0E-7);
        for (var cb : localizedFeetCbs) {
            if (playerCb.intersects(cb)) {
                return feet;
            }
        }
        // if not, shift up for blocks like half slabs and sould sand
        return feet.above();
    }

    public Vector3d playerFeetAsVec() {
        return Vector3d.from(player().getX(), player().getY(), player().getZ());
    }

    public Vector3d playerMotion() {
        var velocity = player().getVelocity();
        return Vector3d.from(velocity.getX(), velocity.getY(), velocity.getZ());
    }

    public Vector3d playerHead() {
        return Vector3d.from(player().getX(), player().getEyeY(), player().getZ());
    }

    public Rotation playerRotations() {
        return new Rotation(player().getYaw(), player().getPitch());
    }

    /**
     * Returns the block that the crosshair is currently placed over. Updated once per tick.
     *
     * @return The position of the highlighted block
     */
    public Optional<BlockPos> getSelectedBlock() {
        var raycast = RaycastHelper.playerBlockRaycast(player().getBlockReachDistance(), false);
        if (raycast.hit()) {
            return Optional.of(new BlockPos(raycast.x(), raycast.y(), raycast.z()));
        }
        return Optional.empty();
//        HitResult result = objectMouseOver();
//        if (result != null && result.getType() == HitResult.Type.BLOCK) {
//            return Optional.of(((BlockHitResult) result).getBlockPos());
//        }
//        return Optional.empty();
    }

    public BlockRaycastResult objectMouseOver() {
        return RaycastHelper.playerBlockRaycast(player().getBlockReachDistance(), false);
    }

    public boolean isLookingAt(BlockPos pos) {
        return getSelectedBlock().equals(Optional.of(pos));
    }
}
