package com.zenith.feature.pathfinder.process.elytra;

import com.zenith.feature.pathfinder.process.elytra.pathfinder.PathSegment;
import com.zenith.mc.block.BlockPos;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Brady
 */
public final class UnpackedSegment {

    private final Stream<BlockPos> path;
    private final boolean finished;

    public UnpackedSegment(Stream<BlockPos> path, boolean finished) {
        this.path = path;
        this.finished = finished;
    }

    public UnpackedSegment append(Stream<BlockPos> other, boolean otherFinished) {
        // The new segment is only finished if the one getting added on is
        return new UnpackedSegment(Stream.concat(this.path, other), otherFinished);
    }

    public UnpackedSegment prepend(Stream<BlockPos> other) {
        return new UnpackedSegment(Stream.concat(other, this.path), this.finished);
    }

    public List<BlockPos> collect() {
        final List<BlockPos> path = this.path.collect(Collectors.toList());

        // Remove backtracks
        final Map<BlockPos, Integer> positionFirstSeen = new HashMap<>();
        for (int i = 0; i < path.size(); i++) {
            var pos = path.get(i);
            if (positionFirstSeen.containsKey(pos)) {
                int j = positionFirstSeen.get(pos);
                while (i > j) {
                    path.remove(i);
                    i--;
                }
            } else {
                positionFirstSeen.put(pos, i);
            }
        }

        return path;
    }

    public boolean isFinished() {
        return this.finished;
    }

    public static UnpackedSegment from(final PathSegment segment) {
        return new UnpackedSegment(
                segment.blocks.stream().map(BlockPos::new),
                segment.finished
        );
    }
}
