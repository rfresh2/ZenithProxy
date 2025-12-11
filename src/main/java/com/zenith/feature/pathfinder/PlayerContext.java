package com.zenith.feature.pathfinder;

import com.zenith.cache.data.entity.EntityLiving;
import com.zenith.feature.player.Bot;
import com.zenith.feature.player.Rotation;
import com.zenith.feature.player.raycast.BlockRaycastResult;
import com.zenith.feature.player.raycast.RaycastHelper;
import com.zenith.mc.block.BlockPos;
import org.cloudburstmc.math.vector.Vector3d;

import java.util.Optional;
import java.util.stream.Stream;

import static com.zenith.Globals.BOT;
import static com.zenith.Globals.CACHE;

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
        // TODO find a better way to deal with soul sand!!!!!
        BlockPos feet = new BlockPos(player().getX(), player().getY() + 0.1251, player().getZ());

        // todo: ???
        if (BlockStateInterface.getBlock(feet).name().endsWith("_slab")) {
            return feet.above();
        }

        return feet;
    }

    //
    //    default Vec3 playerFeetAsVec() {
    //        return new Vec3(player().position().x, player().position().y, player().position().z);
    //    }
    //
    public Vector3d playerHead() {
        return Vector3d.from(player().getX(), player().getEyeY(), player().getZ());
    }

//
//    default Vec3 playerMotion() {
//        return player().getDeltaMovement();
//    }
//
//    BetterBlockPos viewerPos();
//
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
