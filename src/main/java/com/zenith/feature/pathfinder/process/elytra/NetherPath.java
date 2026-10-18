package com.zenith.feature.pathfinder.process.elytra;

import com.zenith.mc.block.BlockPos;
import org.cloudburstmc.math.vector.Vector3d;

import java.util.AbstractList;
import java.util.Collections;
import java.util.List;

/**
 * @author Brady
 */
public final class NetherPath extends AbstractList<BlockPos> {

    private static final NetherPath EMPTY_PATH = new NetherPath(Collections.emptyList());

    private final List<BlockPos> backing;

    NetherPath(List<BlockPos> backing) {
        this.backing = backing;
    }

    @Override
    public BlockPos get(int index) {
        return this.backing.get(index);
    }

    @Override
    public int size() {
        return this.backing.size();
    }

    /**
     * @return The last position in the path, or {@code null} if empty
     */
    public BlockPos getLast() {
        return this.isEmpty() ? null : this.backing.get(this.backing.size() - 1);
    }

    public Vector3d getVec(int index) {
        var pos = this.get(index);
        return Vector3d.from(pos.x(), pos.y(), pos.z());
    }

    public static NetherPath emptyPath() {
        return EMPTY_PATH;
    }
}
