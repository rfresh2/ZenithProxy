package com.zenith.module.impl;

import com.zenith.event.module.ClientBotTick;
import com.zenith.feature.world.Input;
import com.zenith.feature.world.MovementInputRequest;
import com.zenith.feature.world.Pathing;
import com.zenith.feature.world.World;
import com.zenith.mc.block.*;
import com.zenith.mc.dimension.DimensionRegistry;
import com.zenith.module.Module;
import com.zenith.util.math.MathHelper;
import com.zenith.util.math.MutableVec3d;
import lombok.Getter;
import org.geysermc.mcprotocollib.protocol.data.game.entity.Effect;
import org.geysermc.mcprotocollib.protocol.data.game.entity.attribute.AttributeModifier;
import org.geysermc.mcprotocollib.protocol.data.game.entity.attribute.AttributeType;
import org.geysermc.mcprotocollib.protocol.data.game.entity.attribute.ModifierOperation;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.ByteEntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.GameMode;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.PlayerState;
import org.geysermc.mcprotocollib.protocol.data.game.entity.type.EntityType;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundExplodePacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.level.ServerboundAcceptTeleportationPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.level.ServerboundPlayerInputPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.*;

import java.util.List;
import java.util.Optional;

import static com.github.rfresh2.EventConsumer.of;
import static com.zenith.Shared.*;

public class PlayerSimulation extends Module {
    @Getter private double x;
    @Getter private double y;
    @Getter private double z;
    private double lastX;
    private double lastY;
    private double lastZ;
    @Getter private float yaw;
    @Getter private float pitch;
    private float lastYaw;
    private float lastPitch;
    @Getter private boolean onGround;
    private boolean lastOnGround;
    private boolean isSneaking;
    private boolean wasSneaking;
    private boolean isSprinting;
    private boolean lastSprinting;
    private boolean isFlying;
    private boolean isFallFlying;
    private boolean isGliding;
    private double fallDistance;
    @Getter private boolean isTouchingWater;
    private boolean isTouchingLava;
    private int ticksSinceLastPositionPacketSent;
    private final MutableVec3d stuckSpeedMultiplier = new MutableVec3d(0, 0, 0);
    private final MutableVec3d velocity = new MutableVec3d(0, 0, 0);
    private final Input movementInput = new Input();
    private int waitTicks = 0;
    private static final CollisionBox STANDING_COLLISION_BOX = new CollisionBox(-0.3, 0.3, 0, 1.8, -0.3, 0.3);
    private static final CollisionBox SNEAKING_COLLISION_BOX = new CollisionBox(-0.3, 0.3, 0, 1.5, -0.3, 0.3);
    private LocalizedCollisionBox playerCollisionBox = new LocalizedCollisionBox(STANDING_COLLISION_BOX, 0, 0, 0);
    private double gravity = 0.08;
    private float stepHeight = 0.6f;
    private float waterMovementEfficiency = 0.0f;
    private float movementEfficiency = 0.0f;
    private float speed = 0.10000000149011612f;
    private float sneakSpeed = 0.3f;
    private float jumpStrength = 0.42f;
    private boolean forceUpdateSupportingBlockPos = false;
    private Optional<BlockPos> supportingBlockPos = Optional.empty();
    private int jumpingCooldown;
    private boolean horizontalCollision = false;
    private boolean verticalCollision = false;

    @Override
    public void subscribeEvents() {
        EVENT_BUS.subscribe(
            this,
            // we want this to be one of the last thing that happens in the tick
            // to allow other modules to update the player's input
            // other modules can also do actions after this tick by setting an even lower priority
            of(ClientBotTick.class, -20000, this::tick),
            of(ClientBotTick.Starting.class, this::handleClientTickStarting),
            of(ClientBotTick.Stopped.class, this::handleClientTickStopped)
        );
    }

    @Override
    public boolean enabledSetting() {
        return true;
    }

    public synchronized void handleClientTickStarting(final ClientBotTick.Starting event) {
        syncFromCache(false);
    }

    public synchronized void handleClientTickStopped(final ClientBotTick.Stopped event) {
        if (isSneaking) {
            sendClientPacketAsync(new ServerboundPlayerCommandPacket(CACHE.getPlayerCache().getEntityId(), PlayerState.STOP_SNEAKING));
        }
        if (isSprinting) {
            sendClientPacketAsync(new ServerboundPlayerCommandPacket(CACHE.getPlayerCache().getEntityId(), PlayerState.STOP_SPRINTING));
        }
    }

    public void doRotate(float yaw, float pitch) {
        yaw = shortestRotation(yaw);
        pitch = MathHelper.clamp(pitch, -90.0F, 90.0F);
        pitch = ((int) (pitch * 10.0f)) / 10.0f; // always clamp pitch to 1 decimal place to avoid flagging for very small adjustments
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public float shortestRotation(float targetYaw) {
        float difference = targetYaw - this.yaw;
        if (difference > 180) difference -= 360;
        else if (difference < -180) difference += 360;
        return this.yaw + difference;
    }

    public synchronized void doMovementInput(
        boolean pressingForward,
        boolean pressingBack,
        boolean pressingLeft,
        boolean pressingRight,
        boolean jumping,
        boolean sneaking,
        boolean sprinting
    ) {
        if (!pressingForward || !pressingBack) {
            this.movementInput.pressingForward = pressingForward;
            this.movementInput.pressingBack = pressingBack;
        }
        if (!pressingLeft || !pressingRight) {
            this.movementInput.pressingLeft = pressingLeft;
            this.movementInput.pressingRight = pressingRight;
        }
        this.movementInput.jumping = jumping;
        this.movementInput.sneaking = sneaking;
        this.movementInput.sprinting = sprinting;
        if (movementInput.sprinting && (movementInput.pressingBack || movementInput.sneaking)) {
            movementInput.sprinting = false;
        }
    }

    public synchronized void doMovementInput(final Input input) {
        doMovementInput(
            input.pressingForward,
            input.pressingBack,
            input.pressingLeft,
            input.pressingRight,
            input.jumping,
            input.sneaking,
            input.sprinting
        );
    }

    public void doMovement(final MovementInputRequest request) {
        request.input().ifPresent(this::doMovementInput);
        if (request.yaw().isPresent() || request.pitch().isPresent()) {
            doRotate(request.yaw().orElse(this.yaw), request.pitch().orElse(this.pitch));
        }
    }

    private synchronized void tick(final ClientBotTick event) {
        if (this.jumpingCooldown > 0) --this.jumpingCooldown;
        if (!CACHE.getChunkCache().isChunkLoaded((int) x >> 4, (int) z >> 4)) return;
        if (waitTicks-- > 0) return;
        if (waitTicks < 0) waitTicks = 0;

        if (resyncTeleport()) return;

        if (Math.abs(velocity.getX()) < 0.003) velocity.setX(0);
        if (Math.abs(velocity.getY()) < 0.003) velocity.setY(0);
        if (Math.abs(velocity.getZ()) < 0.003) velocity.setZ(0);

        var fallFlyingMetadata = CACHE.getPlayerCache().getThePlayer().getMetadata().get(0);
        if (fallFlyingMetadata instanceof ByteEntityMetadata byteEntityMetadata) {
            var b = byteEntityMetadata.getPrimitiveValue();
            isFallFlying = (b & 0x80) != 0;
        } else {
            isFallFlying = false;
        }
        isSneaking = movementInput.sneaking;
        isSprinting = movementInput.sprinting;
        updateInWaterStateAndDoFluidPushing();

        if (movementInput.isJumping()) {
            if (this.onGround && jumpingCooldown == 0 && !isTouchingWater) {
                jump();
                jumpingCooldown = 10;
            } else if (isTouchingWater) {
                this.velocity.setY(this.velocity.getY() + 0.04F);
            }
            // todo: lava swimming
            // todo: full jump when at water surface
        } else jumpingCooldown = 0;

        final MutableVec3d movementInputVec = getMovementInputVec();
        if (isTouchingWater && isSneaking && !isFlying) velocity.setY(velocity.getY() - 0.04f);
        if (CACHE.getPlayerCache().getGameMode() == GameMode.SPECTATOR) {
            // todo: handle creative and spectator mode movement
            //  for now, we just stay still (unless in a vehicle)
            //  ideally we'd check if isFlying = true
            //  but we don't cache or intercept where this would be set server side yet
            velocity.set(0, 0, 0);
        } else {
            travel(movementInputVec);
        }

        if (CACHE.getPlayerCache().getThePlayer().isInVehicle()) {
            sendClientPacketsAsync(
                new ServerboundMovePlayerRotPacket(false, this.yaw, this.pitch),
                new ServerboundPlayerInputPacket(0.0f, 0.0f, false, false)
            );
            // todo: handle vehicle travel movement
        } else {
            // send movement packets based on position
            if (wasSneaking != isSneaking) {
                sendClientPacketAsync(new ServerboundPlayerCommandPacket(
                    CACHE.getPlayerCache().getEntityId(),
                    isSneaking ? PlayerState.START_SNEAKING : PlayerState.STOP_SNEAKING));
            }
            if (lastSprinting != isSprinting) {
                sendClientPacketAsync(new ServerboundPlayerCommandPacket(
                    CACHE.getPlayerCache().getEntityId(),
                    isSprinting ? PlayerState.START_SPRINTING : PlayerState.STOP_SPRINTING));
            }
            double xDelta = this.x - this.lastX;
            double yDelta = this.y - this.lastY;
            double zDelta = this.z - this.lastZ;
            double pitchDelta = this.pitch - this.lastPitch;
            double yawDelta = this.yaw - this.lastYaw;
            ++this.ticksSinceLastPositionPacketSent;
            boolean shouldUpdatePos = MathHelper.squareLen(xDelta, yDelta, zDelta) > MathHelper.square(2.0E-4) || this.ticksSinceLastPositionPacketSent >= 20;
            boolean shouldUpdateRot = pitchDelta != 0.0 || yawDelta != 0.0;
            if (shouldUpdatePos && shouldUpdateRot) {
                sendClientPacketAsync(new ServerboundMovePlayerPosRotPacket(this.onGround, this.x, this.y, this.z, this.yaw, this.pitch));
            } else if (shouldUpdatePos) {
                sendClientPacketAsync(new ServerboundMovePlayerPosPacket(this.onGround, this.x, this.y, this.z));
            } else if (shouldUpdateRot) {
                sendClientPacketAsync(new ServerboundMovePlayerRotPacket(this.onGround, this.yaw, this.pitch));
            } else if (this.lastOnGround != this.onGround) {
                sendClientPacketAsync(new ServerboundMovePlayerStatusOnlyPacket(this.onGround));
            }

            if (shouldUpdatePos) {
                this.lastX = this.x;
                this.lastY = this.y;
                this.lastZ = this.z;
                this.ticksSinceLastPositionPacketSent = 0;
            }

            if (shouldUpdateRot) {
                this.lastYaw = this.yaw;
                this.lastPitch = this.pitch;
            }

            this.lastOnGround = this.onGround;
            this.wasSneaking = this.isSneaking;
            this.lastSprinting = this.isSprinting;
        }
        this.movementInput.reset();
    }

    private MutableVec3d getMovementInputVec() {
        float strafe = 0.0F;
        if (movementInput.pressingLeft) --strafe;
        if (movementInput.pressingRight) ++strafe;
        if (movementInput.sneaking) strafe *= sneakSpeed;
        strafe = strafe * 0.98f;
        float fwd = 0.0F;
        if (movementInput.pressingForward) ++fwd;
        if (movementInput.pressingBack) --fwd;
        if (movementInput.sneaking) fwd *= sneakSpeed;
        fwd = fwd * 0.98f;
        return new MutableVec3d(strafe, 0, fwd);
    }

    private void jump() {
        this.velocity.setY(jumpStrength);
        if (this.isSprinting) {
            float sprintAngle = yaw * (float) (Math.PI / 180.0);
            this.velocity.setX(this.velocity.getX() - (Math.sin(sprintAngle) * 0.2F));
            this.velocity.setY(Math.cos(sprintAngle) * 0.2F);
        }
    }

    public synchronized void handlePlayerPosRotate(final int teleportId) {
        syncFromCache(false);
        CLIENT_LOG.debug("Server teleport {} to: {}, {}, {}", teleportId, this.x, this.y, this.z);
        sendClientPacket(new ServerboundAcceptTeleportationPacket(teleportId));
        sendClientPacket(new ServerboundMovePlayerPosRotPacket(false, this.x, this.y, this.z, this.yaw, this.pitch));
    }

    public synchronized void handleRespawn() {
        syncFromCache(true);
    }

    private void travel(MutableVec3d movementInputVec) {
        if (isTouchingWater) {
            boolean falling = velocity.getY() <= 0.0;
            float waterSlowdown = isSprinting ? 0.9f : 0.8f;
            float waterSpeed = 0.02f;
            float movementEfficiency = this.waterMovementEfficiency;
            if (!onGround) movementEfficiency *= 0.5f;
            if (movementEfficiency > 0.0f) {
                waterSlowdown += (0.54600006F - waterSlowdown) * movementEfficiency;
                waterSpeed += (this.speed - waterSpeed) * movementEfficiency;
            }
            if (CACHE.getPlayerCache().getThePlayer().getPotionEffectMap().containsKey(Effect.DOLPHINS_GRACE)) {
                waterSlowdown = 0.96f;
            }
            updateVelocity(waterSpeed, movementInputVec);
            move();
            // todo: check if on climbable block here
            velocity.multiply(waterSlowdown, 0.8f, waterSlowdown);
            double d;
            if (falling && Math.abs(velocity.getY() - 0.005) >= 0.003 && Math.abs(velocity.getY() - gravity / 16.0) < 0.003) {
                d = -0.003;
            } else {
                d = velocity.getY() - gravity / 16.0;
            }
            velocity.setY(d);
            // todo: autojump when near shore block. need more checks for water level collisions
//            if (horizontalCollision && World.isFree(playerCollisionBox.move(0, 0.6 - getY() + beforeMoveY, 0))) {
//                velocity.setY(0.3);
//            }
        } else if (isTouchingLava) {
            updateVelocity(0.02f, movementInputVec);
            move();
            // todo: fluid height conditional here, different y velocity calc when below 0.4
            velocity.multiply(0.5);
            if (gravity != 0.0) {
                velocity.add(0, -gravity / 4.0, 0);
            }
            // todo: autojump when near shore block. need more checks for water level collisions
//            if (horizontalCollision && World.isFree(playerCollisionBox.move(velocity.getX(), velocity.getY() + 0.6 - getY() + beforeMoveY, velocity.getZ()))) {
//                velocity.setY(0.3);
//            }
        } else if (isFallFlying) {
            if (velocity.getY() > -0.5 && fallDistance < 1) {
                fallDistance = 1;
            }
            var lookVec = MathHelper.calculateViewVector(yaw, pitch);
            float pitchRad = pitch * (float) (Math.PI / 180.0);
            double hLookVec = Math.sqrt(lookVec.getX() * lookVec.getX() + lookVec.getZ() * lookVec.getZ());
            double hVel = velocity.horizontalDistance();
            double lookVecLen = lookVec.length();
            double cosPitch = Math.cos(pitchRad);
            cosPitch = cosPitch * cosPitch * Math.min(1.0, lookVecLen / 0.4);
            velocity.add(0, gravity * (-1.0 + cosPitch * 0.75), 0);
            if (velocity.getY() < 0 && hLookVec > 0) {
                double m = velocity.getY() * -0.1 * cosPitch;
                velocity.add(lookVec.getX() * m / hLookVec, m, lookVec.getZ() * m / hLookVec);
            }
            if (pitchRad < 0 && hLookVec > 0) {
                double m = hVel * -Math.sin(pitchRad) * 0.04;
                velocity.add(-lookVec.getX() * m / hLookVec, m * 3.2, -lookVec.getZ() * m / hLookVec);
            }
            if (hLookVec > 0) {
                velocity.add((lookVec.getX() / hLookVec * hVel - velocity.getX()) * 0.1, 0, (lookVec.getZ() / hLookVec * hVel - velocity.getZ()) * 0.1);
            }
            velocity.multiply(0.99, 0.98, 0.99);
            move();
        } else {
            final Block floorBlock = World.getBlockAtBlockPos(getVelocityAffectingPos());
            float floorSlipperiness = BLOCK_DATA.getBlockSlipperiness(floorBlock);
            float friction = this.onGround ? floorSlipperiness * 0.91f : 0.91F;
            applyMovementInput(movementInputVec, floorSlipperiness);
            if (!isFlying) velocity.setY(velocity.getY() - gravity);
            velocity.multiply(friction, 0.9800000190734863, friction);
        }
    }

    private void move() {
        MutableVec3d localVelocity = new MutableVec3d(velocity);
        if (stuckSpeedMultiplier.lengthSquared() > 1.0E-7) {
            localVelocity.multiply(stuckSpeedMultiplier.getX(), stuckSpeedMultiplier.getY(), stuckSpeedMultiplier.getZ());
            stuckSpeedMultiplier.set(0, 0, 0);
            velocity.set(0, 0, 0);
        }

        // in-place velocity update
        adjustMovementForSneaking(localVelocity);

        List<LocalizedCollisionBox> blockCollisionBoxes = World.getIntersectingCollisionBoxes(
            playerCollisionBox.stretch(localVelocity.getX(), localVelocity.getY(), localVelocity.getZ()));
        MutableVec3d adjustedMovement = adjustMovementForCollisions(localVelocity, playerCollisionBox, blockCollisionBoxes);
        boolean isYAdjusted = localVelocity.getY() != adjustedMovement.getY();
        boolean isXAdjusted = localVelocity.getX() != adjustedMovement.getX();
        boolean isZAdjusted = localVelocity.getZ() != adjustedMovement.getZ();
        if (onGround && (isXAdjusted || isZAdjusted)) {
            // attempt to step up in xz direction block
            MutableVec3d stepUpAdjustedVec = adjustMovementForCollisions(
                new MutableVec3d(localVelocity.getX(), stepHeight, localVelocity.getZ()),
                playerCollisionBox,
                blockCollisionBoxes);
            MutableVec3d stepUpWithMoveXZAdjustedVec = adjustMovementForCollisions(
                new MutableVec3d(0.0, stepHeight, 0.0),
                playerCollisionBox.stretch(localVelocity.getX(), 0.0, localVelocity.getZ()),
                blockCollisionBoxes);
            if (stepUpWithMoveXZAdjustedVec.getY() < this.stepHeight) {
                MutableVec3d stepUpAndMoveVec = adjustMovementForCollisions(
                    new MutableVec3d(localVelocity.getX(), 0.0, localVelocity.getZ()),
                    playerCollisionBox.move(stepUpWithMoveXZAdjustedVec.getX(),
                                            stepUpWithMoveXZAdjustedVec.getY(),
                                            stepUpWithMoveXZAdjustedVec.getZ()),
                    blockCollisionBoxes);
                stepUpAndMoveVec.add(stepUpWithMoveXZAdjustedVec);
                if (stepUpAndMoveVec.horizontalLengthSquared() > stepUpAdjustedVec.horizontalLengthSquared()) {
                    stepUpAdjustedVec = stepUpAndMoveVec;
                }
            }

            if (stepUpAdjustedVec.horizontalLengthSquared() > adjustedMovement.horizontalLengthSquared()) {
                stepUpAdjustedVec.add(adjustMovementForCollisions(
                    new MutableVec3d(0.0, -stepUpAdjustedVec.getY() + localVelocity.getY(), 0.0),
                    playerCollisionBox.move(stepUpAdjustedVec.getX(),
                                            stepUpAdjustedVec.getY(),
                                            stepUpAdjustedVec.getZ()),
                    blockCollisionBoxes));
                adjustedMovement = stepUpAdjustedVec;
            }
        }
        horizontalCollision = isXAdjusted || isZAdjusted;
        verticalCollision = isYAdjusted;
        this.setOnGround(isYAdjusted && localVelocity.getY() < 0.0, adjustedMovement);

        final LocalizedCollisionBox movedPlayerCollisionBox = playerCollisionBox.move(
            adjustedMovement.getX(),
            adjustedMovement.getY(),
            adjustedMovement.getZ());
        if (isXAdjusted) velocity.setX(0.0);
        if (isYAdjusted) velocity.setY(0.0);
        if (isZAdjusted) velocity.setZ(0.0);

        // todo: apply block falling effects like bouncing off slime blocks

        this.x = ((movedPlayerCollisionBox.minX() + movedPlayerCollisionBox.maxX()) / 2.0);
        this.y = movedPlayerCollisionBox.minY();
        this.z = ((movedPlayerCollisionBox.minZ() + movedPlayerCollisionBox.maxZ()) / 2.0);
        syncPlayerCollisionBox();
        tryCheckInsideBlocks();
        float velocityMultiplier = MathHelper.lerp(movementEfficiency, this.getBlockSpeedFactor(), 1.0f);
        velocity.multiply(velocityMultiplier, 1.0, velocityMultiplier);
    }

    private void tryCheckInsideBlocks() {
        var collidingBlockStates = World.getCollidingBlockStates(playerCollisionBox);
        if (collidingBlockStates.isEmpty()) return;
        for (int i = 0; i < collidingBlockStates.size(); i++) {
            var localState = collidingBlockStates.get(i);
            if (localState.id() == BlockRegistry.BUBBLE_COLUMN.minStateId()) {
                if (World.getBlockAtBlockPos(localState.x(), localState.y() + 1, localState.z()) == BlockRegistry.AIR) {
                    velocity.setY(Math.max(-0.9, velocity.getY() - 0.03));
                } else {
                    velocity.setY(Math.max(-0.3, velocity.getY() - 0.03));
                }
            } else if (localState.id() == BlockRegistry.BUBBLE_COLUMN.maxStateId()) {
                if (World.getBlockAtBlockPos(localState.x(), localState.y() + 1, localState.z()) == BlockRegistry.AIR) {
                    velocity.setY(Math.min(1.8, velocity.getY() + 0.1));
                } else {
                    velocity.setY(Math.min(0.7, velocity.getY() + 0.06));
                }
            } else if (localState.block() == BlockRegistry.COBWEB) {
                fallDistance = 0.0;
                stuckSpeedMultiplier.set(0.25, 0.05, 0.25);
            } else if (localState.block() == BlockRegistry.HONEY_BLOCK) {
                if (isSlidingDownHoneyBlock(localState.x(), localState.y(), localState.z())) {
                    if (velocity.getY() < -0.13) {
                        double d = -0.05 / velocity.getY();
                        velocity.multiply(d, 1, d);
                        velocity.setY(-0.05);
                    } else {
                        velocity.setY(-0.05);
                    }
                }
            } else if (localState.block() == BlockRegistry.POWDER_SNOW) {
                int floorX = MathHelper.floorI(getX());
                int floorY = MathHelper.floorI(getY());
                int floorZ = MathHelper.floorI(getZ());
                if (floorX == localState.x() && floorY == localState.y() && floorZ == localState.z()) {
                    fallDistance = 0.0;
                    stuckSpeedMultiplier.set(0.9, 1.5, 0.9);
                }
            } else if (localState.block() == BlockRegistry.SWEET_BERRY_BUSH) {
                fallDistance = 0.0;
                stuckSpeedMultiplier.set(0.8, 0.75, 0.8);
            }
        }
    }

    private boolean isSlidingDownHoneyBlock(int x, int y, int z) {
        if (onGround) {
            return false;
        } else if (getY() > (double)y + 0.9375 - 1.0E-7) {
            return false;
        } else if (velocity.getY() >= -0.08) {
            return false;
        } else {
            double xDiff = Math.abs((double)x + 0.5 - getX());
            double zDiff = Math.abs((double)z + 0.5 - getZ());
            double inBB = 0.4375 + 0.3;
            return xDiff + 1.0E-7 > inBB || zDiff + 1.0E-7 > inBB;
        }
    }

    private void setOnGround(boolean onGround, MutableVec3d movement) {
        this.onGround = onGround;
        updateSupportingBlockPos(onGround, movement);
    }

    private void updateSupportingBlockPos(boolean onGround, MutableVec3d movement) {
        if (onGround) {
            var cb = new LocalizedCollisionBox(
                playerCollisionBox.minX(), playerCollisionBox.maxX(),
                playerCollisionBox.minY() - 1.0E-6, playerCollisionBox.minY(),
                playerCollisionBox.minZ(), playerCollisionBox.maxZ(),
                x, y, z);
            var supportPos = World.findSupportingBlockPos(cb);
            if (supportPos.isPresent() || this.forceUpdateSupportingBlockPos) {
                this.supportingBlockPos = supportPos;
            } else if (movement != null) {
                var moveAdjustedCb = new LocalizedCollisionBox(
                    cb.minX() - movement.getX(), cb.maxX() - movement.getX(),
                    cb.minY(), cb.maxY(),
                    cb.minZ() - movement.getZ(), cb.maxZ() - movement.getZ(),
                    x, y, z);
                this.supportingBlockPos = supportPos = World.findSupportingBlockPos(moveAdjustedCb);
            }
            this.forceUpdateSupportingBlockPos = supportPos.isEmpty();
        } else {
            this.forceUpdateSupportingBlockPos = false;
            if (this.supportingBlockPos.isPresent()) this.supportingBlockPos = Optional.empty();
        }
    }

    private BlockPos getVelocityAffectingPos() {
        if (this.supportingBlockPos.isPresent()) {
            BlockPos blockPos = this.supportingBlockPos.get();
            // todo: fences and walls calcs
            return new BlockPos(blockPos.x(), MathHelper.floorI(this.y - 0.500001), blockPos.z());
        } else {
            return new BlockPos(MathHelper.floorI(this.x), MathHelper.floorI(this.y - 0.500001), MathHelper.floorI(this.z));
        }
    }

    private MutableVec3d adjustMovementForCollisions(MutableVec3d movement, LocalizedCollisionBox pCollisionBox, List<LocalizedCollisionBox> blockCollisionBoxes) {
        double xVel = movement.getX();
        double yVel = movement.getY();
        double zVel = movement.getZ();
        if (yVel != 0.0) {
            for (LocalizedCollisionBox cb : blockCollisionBoxes) {
                yVel = cb.collideY(pCollisionBox, yVel);
            }
            pCollisionBox = pCollisionBox.move(0.0, yVel, 0.0);
        }
        boolean isMoreZMovement = Math.abs(xVel) < Math.abs(zVel);
        if (isMoreZMovement && zVel != 0.0) {
            for (LocalizedCollisionBox cb : blockCollisionBoxes) {
                zVel = cb.collideZ(pCollisionBox, zVel);
            }
            pCollisionBox = pCollisionBox.move(0.0, 0.0, zVel);
        }
        if (xVel != 0.0) {
            for (LocalizedCollisionBox cb : blockCollisionBoxes) {
                xVel = cb.collideX(pCollisionBox, xVel);
            }
            pCollisionBox = pCollisionBox.move(xVel, 0.0, 0.0);
        }
        if (!isMoreZMovement && zVel != 0.0) {
            for (LocalizedCollisionBox cb : blockCollisionBoxes) {
                zVel = cb.collideZ(pCollisionBox, zVel);
            }
        }
        return new MutableVec3d(xVel, yVel, zVel);
    }

    private boolean shouldAdjustLedgeSneak() {
        return this.isOnGround()
//            || this.fallDistance < this.stepHeight
            && !World.isSpaceEmpty(playerCollisionBox.move(0.0, -this.stepHeight, 0.0));
    }

    protected void adjustMovementForSneaking(MutableVec3d movement) {
        if (!this.isFlying
            && movement.getY() <= 0.0
            && isSneaking
            && shouldAdjustLedgeSneak()) {
            double xMovement = movement.getX();
            double zMovement = movement.getZ();

            while(xMovement != 0.0 && World.isSpaceEmpty(playerCollisionBox.move(xMovement, -this.stepHeight, 0.0))) {
                if (xMovement < 0.05 && xMovement >= -0.05)
                    xMovement = 0.0;
                else if (xMovement > 0.0)
                    xMovement -= 0.05;
                else
                    xMovement += 0.05;
            }
            while(zMovement != 0.0 && World.isSpaceEmpty(playerCollisionBox.move(0.0, -this.stepHeight, zMovement))) {
                if (zMovement < 0.05 && zMovement >= -0.05)
                    zMovement = 0.0;
                else if (zMovement > 0.0)
                    zMovement -= 0.05;
                else
                    zMovement += 0.05;
            }
            while(xMovement != 0.0 && zMovement != 0.0 && World.isSpaceEmpty(playerCollisionBox.move(xMovement, -this.stepHeight, zMovement))) {
                if (xMovement < 0.05 && xMovement >= -0.05)
                    xMovement = 0.0;
                else if (xMovement > 0.0)
                    xMovement -= 0.05;
                else
                    xMovement += 0.05;

                if (zMovement < 0.05 && zMovement >= -0.05)
                    zMovement = 0.0;
                else if (zMovement > 0.0)
                    zMovement -= 0.05;
                else
                    zMovement += 0.05;
            }
            movement.setX(xMovement);
            movement.setZ(zMovement);
        }
    }

    private void syncPlayerCollisionBox() {
        // todo: handle sneaking collision box y change
        //  need to store some additional state about the player's sneaking status in the cb or elsewhere
        playerCollisionBox = new LocalizedCollisionBox(isSneaking ? SNEAKING_COLLISION_BOX : STANDING_COLLISION_BOX, x, y, z);
    }

    private void applyMovementInput(MutableVec3d movementInputVec, float slipperiness) {
        float movementSpeed = this.getMovementSpeed(slipperiness);
        this.updateVelocity(movementSpeed, movementInputVec);
        // todo: apply climbing speed
        this.move();
    }

    private void updateVelocity(float speed, MutableVec3d movementInput) {
        MutableVec3d vec3d = movementInputToVelocity(movementInput, speed, this.yaw);
        this.velocity.add(vec3d);
    }

    private MutableVec3d movementInputToVelocity(MutableVec3d movementInput, float speed, float yaw) {
        double movementLengthSquared = movementInput.lengthSquared();
        if (movementLengthSquared < 1.0E-7) {
            return MutableVec3d.ZERO;
        } else {
            if (movementLengthSquared > 1.0) movementInput.normalize();
            movementInput.multiply(speed);
            float yawSin = (float) Math.sin(yaw * 0.017453292f);
            float yawCos = (float) Math.cos(yaw * 0.017453292f);
            return new MutableVec3d(
                movementInput.getX() * (double)yawCos - movementInput.getZ() * (double)yawSin,
                movementInput.getY(),
                movementInput.getZ() * (double)yawCos + movementInput.getX() * (double)yawSin);
        }
    }

    private float getMovementSpeed(float slipperiness) {
        return this.onGround ? this.speed * (0.21600002f / (slipperiness * slipperiness * slipperiness)) : 0.02f;
    }

    private float getBlockSpeedFactor() {
        if (this.isGliding || this.isFlying) return 1.0f;
        Block inBlock = World.getBlockAtBlockPos(MathHelper.floorI(Pathing.getCurrentPlayerX()), MathHelper.floorI(Pathing.getCurrentPlayerY()), MathHelper.floorI(Pathing.getCurrentPlayerZ()));
        float inBlockSpeedFactor = getBlockSpeedFactor(inBlock);
        if (inBlockSpeedFactor != 1.0f || World.isWater(inBlock)) return inBlockSpeedFactor;
        Block underPlayer = World.getBlockAtBlockPos(MathHelper.floorI(Pathing.getCurrentPlayerX()), MathHelper.floorI(Pathing.getCurrentPlayerY()) - 1, MathHelper.floorI(Pathing.getCurrentPlayerZ()));
        return getBlockSpeedFactor(underPlayer);
    }

    private float getBlockSpeedFactor(Block block) {
        if (block == BlockRegistry.HONEY_BLOCK || block == BlockRegistry.SOUL_SAND) return 0.4f;
        return 1.0f;
    }

    public void handleSetMotion(final double motionX, final double motionY, final double motionZ) {
        this.velocity.set(motionX, motionY, motionZ);
    }

    public void handleExplosion(final ClientboundExplodePacket packet) {
        this.velocity.add(packet.getPushX(), packet.getPushY(), packet.getPushZ());
    }

    private void syncFromCache(boolean full) {
        this.x = this.lastX = CACHE.getPlayerCache().getX();
        this.y = this.lastY = CACHE.getPlayerCache().getY();
        this.z = this.lastZ = CACHE.getPlayerCache().getZ();
        this.yaw = this.lastYaw = CACHE.getPlayerCache().getYaw();
        this.pitch = this.lastPitch = CACHE.getPlayerCache().getPitch();
        this.onGround = true; // todo: cache
        this.lastOnGround = true;
        this.velocity.set(0, 0, 0);
        this.supportingBlockPos = Optional.empty();
        this.forceUpdateSupportingBlockPos = false;
        this.ticksSinceLastPositionPacketSent = 0;
        if (full) {
            this.isSneaking = this.wasSneaking = false;
            this.isSprinting = this.lastSprinting = false;
        } else {
            this.isSneaking = this.wasSneaking = CACHE.getPlayerCache().isSneaking();
            this.isSprinting = this.lastSprinting = CACHE.getPlayerCache().isSprinting();
        }
        syncPlayerCollisionBox();
        updateAttributes();
    }

    private void updateInWaterStateAndDoFluidPushing() {
        updateInWaterStateAndDoWaterCurrentPushing();
        var currentDim = CACHE.getChunkCache().getCurrentDimension();
        double lavaSpeedMult = currentDim != null && currentDim.id() == DimensionRegistry.THE_NETHER.id()
            ? 0.007
            : 0.0023333333333333335;
        if (updateFluidHeightAndDoFluidPushing(false, lavaSpeedMult)) {
            fallDistance = 0;
            isTouchingLava = true;
        } else {
            isTouchingLava = false;
        }
    }

    private void updateInWaterStateAndDoWaterCurrentPushing() {
        if (CACHE.getPlayerCache().getThePlayer().isInVehicle()) {
            var vehicle = CACHE.getEntityCache().get(CACHE.getPlayerCache().getThePlayer().getVehicleId());
            // todo: check if boat is underwater
            if (vehicle != null && vehicle.getEntityType() == EntityType.BOAT) {
                isTouchingWater = false;
                return;
            }
        }

        if (updateFluidHeightAndDoFluidPushing(true, 0.014)) {
            fallDistance = 0;
            isTouchingWater = true;
        } else {
            isTouchingWater = false;
        }
    }

    // todo: clean up and optimize
    //  also handle missing edge cases:
    //      waterlogged blocks
    //      lava and water next to each other
    private boolean updateFluidHeightAndDoFluidPushing(boolean waterFluid, double motionScale) {
        int floorX = MathHelper.floorI(playerCollisionBox.minX() + 0.001);
        int ceilX = MathHelper.ceilI(playerCollisionBox.maxX() - 0.001);
        int floorY = MathHelper.floorI(playerCollisionBox.minY() + 0.001);
        int ceilY = MathHelper.ceilI(playerCollisionBox.maxY() - 0.001);
        int floorZ = MathHelper.floorI(playerCollisionBox.minZ() + 0.001);
        int ceilZ = MathHelper.ceilI(playerCollisionBox.maxZ() - 0.001);
        double topFluidHDelta = 0.0;
        MutableVec3d pushVec = new MutableVec3d(0, 0, 0);
        int affectingFluidsCount = 0;
        boolean touched = false;

        for (int x = floorX; x < ceilX; x++) {
            for (int y = floorY; y < ceilY; y++) {
                for (int z = floorZ; z < ceilZ; z++) {
                    double fluidHeightToWorld;
                    var blockState = World.getBlockState(x, y, z);
                    if (waterFluid) {
                        if (blockState.block() != BlockRegistry.WATER) continue;
                    } else {
                        if (blockState.block() != BlockRegistry.LAVA) continue;
                    }
                    float fluidHeight = World.getFluidHeight(blockState);
                    if (fluidHeight == 0 || (fluidHeightToWorld = y + fluidHeight) < playerCollisionBox.minY() + 0.001) continue;
                    touched = true;
                    topFluidHDelta = Math.max(fluidHeightToWorld - (playerCollisionBox.minY() + 0.001), topFluidHDelta);
                    if (!isFlying) {
                        var flowVec = World.getFluidFlow(blockState);
                        if (topFluidHDelta < 0.4) {
                            flowVec.multiply(topFluidHDelta);
                        }
                        pushVec.add(flowVec);
                        affectingFluidsCount++;
                    }
                }
            }
        }

        if (pushVec.lengthSquared() > 0) {
            if (affectingFluidsCount > 0) {
                pushVec.multiply(1.0 / affectingFluidsCount);
            }
            if (CACHE.getPlayerCache().getThePlayer().isInVehicle()) {
                pushVec.normalize();
            }
            pushVec.multiply(motionScale);
            velocity.add(pushVec);
        }
        return touched;
    }

    private boolean resyncTeleport() {
        if (!CONFIG.debug.resyncTeleports) return false;
        // can occur when a connected player disconnects in an unusual way like crashing
        if (CACHE.getPlayerCache().getTeleportQueue().isEmpty()) return false;
        int queuedTeleport = CACHE.getPlayerCache().getTeleportQueue().dequeueInt();
        warn("Detected teleport desync, resyncing. queuedTeleport: {}, queueSize: {}", queuedTeleport, CACHE.getPlayerCache().getTeleportQueue().size());
        sendClientPacket(new ServerboundAcceptTeleportationPacket(queuedTeleport));
        sendClientPacket(new ServerboundMovePlayerPosRotPacket(onGround, x, y, z, yaw, pitch));
        return true;
    }

    public void updateAttributes() {
        this.speed = getAttributeValue(AttributeType.Builtin.GENERIC_MOVEMENT_SPEED, 0.10000000149011612f);
        this.movementEfficiency = getAttributeValue(AttributeType.Builtin.GENERIC_MOVEMENT_EFFICIENCY, 0.0f);
        this.waterMovementEfficiency = getAttributeValue(AttributeType.Builtin.GENERIC_WATER_MOVEMENT_EFFICIENCY, 0.0f);
        this.stepHeight = getAttributeValue(AttributeType.Builtin.GENERIC_STEP_HEIGHT, 0.6f);
        this.gravity = getAttributeValue(AttributeType.Builtin.GENERIC_GRAVITY, 0.08f);
        this.jumpStrength = getAttributeValue(AttributeType.Builtin.GENERIC_JUMP_STRENGTH, 0.42f);
        this.sneakSpeed = getAttributeValue(AttributeType.Builtin.PLAYER_SNEAKING_SPEED, 0.3f);
    }

    private float getAttributeValue(final AttributeType.Builtin attributeType, float defaultValue) {
        var attribute = CACHE.getPlayerCache().getThePlayer().getAttributes().get(attributeType);
        if (attribute == null) return defaultValue;
        double v1 = attribute.getValue();
        for (AttributeModifier modifier : attribute.getModifiers()) {
            if (modifier.getOperation() == ModifierOperation.ADD) {
                v1 += modifier.getAmount();
            }
        }
        double v2 = v1;
        for (AttributeModifier modifier : attribute.getModifiers()) {
            if (modifier.getOperation() == ModifierOperation.ADD_MULTIPLIED_BASE) {
                v2 += v1 * modifier.getAmount();
            }
        }
        for (AttributeModifier modifier : attribute.getModifiers()) {
            if (modifier.getOperation() == ModifierOperation.ADD_MULTIPLIED_TOTAL) {
                v2 *= 1.0 + modifier.getAmount();
            }
        }
        return (float) v2;
    }
}
