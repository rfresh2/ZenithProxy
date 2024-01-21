package com.zenith.feature.pathing;

import com.zenith.event.module.ClientTickEvent;
import com.zenith.module.impl.PlayerSimulation;
import com.zenith.util.math.MathHelper;
import org.cloudburstmc.math.vector.Vector2f;

import java.util.*;

import static com.zenith.Shared.*;
import static com.zenith.event.EventConsumer.of;

public class Pathing {
    private final Set<MovementInputRequest> movementInputRequests = Collections.synchronizedSet(new HashSet<>());

    public Pathing() {
        EVENT_BUS.subscribe(this,
                            of(ClientTickEvent.class, this::handleTick)
        );
    }

    /**
     * Interface to request movement on the next tick
     */

    public void moveReq(final MovementInputRequest movementInputRequest) {
        this.movementInputRequests.add(movementInputRequest);
    }

    public void moveRot(final Input input, final float yaw, final float pitch, final int priority) {
        this.moveReq(new MovementInputRequest(Optional.of(input), Optional.of(yaw), Optional.of(pitch), priority));
    }

    public void moveYaw(final Input input, final float yaw, final int priority) {
        this.moveReq(new MovementInputRequest(Optional.of(input), Optional.of(yaw), Optional.empty(), priority));
    }

    public void moveRotTowards(final double x, final double z, final int priority) {
        final float yaw = yawToXZ(x, z);
        this.moveReq(new MovementInputRequest(Optional.of(forwardInput()), Optional.of(yaw), Optional.empty(), priority));
    }

    public void moveRotTowards(final double x, final double y, final double z, final int priority) {
        final Vector2f rotationTo = rotationTo(x, y, z);
        this.moveReq(new MovementInputRequest(Optional.of(forwardInput()), Optional.of(rotationTo.getX()), Optional.of(rotationTo.getY()), priority));
    }

    public void moveRotTowardsBlockPos(final int x, final int z, final int priority) {
        final float yaw = yawToXZ(x + 0.5, z + 0.5);
        this.moveReq(new MovementInputRequest(Optional.of(forwardInput()), Optional.of(yaw), Optional.empty(), priority));
    }

    public void moveRotSneakTowardsBlockPos(final int x, final int z, final int priority) {
        final float yaw = yawToXZ(x + 0.5, z + 0.5);
        this.moveReq(new MovementInputRequest(Optional.of(forwardSneakInput()), Optional.of(yaw), Optional.empty(), priority));
    }

    public void move(final Input input, final int priority) {
        this.moveReq(new MovementInputRequest(Optional.of(input), Optional.empty(), Optional.empty(), priority));
    }

    public void rotate(final float yaw, final float pitch, final int priority) {
        this.moveReq(new MovementInputRequest(Optional.empty(), Optional.of(yaw), Optional.of(pitch), priority));
    }

    public void rotateTowards(final double x, final double y, final double z, final int priority) {
        final Vector2f rotationTo = rotationTo(x, y, z);
        this.moveReq(new MovementInputRequest(Optional.empty(), Optional.of(rotationTo.getX()), Optional.of(rotationTo.getY()), priority));
    }

    public void rotateYaw(final float yaw, final int priority) {
        this.moveReq(new MovementInputRequest(Optional.empty(), Optional.of(yaw), Optional.empty(), priority));
    }

    public void rotatePitch(final float pitch, final int priority) {
        this.moveReq(new MovementInputRequest(Optional.empty(), Optional.empty(), Optional.of(pitch), priority));
    }

    public void stop(final int priority) {
        this.moveReq(new MovementInputRequest(Optional.empty(), Optional.empty(), Optional.empty(), priority));
    }

    public void jump(final int priority) {
        this.moveReq(new MovementInputRequest(Optional.of(jumpInput()), Optional.empty(), Optional.empty(), priority));
    }

    public void handleTick(final ClientTickEvent event) {
        this.movementInputRequests.stream()
            .max(Comparator.comparingInt(MovementInputRequest::priority))
            .ifPresent(request -> MODULE_MANAGER.get(PlayerSimulation.class).doMovement(request));
        this.movementInputRequests.clear();
    }

    // todo: Pathing interface based on goals
    //  i.e. set a destination XYZ and use A* to path to it over multiple ticks

    /**
     * Helper methods for immediate movement mode
     */

    public static Input forwardInput() {
        return new Input(
            true,
            false,
            false,
            false,
            false,
            false,
            false
        );
    }

    public static Input forwardSneakInput() {
        return new Input(
            true,
            false,
            false,
            false,
            false,
            true,
            false
        );
    }

    public static Input jumpInput() {
        return new Input(
            false,
            false,
            false,
            false,
            true,
            false,
            false
        );
    }

    public static float yawToXZ(final double x, final double z) {
        final double dx = x - CACHE.getPlayerCache().getX();
        final double dz = z - CACHE.getPlayerCache().getZ();
        final double yaw = Math.toDegrees(Math.atan2(dz, dx)) - 90.0;
        return (float) yaw;
    }

    public static Vector2f rotationTo(final double x, final double y, final double z) {
        final double dx = x - CACHE.getPlayerCache().getX();
        final double dy = y - (MODULE_MANAGER.get(PlayerSimulation.class).getEyeY());
        final double dz = z - CACHE.getPlayerCache().getZ();
        final double distance = Math.sqrt(dx * dx + dz * dz);
        final double yaw = Math.toDegrees(Math.atan2(dz, dx)) - 90.0;
        final double pitch = -Math.toDegrees(Math.atan2(dy, distance));
        return Vector2f.from((float) yaw, (float) pitch);
    }

    public static Position getCurrentPlayerPos() {
        return new Position(MathHelper.round(CACHE.getPlayerCache().getX(), 5), MathHelper.round(CACHE.getPlayerCache().getY(), 5), MathHelper.round(CACHE.getPlayerCache().getZ(), 5));
    }
}
