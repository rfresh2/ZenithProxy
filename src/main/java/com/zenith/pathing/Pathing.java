package com.zenith.pathing;

import com.google.common.collect.ImmutableList;
import lombok.RequiredArgsConstructor;
import net.daporkchop.lib.math.vector.Vec3i;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static com.zenith.util.Constants.*;

@RequiredArgsConstructor
public class Pathing {
    private static final double walkBlocksPerTick = 3 / 20.0; // need to go slower than sprint to not trip NCP

    private final World world;

    // todo: this pathing doesn't know how to get around blocks
    //  i.e. sometimes you need to go in the opposite direction to get to a goal
    //  might need to incorporate some algorithm like A* to chart out a full path rather than just the next move
    public Position calculateNextMove(final BlockPos goal) {
        final Position currentPlayerPos = getCurrentPlayerPos();
        final Position goalPosition = new Position(goal.getX() + 0.5, goal.getY(), goal.getZ() + 0.5);
        final double xDelta = goalPosition.getX() - currentPlayerPos.getX();
        final double zDelta = goalPosition.getZ() - currentPlayerPos.getZ();
        if (Math.abs(xDelta) > Math.abs(zDelta)) {
            if (xDelta != 0) {
                final Position xMovePos = currentPlayerPos.addX(getNextMoveDelta(xDelta));
                if (isNextWalkSafe(xMovePos)) {
                    return xMovePos;
                }
            }
            if (zDelta != 0) {
                final Position zMovePos = currentPlayerPos.addZ(getNextMoveDelta(zDelta));
                if (isNextWalkSafe(zMovePos)) {
                    return zMovePos;
                }
            }
        } else {
            if (zDelta != 0) {
                final Position zMovePos = currentPlayerPos.addZ(getNextMoveDelta(zDelta));
                if (isNextWalkSafe(zMovePos)) {
                    return zMovePos;
                }
            }
            if (xDelta != 0) {
                final Position xMovePos = currentPlayerPos.addX(getNextMoveDelta(xDelta));
                if (isNextWalkSafe(xMovePos)) {
                    return xMovePos;
                }
            }
        }

//        CLIENT_LOG.info("Pathing: No safe movement towards goal found");
        return currentPlayerPos;
    }

    private double getNextMoveDelta(final double delta) {
        double walkD = walkBlocksPerTick * Math.signum(delta);
        if (Math.abs(walkD) > Math.abs(delta)) {
            return delta;
        } else {
            return walkD;
        }
    }

    private Instant lastFatalFallWarningTime = Instant.now();

    // empty optional when we shouldn't do a gravity move
    public Optional<Position> calculateNextGravityMove(
            // t = current tick time from when we started falling
            final int t) {
        final Position currentPlayerPos = getCurrentPlayerPos();
        if (t < 0) return Optional.of(currentPlayerPos);
        // note: if floor is > downVec Y distance away we're not going to calc a gravity move
        // might make this distance configurable but this is probably fine until there's some reason
        // we want to do a fatal fall
        final Optional<BlockPos> groundTraceResult = this.world.rayTraceCBDown(currentPlayerPos);
        if (groundTraceResult.isPresent()) {
            // todo: handle half blocks
            final BlockPos floor = groundTraceResult.get().addY(1);
            if ((double) floor.getY() == currentPlayerPos.getY()) {
                return Optional.empty(); // we're on ground
            } else {
                final double yDelta = floor.getY() - currentPlayerPos.getY();
                if (yDelta > 0) return Optional.empty();
                if (CONFIG.client.extra.antiafk.actions.safeGravity) {
                    if (calculateFallDamage(Math.abs(yDelta)) >= CACHE.getPlayerCache().getThePlayer().getHealth()) {
                        if (lastFatalFallWarningTime.plusSeconds(20).isBefore(Instant.now())) {
                            lastFatalFallWarningTime = Instant.now();
                            CLIENT_LOG.warn("Gravity: possible fatal fall detected");
                        }
                        return Optional.empty();
                    }
                }
                final double nextGravityMoveDelta = calculateGravity(t);
                Position nextGravityMove = currentPlayerPos.addY(nextGravityMoveDelta);
                if (nextGravityMove.getY() <= floor.getY()) {
                    // handle ground impact
                    nextGravityMove = currentPlayerPos.addY(yDelta);
                }
                return Optional.of(nextGravityMove);
            }
        }
        return Optional.empty();
    }

    private static final List<Double> jumpYPositions = ImmutableList.of(0.0, 0.41999998688698, 0.7531999805212, 1.00133597911214, 1.16610926093821,
            1.24918707874468, 1.17675927506424, 1.02442408821369, 0.79673560066871, 0.49520087700593, 0.1212968405392, 0.0);

    public Optional<Position> calculateNextJumpMove(final double startY, final int t) {
        final Position currentPlayerPos = getCurrentPlayerPos();
        if (t < 0) return Optional.of(currentPlayerPos);
        if (t > jumpYPositions.size() - 1) return Optional.empty();
        return Optional.of(new Position(currentPlayerPos.getX(), startY + jumpYPositions.get(t), currentPlayerPos.getZ()));
    }

    public double calculateFallDamage(final double distance) {
        // todo: check if we have feather falling + prot 4 which will increase our distance to 103 before death
        // todo: check if we're falling into a liquid
        // todo: check if we have a totem equipped
        return Math.max(0, distance - 3.5);
    }

    public double calculateGravity(final int t) {
        // https://minecraft.fandom.com/wiki/Transportation
        int a = t;
        double result = 0;
        while (a > 0) {
            result = (result - 0.08) * 0.98;
            a--;
        }
        return result;
    }

    public Position getCurrentPlayerPos() {
        return new Position(CACHE.getPlayerCache().getX(), CACHE.getPlayerCache().getY(), CACHE.getPlayerCache().getZ());
    }

    public boolean isNextWalkSafe(final Position position) {
        final Vec3i direction = position.minus(getCurrentPlayerPos()).toDirectionVector();
        final BlockPos blockPos = position.toBlockPos();
        final BlockPos groundBlockPos = blockPos.addY(-1);
        final Optional<BlockPos> legsRayTrace = this.world.rayTraceCB(position.addY(0.01), direction);
        if (legsRayTrace.isPresent()) {
            return false;
        }
        final Optional<BlockPos> headRayTrace = this.world.rayTraceCB(position.addY(1.01), direction);
        if (headRayTrace.isPresent()) {
            return false;
        }
        final boolean groundSolid = this.world.rayTraceCB(position, Vec3i.of(0, -2, 0)).isPresent();
        final boolean blocked = this.world.isSolidBlock(blockPos) || this.world.isSolidBlock(blockPos.addY(1));
        if (!CONFIG.client.extra.antiafk.actions.safeWalk) {
            if (!groundSolid) {
                if (CONFIG.client.extra.antiafk.actions.gravity) {
                    if (CONFIG.client.extra.antiafk.actions.safeGravity) {
                        Optional<BlockPos> groundTraceResult = this.world.raytraceDown(groundBlockPos);
                        if (groundTraceResult.isPresent()) {
                            final BlockPos floor = groundTraceResult.get().addY(1);
                            final double yDelta = floor.getY() - groundBlockPos.getY();
                            if (calculateFallDamage(Math.abs(yDelta)) < CACHE.getPlayerCache().getThePlayer().getHealth()) {
                                return !blocked;
                            }
                        }
                    } else {
                        return !blocked;
                    }
                }
            }
        }
        return groundSolid && !blocked;
    }

    public boolean isOnGround() {
        final Position currentPlayerPos = getCurrentPlayerPos();
        final Optional<BlockPos> groundTraceResult = this.world.rayTraceCBDown(currentPlayerPos);
        if (groundTraceResult.isPresent()) {
            final BlockPos floor = groundTraceResult.get().addY(1);
            return (double) floor.getY() == currentPlayerPos.getY();
        } else {
            return false;
        }
    }
}
