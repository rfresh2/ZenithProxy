package com.zenith.module.impl;

import com.github.steveice10.mc.protocol.data.game.entity.EquipmentSlot;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.github.steveice10.mc.protocol.data.game.entity.player.PlayerState;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.level.ClientboundExplodePacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.level.ServerboundAcceptTeleportationPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.level.ServerboundPlayerInputPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.*;
import com.zenith.Proxy;
import com.zenith.event.SimpleEventBus;
import com.zenith.event.Subscription;
import com.zenith.event.module.ClientTickEvent;
import com.zenith.feature.pathing.*;
import com.zenith.feature.pathing.blockdata.Block;
import com.zenith.module.Module;
import com.zenith.util.math.MathHelper;
import com.zenith.util.math.MutableVec3d;
import lombok.Getter;

import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Supplier;

import static com.zenith.Shared.*;

public class PlayerSimulation extends Module {
    private double gravity = 0.08;
    private double x;
    private double y;
    private double z;
    private double lastX;
    private double lastY;
    private double lastZ;
    private float yaw;
    private float pitch;
    private float lastYaw;
    private float lastPitch;
    @Getter
    private boolean onGround;
    private boolean lastOnGround;
    private boolean isSneaking;
    private boolean wasSneaking;
    private boolean isSprinting;
    private boolean lastSprinting;
    private boolean isFlying;
    private boolean canFly;
    private boolean wasFlying;
    private boolean isSwimming;
    private boolean wasSwimming;
    private boolean isClimbing;
    private boolean isGliding;
    private boolean wasGliding;
    private double fallDistance;
    private boolean isTouchingWater;
    private int ticksSinceLastPositionPacketSent;
    private MutableVec3d velocity = new MutableVec3d(0, 0, 0);
    private Input movementInput = new Input();
    private int waitTicks = 0;
    private Queue<Runnable> taskQueue = new ConcurrentLinkedQueue<>();
    private static final CollisionBox STANDING_COLLISION_BOX = new CollisionBox(-0.3, 0.3, 0, 1.8, -0.3, 0.3);
    private static final CollisionBox SNEAKING_COLLISION_BOX = new CollisionBox(-0.3, 0.3, 0, 1.5, -0.3, 0.3);
    private LocalizedCollisionBox playerCollisionBox = new LocalizedCollisionBox(STANDING_COLLISION_BOX, 0, 0, 0);
    private float stepHeight = 0.6F;
    private boolean forceUpdateSupportingBlockPos = false;
    private Optional<BlockPos> supportingBlockPos = Optional.empty();

    @Override
    public Subscription subscribeEvents() {
        return EVENT_BUS.subscribe(
            SimpleEventBus.pair(ClientTickEvent.class, this::tick)
        );
    }

    @Override
    public Supplier<Boolean> shouldBeEnabled() {
        return () -> true;
    }

    @Override
    public synchronized void clientTickStarting() {
        this.taskQueue.clear();
        syncFromCache(false);
    }

    @Override
    public synchronized void clientTickStopped() {
        if (isSneaking) {
            sendClientPacketAsync(new ServerboundPlayerCommandPacket(CACHE.getPlayerCache().getEntityId(), PlayerState.STOP_SNEAKING));
        }
        if (isSprinting) {
            sendClientPacketAsync(new ServerboundPlayerCommandPacket(CACHE.getPlayerCache().getEntityId(), PlayerState.STOP_SPRINTING));
        }
        this.taskQueue.clear();
    }

    public synchronized void doRotate(float yaw, float pitch) {
        yaw = shortestRotation(yaw);
        pitch = MathHelper.clamp(pitch, -90.0F, 90.0F);
        pitch = ((int) pitch * 10.0f) / 10.0f; // always clamp pitch to 1 decimal place to avoid flagging for very small adjustments
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public float shortestRotation(float targetYaw) {
        float difference = targetYaw - this.yaw;
        if (difference > 180) difference -= 360;
        else if (difference < -180) difference += 360;
        return this.yaw + difference;
    }

    public synchronized void doMovementInput(boolean pressingForward,
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
    }

    public synchronized void doMovementInput(final Input input) {
        doMovementInput(input.pressingForward,
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

    public double getEyeY() {
        return playerCollisionBox.getMaxY() - 0.2;
    }

    // its important that we process certain packets in order. Grim tracks the order of "transactions"
    // i.e. if we get teleported back, we need to process it before we do the next ping packet. otherwise the transactions are out of order and we get into a flag failure loop
    private void processTaskQueue() {
        while (!taskQueue.isEmpty()) {
            taskQueue.poll().run();
        }
    }

    public void addTask(Runnable task) {
        if (!Proxy.getInstance().hasActivePlayer())
            taskQueue.add(task);
    }

    private synchronized void tick(final ClientTickEvent event) {
        processTaskQueue();
        if (!CACHE.getChunkCache().isChunkLoaded((int) x >> 4, (int) z >> 4)) return;
        if (waitTicks-- > 0) return;
        if (waitTicks < 0) waitTicks = 0;

        if (Math.abs(velocity.getX()) < 0.003) velocity.setX(0);
        if (Math.abs(velocity.getY()) < 0.003) velocity.setY(0);
        if (Math.abs(velocity.getZ()) < 0.003) velocity.setZ(0);

        updateMovementState();
        isSneaking = movementInput.sneaking;
        isSprinting = movementInput.sprinting;
        this.isTouchingWater = World.isTouchingWater(playerCollisionBox);
        this.movementInput.movementForward *= 0.98f;
        this.movementInput.movementSideways *= 0.98f;
        final MutableVec3d movementInputVec = new MutableVec3d(movementInput.movementSideways, 0, movementInput.movementForward);
        if (isTouchingWater && isSneaking && !isFlying) velocity.setY(velocity.getY() - 0.04f);
        travel(movementInputVec);

        if (CACHE.getPlayerCache().getThePlayer().isInVehicle()) {
            sendClientPacketAsync(new ServerboundMovePlayerRotPacket(false, this.yaw, this.pitch));
            sendClientPacketAsync(new ServerboundPlayerInputPacket(0.0f, 0.0f, false, false));
            // todo: handle vehicle travel movement
        } else {
            // send movement packets based on position
            if (wasSneaking != isSneaking) {
                if (isSneaking) {
                    sendClientPacketAsync(new ServerboundPlayerCommandPacket(CACHE.getPlayerCache().getEntityId(), PlayerState.START_SNEAKING));
                } else {
                    sendClientPacketAsync(new ServerboundPlayerCommandPacket(CACHE.getPlayerCache().getEntityId(), PlayerState.STOP_SNEAKING));
                }
            }
            if (lastSprinting != isSprinting) {
                if (isSprinting) {
                    sendClientPacketAsync(new ServerboundPlayerCommandPacket(CACHE.getPlayerCache().getEntityId(), PlayerState.START_SPRINTING));
                } else {
                    sendClientPacketAsync(new ServerboundPlayerCommandPacket(CACHE.getPlayerCache().getEntityId(), PlayerState.STOP_SPRINTING));
                }
            }
            double xDelta = this.x - this.lastX;
            double yDelta = this.y - this.lastY;
            double zDelta = this.z - this.lastZ;
            double pitchDelta = this.pitch - this.lastPitch;
            double yawDelta = this.yaw - this.lastYaw;
            ++this.ticksSinceLastPositionPacketSent;
            boolean shouldUpdatePos = MathHelper.squaredMagnitude(xDelta, yDelta, zDelta) > MathHelper.square(2.0E-4) || this.ticksSinceLastPositionPacketSent >= 20;
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

    public synchronized void handlePlayerPosRotate(final int teleportId) {
        syncFromCache(false);
        addTask(() -> {
            CLIENT_LOG.debug("Server teleport to: {}, {}, {}", this.x, this.y, this.z);
            Proxy.getInstance().getClient().send(new ServerboundAcceptTeleportationPacket(teleportId));
            Proxy.getInstance().getClient().send(new ServerboundMovePlayerPosRotPacket(false, this.x, this.y, this.z, this.yaw, this.pitch));
        });
    }

    public synchronized void handleRespawn() {
        syncFromCache(true);
    }

    private void travel(MutableVec3d movementInputVec) {
        if (isTouchingWater) {
            boolean falling = velocity.getY() <= 0.0;
            float waterSpeed = 0.02f;
            updateVelocity(waterSpeed, movementInputVec);
            move();
            velocity.setX(velocity.getX() * 0.8f);
            velocity.setY(velocity.getY() * 0.8f);
            velocity.setZ(velocity.getZ() * 0.8f);
            double d;
            if (falling && Math.abs(velocity.getY() - 0.005) >= 0.003 && Math.abs(velocity.getY() - gravity / 16.0) < 0.003) {
                d = -0.003;
            } else {
                d = velocity.getY() - gravity / 16.0;
            }
            velocity.setY(d);
            // todo: additional logic for sprinting, swimming, and collisions affecting velocity
            //  this seems sufficient for falling into water and doing basic walking though

            // todo: lava movement
        } else {
            final Block floorBlock = World.getBlockAtBlockPos(getVelocityAffectingPos());
            float floorSlipperiness = BLOCK_DATA_MANAGER.getBlockSlipperiness(floorBlock);
            float friction = this.onGround ? floorSlipperiness * 0.91f : 0.91F;
            applyMovementInput(movementInputVec, floorSlipperiness);
            if (!isFlying) velocity.setY(velocity.getY() - gravity);
            velocity.setX(velocity.getX() * (double) friction);
            velocity.setY(velocity.getY() * 0.9800000190734863);
            velocity.setZ(velocity.getZ() * (double) friction);
        }
    }

    private void move() {
        // todo: movement slowing from certain blocks like cobweb

        // in-place velocity update
        adjustMovementForSneaking(velocity);

        List<LocalizedCollisionBox> blockCollisionBoxes = World.getSolidBlockCollisionBoxes(playerCollisionBox.stretch(velocity.getX(),
                                                                                                                       velocity.getY(),
                                                                                                                       velocity.getZ()));
        MutableVec3d adjustedMovement = adjustMovementForCollisions(velocity, playerCollisionBox, blockCollisionBoxes);
        boolean isYAdjusted = velocity.getY() != adjustedMovement.getY();
        boolean isXAdjusted = velocity.getX() != adjustedMovement.getX();
        boolean isZAdjusted = velocity.getZ() != adjustedMovement.getZ();
        if (onGround && (isXAdjusted || isZAdjusted)) {
            // attempt to step up in xz direction block
            MutableVec3d stepUpAdjustedVec = adjustMovementForCollisions(new MutableVec3d(velocity.getX(), stepHeight, velocity.getZ()),
                                                                         playerCollisionBox,
                                                                         blockCollisionBoxes);
            MutableVec3d stepUpWithMoveXZAdjustedVec = adjustMovementForCollisions(new MutableVec3d(0.0, stepHeight, 0.0),
                                                                                   playerCollisionBox.stretch(velocity.getX(), 0.0, velocity.getZ()),
                                                                                   blockCollisionBoxes);
            if (stepUpWithMoveXZAdjustedVec.getY() < this.stepHeight) {
                MutableVec3d stepUpAndMoveVec = adjustMovementForCollisions(new MutableVec3d(velocity.getX(), 0.0, velocity.getZ()),
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
                stepUpAdjustedVec.add(adjustMovementForCollisions(new MutableVec3d(0.0, -stepUpAdjustedVec.getY() + velocity.getY(), 0.0),
                                                                  playerCollisionBox.move(stepUpAdjustedVec.getX(),
                                                                                          stepUpAdjustedVec.getY(),
                                                                                          stepUpAdjustedVec.getZ()),
                                                                  blockCollisionBoxes));
                adjustedMovement = stepUpAdjustedVec;
            }
        }
        this.setOnGround(isYAdjusted && velocity.getY() < 0.0, adjustedMovement);

        final LocalizedCollisionBox movedPlayerCollisionBox = playerCollisionBox.move(adjustedMovement.getX(),
                                                                                            adjustedMovement.getY(),
                                                                                            adjustedMovement.getZ());

        if (isXAdjusted) {
            velocity.setX(0.0);
        }
        if (isYAdjusted) {
            velocity.setY(0.0);
        }
        if (isZAdjusted) {
            velocity.setZ(0.0);
        }

        // todo: apply block falling effects like bouncing off slime blocks

        this.x = ((movedPlayerCollisionBox.getMinX() + movedPlayerCollisionBox.getMaxX()) / 2.0);
        this.y = movedPlayerCollisionBox.getMinY();
        this.z = ((movedPlayerCollisionBox.getMinZ() + movedPlayerCollisionBox.getMaxZ()) / 2.0);
        syncPlayerCollisionBox();
        float velocityMultiplier = this.getBlockSpeedFactor();
        velocity.multiply(velocityMultiplier, 1.0, velocityMultiplier);
    }

    private void setOnGround(boolean onGround, MutableVec3d movement) {
        this.onGround = onGround;
        updateSupportingBlockPos(onGround, movement);
    }

    private void updateSupportingBlockPos(boolean onGround, MutableVec3d movement) {
        if (onGround) {
            LocalizedCollisionBox box = this.playerCollisionBox;
            LocalizedCollisionBox box2 = new LocalizedCollisionBox(box.getMinX(), box.getMaxX(), box.getMinY() - 1.0E-6, box.getMinY(), box.getMinZ(), box.getMaxZ(), x, y, z);
            Optional<BlockPos> optional = World.findSupportingBlockPos(box2);
            if (optional.isPresent() || this.forceUpdateSupportingBlockPos) {
                this.supportingBlockPos = optional;
            } else if (movement != null) {
                LocalizedCollisionBox box3 = new LocalizedCollisionBox(box2.getMinX() - movement.getX(),
                                                                       box2.getMaxX() - movement.getX(),
                                                                       box2.getMinY(),
                                                                       box2.getMaxY(),
                                                                       box2.getMinZ() - movement.getZ(),
                                                                       box2.getMaxZ() - movement.getZ(),
                                                                       x,
                                                                       y,
                                                                       z);
                optional = World.findSupportingBlockPos(box3);
                this.supportingBlockPos = optional;
            }

            this.forceUpdateSupportingBlockPos = optional.isEmpty();
        } else {
            this.forceUpdateSupportingBlockPos = false;
            if (this.supportingBlockPos.isPresent()) {
                this.supportingBlockPos = Optional.empty();
            }
        }
    }

    private BlockPos getVelocityAffectingPos() {
        return this.getPosWithYOffset(0.500001F);
    }

    private BlockPos getPosWithYOffset(float offset) {
        if (this.supportingBlockPos.isPresent()) {
            BlockPos blockPos = this.supportingBlockPos.get();
            if (!(offset > 1.0E-5F)) {
                return blockPos;
            } else {
                // todo: fences and walls calcs
//                BlockState blockState = World.getBlockState(blockPos);
                return
                    (!((double)offset <= 0.5)
//                        || !blockState.isIn(BlockTags.FENCES)
                    )
//                    && !blockState.isIn(BlockTags.WALLS)
//                    && !(blockState.getBlock() instanceof FenceGateBlock)
                    ? new BlockPos(blockPos.getX(), MathHelper.floorToInt(this.y - (double)offset), blockPos.getZ())
                    : blockPos;
            }
        } else {
            int i = MathHelper.floorToInt(this.x);
            int j = MathHelper.floorToInt(this.y - (double)offset);
            int k = MathHelper.floorToInt(this.z);
            return new BlockPos(i, j, k);
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
            return new MutableVec3d(movementInput.getX() * (double)yawCos - movementInput.getZ() * (double)yawSin,
                                    movementInput.getY(),
                                    movementInput.getZ() * (double)yawCos + movementInput.getX() * (double)yawSin);
        }
    }

    private float getMovementSpeed(float slipperiness) {
        return this.onGround ? getSpeed() * (0.21600002f / (slipperiness * slipperiness * slipperiness)) : 0.02f;
    }

    private float getBlockSpeedFactor() {
        if (this.isGliding || this.isFlying) return 1.0f;
        Block inBlock = World.getBlockAtBlockPos(Pathing.getCurrentPlayerPos().toBlockPos());
        float inBlockSpeedFactor = getBlockSpeedFactor(inBlock);
        if (inBlockSpeedFactor != 1.0f || World.isWater(inBlock)) return inBlockSpeedFactor;
        Block underPlayer = World.getBlockAtBlockPos(Pathing.getCurrentPlayerPos().toBlockPos().addY(-1));
        return getBlockSpeedFactor(underPlayer);
    }

    private float getBlockSpeedFactor(Block block) {
        if (block.name().equals("honey_block")) return 0.4f;
        if (block.name().equals("soul_sand")) {
            ItemStack bootsItemStack = CACHE.getPlayerCache().getThePlayer().getEquipment().get(EquipmentSlot.BOOTS);
            if (bootsItemStack != null) {
                // todo: check if soul speed enchantment is on boots
                // todo: create enchantment parser helper class
//                if (bootsItemStack.getNbt().containsKey(EnchantmentTypes.SOUL_SPEED)) return 1.0f;
            }
            return 0.4f;
        }
        return 1.0f;
    }

    public void handleSetMotion(final double motionX, final double motionY, final double motionZ) {
        addTask(() -> {
            this.velocity.setX(motionX);
            this.velocity.setY(motionY);
            this.velocity.setZ(motionZ);
        });
    }

    public void handleExplosion(final ClientboundExplodePacket packet) {
        addTask(() -> {
            this.velocity.add(packet.getPushX(), packet.getPushY(), packet.getPushZ());
        });
    }

    private void syncFromCache(boolean full) {
        this.x = CACHE.getPlayerCache().getX();
        this.lastX = this.x;
        this.y = CACHE.getPlayerCache().getY();
        this.lastY = this.y;
        this.z = CACHE.getPlayerCache().getZ();
        this.lastZ = this.z;
        this.yaw = CACHE.getPlayerCache().getYaw();
        this.lastYaw = this.yaw;
        this.pitch = CACHE.getPlayerCache().getPitch();
        this.lastPitch = this.pitch;
        this.onGround = true; // todo: cache
        this.lastOnGround = true;
        this.velocity = new MutableVec3d(0, 0, 0);
        this.supportingBlockPos = Optional.empty();
        this.forceUpdateSupportingBlockPos = false;
        this.ticksSinceLastPositionPacketSent = 0;
        if (full) {
            this.isSneaking = this.wasSneaking = false;
            this.isSprinting = this.lastSprinting = false;
        } else {
            this.isSneaking = CACHE.getPlayerCache().isSneaking();
            this.isSprinting = CACHE.getPlayerCache().isSprinting();
        }
        syncPlayerCollisionBox();
    }

    private void updateMovementState() {
        float moveForward = 0.0f;
        float moveStrafe = 0.0f;
        if (movementInput.pressingForward) moveForward++;
        if (movementInput.pressingBack) moveForward--;
        if (movementInput.pressingLeft) moveStrafe++;
        if (movementInput.pressingRight) moveStrafe--;
        if (movementInput.sneaking) {
            moveStrafe *= 0.3f;
            moveForward *= 0.3f;
        }
        movementInput.movementSideways = moveStrafe;
        movementInput.movementForward = moveForward;
        if (movementInput.sprinting) {
            // cannot sprint any direction except forwards
            if (moveForward <= 0.0f || movementInput.sneaking)
                movementInput.sprinting = false;
        }
    }

    private float getSpeed() {
        return CACHE.getPlayerCache().getThePlayer().getSpeed();
    }
}
