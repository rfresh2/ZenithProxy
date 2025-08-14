package com.zenith.feature.player;

import com.google.common.collect.Lists;
import com.zenith.cache.data.entity.EntityLiving;
import com.zenith.cache.data.entity.EntityPlayer;
import com.zenith.event.client.ClientBotTick;
import com.zenith.mc.block.*;
import com.zenith.mc.block.properties.api.BlockStateProperties;
import com.zenith.mc.dimension.DimensionRegistry;
import com.zenith.mc.entity.EntityData;
import com.zenith.module.api.ModuleUtils;
import com.zenith.util.math.MathHelper;
import com.zenith.util.math.MutableVec3d;
import it.unimi.dsi.fastutil.doubles.DoubleArraySet;
import it.unimi.dsi.fastutil.doubles.DoubleArrays;
import it.unimi.dsi.fastutil.doubles.DoubleSet;
import lombok.Getter;
import org.geysermc.mcprotocollib.protocol.data.game.PlayerListEntry;
import org.geysermc.mcprotocollib.protocol.data.game.entity.Effect;
import org.geysermc.mcprotocollib.protocol.data.game.entity.attribute.Attribute;
import org.geysermc.mcprotocollib.protocol.data.game.entity.attribute.AttributeModifier;
import org.geysermc.mcprotocollib.protocol.data.game.entity.attribute.AttributeType;
import org.geysermc.mcprotocollib.protocol.data.game.entity.attribute.ModifierOperation;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.ByteEntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.GameMode;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.Hand;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.PlayerState;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.PositionElement;
import org.geysermc.mcprotocollib.protocol.data.game.entity.type.EntityType;
import org.geysermc.mcprotocollib.protocol.data.game.scoreboard.CollisionRule;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundExplodePacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundClientTickEndPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.inventory.ServerboundContainerClosePacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.level.ServerboundAcceptTeleportationPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.level.ServerboundPlayerInputPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static com.github.rfresh2.EventConsumer.of;
import static com.zenith.Globals.*;

public final class Bot extends ModuleUtils {
    @Getter private double x;
    @Getter private double y;
    @Getter private double z;
    private double lastX;
    private double lastY;
    private double lastZ;
    @Getter private float yaw;
    @Getter private float pitch;
    private float requestedYaw;
    private float requestedPitch;
    private float lastYaw;
    private float lastPitch;
    @Getter private boolean onGround;
    private boolean lastOnGround;
    @Getter private boolean isSneaking;
    private boolean wasSneaking;
    private boolean isSprinting;
    private boolean lastSprinting;
    private boolean isFlying;
    private boolean isFallFlying;
    private boolean isGliding;
    private double fallDistance;
    @Getter private boolean isTouchingWater;
    private boolean isTouchingLava;
    private double waterHeight;
    private double lavaHeight;
    private int ticksSinceLastPositionPacketSent;
    private final MutableVec3d stuckSpeedMultiplier = new MutableVec3d(0, 0, 0);
    @Getter private final MutableVec3d velocity = new MutableVec3d(0, 0, 0);
    private boolean wasLeftClicking = false;
    private final Input movementInput = new Input();
    private InputRequestFuture inputRequestFuture = InputRequestFuture.rejected;
    private Input lastSentMovementInput = new Input(movementInput);
    public static final CollisionBox STANDING_COLLISION_BOX = new CollisionBox(-0.3, 0.3, 0, 1.8, -0.3, 0.3);
    public static final CollisionBox SNEAKING_COLLISION_BOX = new CollisionBox(-0.3, 0.3, 0, 1.5, -0.3, 0.3);
    @Getter private LocalizedCollisionBox playerCollisionBox = new LocalizedCollisionBox(STANDING_COLLISION_BOX, 0, 0, 0);
    private double gravity = 0.08;
    private float stepHeight = 0.6f;
    private float waterMovementEfficiency = 0.0f;
    private float movementEfficiency = 0.0f;
    private float speed = 0.10000000149011612f;
    private float sneakSpeed = 0.3f;
    private float jumpStrength = 0.42f;
    private boolean onGroundNoBlocks = false;
    private Optional<BlockPos> supportingBlockPos = Optional.empty();
    private int jumpingCooldown;
    @Getter private boolean horizontalCollision = false;
    private boolean horizontalCollisionMinor = false;
    @Getter private boolean verticalCollision = false;
    @Getter private final PlayerInteractionManager interactions = new PlayerInteractionManager();
    // todo: local attribute cache
    private static final Attribute DEFAULT_SPEED_ATTRIBUTE = new Attribute(AttributeType.Builtin.MOVEMENT_SPEED, 0.10000000149011612f);
    private Attribute speedAttribute = DEFAULT_SPEED_ATTRIBUTE;

    public Bot() {
        EVENT_BUS.subscribe(
            this,
            // we want this to be one of the last thing that happens in the tick
            // to allow other modules to update the player's input
            // other modules can also do actions after this tick by setting an even lower priority
            of(ClientBotTick.class, -20000, this::tick),
            of(ClientBotTick.class, -30000, this::postTick),
            of(ClientBotTick.Starting.class, this::handleClientTickStarting),
            of(ClientBotTick.Stopped.class, this::handleClientTickStopped)
        );
    }

    private void handleClientTickStarting(final ClientBotTick.Starting event) {
        syncFromCache(false);
    }

    private void handleClientTickStopped(final ClientBotTick.Stopped event) {
        if (isSneaking) {
            sendClientPacketAsync(new ServerboundPlayerCommandPacket(CACHE.getPlayerCache().getEntityId(), PlayerState.STOP_SNEAKING));
        }
        if (isSprinting) {
            sendClientPacketAsync(new ServerboundPlayerCommandPacket(CACHE.getPlayerCache().getEntityId(), PlayerState.STOP_SPRINTING));
        }
    }

    void requestMovement(final InputRequest request, final InputRequestFuture inputRequestFuture) {
        var reqInput = request.input();
        if (reqInput != null) {
            movementInput.apply(reqInput);
        }
        var reqYaw = request.yaw();
        var reqPitch = request.pitch();
        if (reqYaw != null) {
            float difference = reqYaw - this.yaw;
            if (difference > 180) difference -= 360;
            else if (difference < -180) difference += 360;
            this.requestedYaw = this.yaw + difference;
        }
        if (reqPitch != null) {
            this.requestedPitch = MathHelper.clamp(reqPitch, -90f, 90f);
            this.requestedPitch = ((int) (this.requestedPitch * 10.0f)) / 10.0f; // always clamp pitch to 1 decimal place to avoid flagging for very small adjustments
        }
        this.inputRequestFuture = inputRequestFuture;
    }

    private void interactionTick() {
        try {
            if (movementInput.clickRequiresRotation) {
                if (!MathHelper.isYawInRange(requestedYaw, yaw, 0.1f) || !MathHelper.isPitchInRange(requestedPitch, pitch, 0.1f)) {
                    interactions.stopDestroyBlock();
                    wasLeftClicking = false;
                    return;
                }
            }
            if (movementInput.isLeftClick()) {
                var raycast = movementInput.clickTarget.apply(getBlockReachDistance(), getEntityInteractDistance());
                if (raycast.hit() && raycast.isBlock()) {
                    int blockX = raycast.block().x();
                    int blockY = raycast.block().y();
                    int blockZ = raycast.block().z();
                    if (!wasLeftClicking && !interactions.isDestroying()) {
                        debug("Starting destroy block at: [{}, {}, {}]", blockX, blockY, blockZ);
                        interactions.startDestroyBlock(
                            MathHelper.floorI(blockX),
                            MathHelper.floorI(blockY),
                            MathHelper.floorI(blockZ),
                            raycast.block().direction());
                        sendClientPacketAsync(new ServerboundSwingPacket(Hand.MAIN_HAND));
                        wasLeftClicking = true;
                        inputRequestFuture.setClickResult(ClickResult.LeftClickResult.startDestroyBlock(blockX, blockY, blockZ, raycast.block().block()));
                        return;
                    } else {
                        if (interactions.continueDestroyBlock(
                            MathHelper.floorI(blockX),
                            MathHelper.floorI(blockY),
                            MathHelper.floorI(blockZ),
                            raycast.block().direction())) {
                            sendClientPacketAsync(new ServerboundSwingPacket(Hand.MAIN_HAND));
                            wasLeftClicking = true;
                        } else {
                            // we could not continue breaking this block for some reason
                            wasLeftClicking = false;
                            interactions.stopDestroyBlock();
                            sendClientPacketAsync(new ServerboundSwingPacket(Hand.MAIN_HAND));
                        }
                        inputRequestFuture.setClickResult(ClickResult.LeftClickResult.continueDestroyBlock(blockX, blockY, blockZ, raycast.block().block()));
                        return;
                    }
                } else if (raycast.hit() && raycast.isEntity() && raycast.entity().entityData().attackable()) {
                    debug("Click attacking entity: {} [{}, {}, {}]", raycast.entity().entity().getEntityType(), raycast.entity().entity().getX(), raycast.entity().entity().getY(), raycast.entity().entity().getZ());
                    interactions.attackEntity(raycast.entity());
                    sendClientPacketAsync(new ServerboundSwingPacket(Hand.MAIN_HAND));
                    inputRequestFuture.setClickResult(ClickResult.LeftClickResult.attackEntity(raycast.entity().entity()));
                } else {
                    debug("Left click swing");
                    sendClientPacketAsync(new ServerboundSwingPacket(Hand.MAIN_HAND));
                    inputRequestFuture.setClickResult(ClickResult.LeftClickResult.swing());
                }
            } else if (movementInput.isRightClick()) {
                var raycast = movementInput.clickTarget.apply(getBlockReachDistance(), getEntityInteractDistance());
                Hand hand = movementInput.hand;
                if (raycast.hit() && raycast.isBlock()) {
                    debug("Right click {} block at: [{}, {}, {}]", hand, raycast.block().x(), raycast.block().y(), raycast.block().z());
                    interactions.useItemOn(hand, raycast.block());
                    sendClientPacketAsync(new ServerboundSwingPacket(hand));
                    inputRequestFuture.setClickResult(ClickResult.RightClickResult.useItemOnBlock(raycast.block().x(), raycast.block().y(), raycast.block().z(), raycast.block().block()));
                } else if (raycast.hit() && raycast.isEntity()) {
                    debug("Right click {} entity: {} [{}, {}, {}]", hand, raycast.entity().entity().getEntityType(), raycast.entity().entity().getX(), raycast.entity().entity().getY(), raycast.entity().entity().getZ());
                    interactions.interactAt(hand, raycast.entity());
                    interactions.interact(hand, raycast.entity());
                    sendClientPacketAsync(new ServerboundSwingPacket(hand));
                    inputRequestFuture.setClickResult(ClickResult.RightClickResult.useItemOnEntity(raycast.entity().entity()));
                } else {
                    debug("Right click {} use item", hand);
                    interactions.useItem(hand);
                    sendClientPacketAsync(new ServerboundSwingPacket(hand));
                    inputRequestFuture.setClickResult(ClickResult.RightClickResult.useItem());
                }
            }
            // todo: track ongoing right click item consumes
            //  and if one is cancelled, do interactions.releaseUsingItem()
            interactions.stopDestroyBlock();
            wasLeftClicking = false;
        } catch (final Exception e) {
            CLIENT_LOG.error("Error during interaction tick", e);
        }
    }

    private void tick(final ClientBotTick event) {
        if (this.jumpingCooldown > 0) --this.jumpingCooldown;
        if (!CACHE.getChunkCache().isChunkLoaded((int) x >> 4, (int) z >> 4)) return;

        if (resyncTeleport()) return;

        if (CACHE.getPlayerCache().getThePlayer().isSleeping()) {
            debug("Player sleeping, sending leave bed packet");
            sendClientPacketAwait(new ServerboundPlayerCommandPacket(CACHE.getPlayerCache().getEntityId(), PlayerState.LEAVE_BED));
            return;
        }

        // stop movement and interaction inputs while a container is open
        if (handleOpenContainer()) {
            movementInput.reset();
            this.inputRequestFuture.complete(false);
            this.inputRequestFuture = InputRequestFuture.rejected;
        } else {
            interactionTick();
            this.yaw = this.requestedYaw;
            this.pitch = this.requestedPitch;
        }

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

        isSneaking = !isFlying
            && !CACHE.getPlayerCache().getThePlayer().isInVehicle()
            && canPlayerFitWithinBlocksAndEntitiesWhen(SNEAKING_COLLISION_BOX)
            && (movementInput.sneaking || !CACHE.getPlayerCache().getThePlayer().isSleeping() && !canPlayerFitWithinBlocksAndEntitiesWhen(STANDING_COLLISION_BOX));
        isSprinting = movementInput.sprinting
            && isOnGround()
            && !isTouchingWater
            && CACHE.getPlayerCache().getThePlayer().getFood() > 6
            && !(horizontalCollision && !horizontalCollisionMinor);
        // cannot start sprinting while we have blindness
        if (isSprinting && !lastSprinting && CACHE.getPlayerCache().getThePlayer().getPotionEffectMap().containsKey(Effect.BLINDNESS)) {
            isSprinting = false;
        }
        if (isSprinting != lastSprinting) applySprintingSpeedAttributeModifier();

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
            // resync position from any vehicle movements
            this.x = this.lastX = CACHE.getPlayerCache().getX();
            this.y = this.lastY = CACHE.getPlayerCache().getY();
            this.z = this.lastZ = CACHE.getPlayerCache().getZ();
            this.isSneaking = this.wasSneaking = false;
            this.isSprinting = this.lastSprinting = false;
            syncPlayerCollisionBox();
            updateAttributes();

            sendClientPacketsAsync(
                new ServerboundMovePlayerRotPacket(false, horizontalCollision, this.yaw, this.pitch),
                // todo: pass in strafe/forward movement inputs from `getMovementInputVec()`
                new ServerboundPlayerInputPacket(false, false, false, false, movementInput.jumping, movementInput.sneaking, false)
            );
            lastSentMovementInput = Input.builder().build();
            // todo: handle vehicle move packets
            //  need to determine if vehicle is a controllable type
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
            if (!lastSentMovementInput.equals(movementInput)) {
                sendClientPacketAsync(new ServerboundPlayerInputPacket(
                    movementInput.pressingForward,
                    movementInput.pressingBack,
                    movementInput.pressingLeft,
                    movementInput.pressingRight,
                    movementInput.jumping,
                    movementInput.sneaking,
                    movementInput.sprinting
                ));
                lastSentMovementInput = new Input(movementInput);
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
                sendClientPacketAsync(new ServerboundMovePlayerPosRotPacket(this.onGround, horizontalCollision, this.x, this.y, this.z, this.yaw, this.pitch));
            } else if (shouldUpdatePos) {
                sendClientPacketAsync(new ServerboundMovePlayerPosPacket(this.onGround, horizontalCollision, this.x, this.y, this.z));
            } else if (shouldUpdateRot) {
                sendClientPacketAsync(new ServerboundMovePlayerRotPacket(this.onGround, horizontalCollision, this.yaw, this.pitch));
            } else if (this.lastOnGround != this.onGround) {
                sendClientPacketAsync(new ServerboundMovePlayerStatusOnlyPacket(this.onGround, horizontalCollision));
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
        tickEntityPushing();
        this.movementInput.reset();
    }

    // returns true if a container is open
    private boolean handleOpenContainer() {
        boolean isContainerOpen = CONFIG.client.inventory.ncpStrict
            ? CACHE.getPlayerCache().getInventoryCache().getActiveContainerId() != -1
            : CACHE.getPlayerCache().getInventoryCache().getOpenContainerId() != 0;
        if (isContainerOpen) {
            int containerId = CACHE.getPlayerCache().getInventoryCache().getOpenContainerId();
            if (INVENTORY.hasActiveRequest()) {
                if (containerId != INVENTORY.requestedContainerId()) {
                    debug("Closing open container: {} for inventory request: {}", containerId, INVENTORY.requestedContainerId());
                    sendClientPacketAwait(new ServerboundContainerClosePacket(
                        CACHE.getPlayerCache().getInventoryCache().getOpenContainerId()
                    ));
                    isContainerOpen = false;
                }
            } else if (CONFIG.client.inventory.autoCloseOpenContainers) {
                long containerOpenedAt = CACHE.getPlayerCache().getInventoryCache().getContainerOpenedAt();
                long lastContainerClick = CACHE.getPlayerCache().getInventoryCache().getLastContainerClick();
                if (System.currentTimeMillis() - Math.max(containerOpenedAt, lastContainerClick) >= TimeUnit.SECONDS.toMillis(CONFIG.client.inventory.autoCloseOpenContainerAfterSeconds)) {
                    debug("Auto closing open container: {}", containerId);
                    sendClientPacketAwait(new ServerboundContainerClosePacket(
                        CACHE.getPlayerCache().getInventoryCache().getOpenContainerId()
                    ));
                    isContainerOpen = false;
                }
            }
        }
        return isContainerOpen;
    }

    private void postTick(ClientBotTick event) {
        this.inputRequestFuture.notifyListeners();
        this.inputRequestFuture = InputRequestFuture.rejected;
        sendClientPacket(ServerboundClientTickEndPacket.INSTANCE);
    }

    private static final String SPRINT_ATTRIBUTE_ID = "minecraft:sprinting";
    private static final AttributeModifier SPRINT_ATTRIBUTE_MODIFIER = new AttributeModifier(
        SPRINT_ATTRIBUTE_ID,
        0.3f,
        ModifierOperation.ADD_MULTIPLIED_TOTAL
    );

    // the server will send us this attribute
    // but not in time for us to apply it on the first tick
    // the vanilla client also applies this attribute locally ahead of time
    private void applySprintingSpeedAttributeModifier() {
        getLocalAttributeValue(speedAttribute, 0.10000000149011612f);
        if (speedAttribute == null) return;
        List<AttributeModifier> modifiers = speedAttribute.getModifiers();
        if (isSprinting) {
            for (AttributeModifier modifier : modifiers) {
                if (SPRINT_ATTRIBUTE_ID.equals(modifier.getId())) {
                    return;
                }
            }
            modifiers.add(SPRINT_ATTRIBUTE_MODIFIER);
            this.speed = getLocalAttributeValue(speedAttribute, 0.10000000149011612f);
        } else {
            for (AttributeModifier modifier : modifiers) {
                if (SPRINT_ATTRIBUTE_ID.equals(modifier.getId())) {
                    modifiers.remove(modifier);
                    this.speed = getLocalAttributeValue(speedAttribute, 0.10000000149011612f);
                    return;
                }
            }
        }
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
        float jumpPower = getJumpPower();
        if (!(jumpPower <= 1.0E-5f)) {
            this.velocity.setY(Math.max(jumpPower, velocity.getY()));
            if (this.isSprinting) {
                float sprintAngle = yaw * (float) (Math.PI / 180.0);
                this.velocity.setX(this.velocity.getX() - (Math.sin(sprintAngle) * 0.2F));
                this.velocity.setZ(this.velocity.getZ() + (Math.cos(sprintAngle) * 0.2F));
            }
        }
    }

    private float getJumpPower() {
        float blockJumpFactor = 1f;
        Block inBlock = World.getBlock(MathHelper.floorI(x), MathHelper.floorI(y), MathHelper.floorI(z));
        if (inBlock == BlockRegistry.HONEY_BLOCK)
            blockJumpFactor = 0.5f;
        else if (supportingBlockPos.isPresent()) {
            Block supportingBlock = World.getBlock(supportingBlockPos.get());
            if (supportingBlock == BlockRegistry.HONEY_BLOCK)
                blockJumpFactor = 0.5f;
        }
        float jumpBoostPower = 0f;
        if (CACHE.getPlayerCache().getThePlayer().getPotionEffectMap().containsKey(Effect.JUMP_BOOST)) {
            jumpBoostPower = 0.1f * (CACHE.getPlayerCache().getThePlayer().getPotionEffectMap().get(Effect.JUMP_BOOST).getAmplifier() + 1.0f);
        }

        return jumpStrength * blockJumpFactor + jumpBoostPower;
    }

    public void handlePlayerPosRotate(final int teleportId) {
        syncFromCache(true);
        CLIENT_LOG.info("Server teleport {} to: {}, {}, {}d", teleportId, this.x, this.y, this.z);
        sendClientPacketAwait(new ServerboundAcceptTeleportationPacket(teleportId));
        sendClientPacketAwait(new ServerboundMovePlayerPosRotPacket(false, false, this.x, this.y, this.z, this.yaw, this.pitch));
        CLIENT_LOG.debug("Accepted teleport: {}", teleportId);
    }

    public void handlePlayerRotate() {
        syncFromCache(false);
    }

    public void handleRespawn() {
        syncFromCache(true);
    }

    private void travel(MutableVec3d movementInputVec) {
        if (isTouchingWater || isTouchingLava) {
            travelInFluid(movementInputVec);
        } else if (isFallFlying) {
            travelFallFlying(movementInputVec);
        } else {
            travelInAir(movementInputVec);
        }
    }

    private void travelInFluid(MutableVec3d movementInputVec) {
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
            if (horizontalCollision && onClimbable()) {
                velocity.setY(0.2);
            }
            velocity.multiply(waterSlowdown, 0.8f, waterSlowdown);
            double fluidFallingAdjustedY;
            if (falling && Math.abs(velocity.getY() - 0.005) >= 0.003 && Math.abs(velocity.getY() - gravity / 16.0) < 0.003) {
                fluidFallingAdjustedY = -0.003;
            } else {
                fluidFallingAdjustedY = velocity.getY() - gravity / 16.0;
            }
            velocity.setY(fluidFallingAdjustedY);
        } else { // lava
            updateVelocity(0.02f, movementInputVec);
            move();
            if (lavaHeight <= 0.4) {
                velocity.multiply(0.5, 0.8, 0.5);
                double fluidFallingAdjustedY;
                boolean falling = velocity.getY() <= 0.0;
                if (falling && Math.abs(velocity.getY() - 0.005) >= 0.003 && Math.abs(velocity.getY() - gravity / 16.0) < 0.003) {
                    fluidFallingAdjustedY = -0.003;
                } else {
                    fluidFallingAdjustedY = velocity.getY() - gravity / 16.0;
                }
                velocity.setY(fluidFallingAdjustedY);
            } else {
                velocity.multiply(0.5);
            }
            if (gravity != 0.0) {
                velocity.add(0, -gravity / 4.0, 0);
            }
        }
        // todo: autojump when near shore block. need more checks for water level collisions
//            if (horizontalCollision && World.isFree(playerCollisionBox.move(velocity.getX(), velocity.getY() + 0.6 - getY() + beforeMoveY, velocity.getZ()))) {
//                velocity.setY(0.3);
//            }
    }

    private void travelFallFlying(MutableVec3d movementInputVec) {
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
    }

    private void travelInAir(MutableVec3d movementInputVec) {
        final Block floorBlock = World.getBlock(getVelocityAffectingPos());
        float floorSlipperiness = BLOCK_DATA.getBlockSlipperiness(floorBlock);
        float friction = this.onGround ? floorSlipperiness * 0.91f : 0.91F;
        applyMovementInput(movementInputVec, floorSlipperiness);
        if (!isFlying) velocity.setY(velocity.getY() - gravity);
        velocity.multiply(friction, 0.9800000190734863, friction);
    }

    private double[] collectCandidateStepUpHeights(LocalizedCollisionBox playerCB, List<LocalizedCollisionBox> colliders, double deltaY, double maxUpStep) {
        DoubleSet doubleSet = new DoubleArraySet(4);
        for (var cb : colliders) {
            double minYDelta = cb.minY() - playerCB.minY();
            if (!(minYDelta < 0) && minYDelta != deltaY) {
                if (minYDelta > maxUpStep) {
                    continue;
                }
                doubleSet.add(minYDelta);
            }
            double maxYDelta = cb.maxY() - playerCB.minY();
            if (!(maxYDelta < 0) && maxYDelta != deltaY) {
                if (maxYDelta > maxUpStep) {
                    continue;
                }
                doubleSet.add(maxYDelta);
            }
        }
        double[] doubleArray = doubleSet.toDoubleArray();
        DoubleArrays.unstableSort(doubleArray);
        return doubleArray;
    }

    private MutableVec3d collide(MutableVec3d movement) {
        List<LocalizedCollisionBox> blockCollisionBoxes = World.getIntersectingCollisionBoxes(
            playerCollisionBox.stretch(movement.getX(), movement.getY(), movement.getZ()));
        MutableVec3d adjustedMovement = collidePlayerBoundingBox(movement, playerCollisionBox, blockCollisionBoxes);
        boolean isYAdjusted = movement.getY() != adjustedMovement.getY();
        boolean isXAdjusted = movement.getX() != adjustedMovement.getX();
        boolean isZAdjusted = movement.getZ() != adjustedMovement.getZ();
        boolean isYAdjustedAndPrevFalling = isYAdjusted && movement.getY() < 0;
        if (stepHeight > 0 && ((isYAdjustedAndPrevFalling) || onGround) && (isXAdjusted || isZAdjusted)) {
            LocalizedCollisionBox playerCB = isYAdjustedAndPrevFalling ? playerCollisionBox.move(0, adjustedMovement.getY(), 0) : playerCollisionBox;
            LocalizedCollisionBox stepUpXZCb = playerCB.stretch(movement.getX(), stepHeight, movement.getZ());
            if (!isYAdjustedAndPrevFalling) {
                stepUpXZCb = stepUpXZCb.stretch(0, -1.0E-5, 0);
            }

            blockCollisionBoxes.addAll(World.getIntersectingCollisionBoxes(stepUpXZCb));
            double[] stepUpHeights = collectCandidateStepUpHeights(playerCB, blockCollisionBoxes, adjustedMovement.getY(), stepHeight);

            for (double stepUpHeight : stepUpHeights) {
                var stepCollision = collidePlayerBoundingBox(new MutableVec3d(movement.getX(), stepUpHeight, movement.getZ()), playerCB, blockCollisionBoxes);
                if (stepCollision.horizontalLengthSquared() > adjustedMovement.horizontalLengthSquared()) {
                    double deltaY = playerCollisionBox.minY() - playerCB.minY();
                    stepCollision.setY(stepCollision.getY() - deltaY);
                    return stepCollision;
                }
            }
        }
        return adjustedMovement;
    }

    private void move() {
        MutableVec3d movement = new MutableVec3d(velocity);
        if (stuckSpeedMultiplier.lengthSquared() > 1.0E-7) {
            movement.multiply(stuckSpeedMultiplier.getX(), stuckSpeedMultiplier.getY(), stuckSpeedMultiplier.getZ());
            stuckSpeedMultiplier.set(0, 0, 0);
            velocity.set(0, 0, 0);
        }

        // in-place velocity update
        maybeBackOffFromEdge(movement);

        var collidedVec = collide(movement);

        boolean isXAdjusted = !MathHelper.equal(collidedVec.getX(), movement.getX());
        boolean isYAdjusted = !MathHelper.equal(collidedVec.getY(), movement.getY());
        boolean isZAdjusted = !MathHelper.equal(collidedVec.getZ(), movement.getZ());

        horizontalCollision = isXAdjusted || isZAdjusted;
        verticalCollision = isYAdjusted;
        boolean verticalCollisionBelow = verticalCollision && movement.getY() < 0;
        setOnGround(verticalCollisionBelow, collidedVec);
        horizontalCollisionMinor = horizontalCollision && isHorizontalCollisionMinor();

        if (horizontalCollision) {
            if (isXAdjusted) velocity.setX(0.0);
            if (isZAdjusted) velocity.setZ(0.0);
        }
        if (isYAdjusted) velocity.setY(0.0);

        final LocalizedCollisionBox movedPlayerCollisionBox = playerCollisionBox.move(
            collidedVec.getX(),
            collidedVec.getY(),
            collidedVec.getZ());

        // todo: apply block falling effects like bouncing off slime blocks

        this.x = ((movedPlayerCollisionBox.minX() + movedPlayerCollisionBox.maxX()) / 2.0);
        this.y = movedPlayerCollisionBox.minY();
        this.z = ((movedPlayerCollisionBox.minZ() + movedPlayerCollisionBox.maxZ()) / 2.0);
        syncPlayerCollisionBox();
        tryCheckInsideBlocks();
        float velocityMultiplier = MathHelper.lerp(movementEfficiency, this.getBlockSpeedFactor(), 1.0f);
        velocity.multiply(velocityMultiplier, 1.0, velocityMultiplier);
    }

    private boolean isHorizontalCollisionMinor() {
        float yawRads = yaw * (float) (Math.PI / 180.0);
        double sinYaw = Math.sin(yawRads);
        double cosYaw = Math.cos(yawRads);
        double leftImpulse = movementInput.pressingLeft && movementInput.pressingRight
            ? 0
            : (movementInput.pressingRight
                ? -1
                : (movementInput.pressingLeft
                    ? 1
                    : 0));
        double forwardImpulse = movementInput.pressingForward && movementInput.pressingBack
            ? 0
            : (movementInput.pressingBack
                ? -1
                : (movementInput.pressingForward
                    ? 1
                    : 0));
        double g = leftImpulse * cosYaw - forwardImpulse * sinYaw;
        double h = forwardImpulse * cosYaw + leftImpulse * sinYaw;
        double i = MathHelper.square(g) + MathHelper.square(h);
        double j = MathHelper.square(velocity.getX()) + MathHelper.square(velocity.getZ());
        if (!(i < 1.0E-5F) && !(j < 1.0E-5F)) {
            double k = g * velocity.getX() + h * velocity.getZ();
            double l = Math.acos(k / Math.sqrt(i * j));
            return l < 0.13962634F;
        } else {
            return false;
        }
    }

    private void tryCheckInsideBlocks() {
        var collidingBlockStates = World.getCollidingBlockStatesInside(playerCollisionBox);
        if (collidingBlockStates.isEmpty()) return;
        for (int i = 0; i < collidingBlockStates.size(); i++) {
            var localState = collidingBlockStates.get(i);
            if (localState.id() == BlockRegistry.BUBBLE_COLUMN.minStateId()) {
                if (BLOCK_DATA.isAir(World.getBlock(localState.x(), localState.y() + 1, localState.z()))) {
                    velocity.setY(Math.max(-0.9, velocity.getY() - 0.03));
                } else {
                    velocity.setY(Math.max(-0.3, velocity.getY() - 0.03));
                }
            } else if (localState.id() == BlockRegistry.BUBBLE_COLUMN.maxStateId()) {
                if (BLOCK_DATA.isAir(World.getBlock(localState.x(), localState.y() + 1, localState.z()))) {
                    velocity.setY(Math.min(1.8, velocity.getY() + 0.1));
                } else {
                    velocity.setY(Math.min(0.7, velocity.getY() + 0.06));
                }
            } else if (localState.block() == BlockRegistry.COBWEB) {
                fallDistance = 0.0;
                stuckSpeedMultiplier.set(0.25, 0.05, 0.25);
            } else if (localState.block() == BlockRegistry.HONEY_BLOCK) {
                // todo: changes in 1.21.3
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
                x, y, z)
                .move(movement.getX(), movement.getY(), movement.getZ());
            var supportPos = World.findSupportingBlockPos(cb);
            if (supportPos.isEmpty() && !this.onGroundNoBlocks) {
                var beforeMoveCb = cb.move(-movement.getX(), 0, -movement.getZ());
                this.supportingBlockPos = supportPos = World.findSupportingBlockPos(beforeMoveCb);
            } else {
                this.supportingBlockPos = supportPos;
            }
            this.onGroundNoBlocks = supportPos.isEmpty();
        } else {
            this.onGroundNoBlocks = false;
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

    private MutableVec3d collidePlayerBoundingBox(MutableVec3d movement, LocalizedCollisionBox pCollisionBox, List<LocalizedCollisionBox> blockCollisionBoxes) {
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

    private void maybeBackOffFromEdge(MutableVec3d movement) {
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
        updateVelocity(movementSpeed, movementInputVec);
        if (onClimbable()) {
            float maxV = 0.15F;
            double velX = Math.clamp(velocity.getX(), -maxV, maxV);
            double velZ = Math.clamp(velocity.getZ(), -maxV, maxV);
            double velY = Math.max(velocity.getY(), -maxV);
            if (velY < 0.0
                && World.getBlock(MathHelper.floorI(x), MathHelper.floorI(y), MathHelper.floorI(z)) != BlockRegistry.SCAFFOLDING
                && isSneaking
            ) {
                velY = 0.0;
            }

            velocity.set(velX, velY, velZ);
        }
        move();
        if (horizontalCollision || movementInput.jumping) {
            Block inBlock = World.getBlock(MathHelper.floorI(x), MathHelper.floorI(y), MathHelper.floorI(z));
            if (onClimbable() || inBlock == BlockRegistry.POWDER_SNOW) { // todo: or inside powder snow
                velocity.setY(0.2);
            }
        }
    }

    private void tickEntityPushing() {
        if (CACHE.getPlayerCache().getGameMode() == GameMode.SPECTATOR) return;
        var selfTeam = CACHE.getTeamCache().getTeamsByPlayer().get(CACHE.getProfileCache().getProfile().getName());
        var selfCollisionRule = selfTeam == null ? CollisionRule.ALWAYS : selfTeam.getCollisionRule();
        if (selfCollisionRule == CollisionRule.NEVER) return;
        List<EntityLiving> pushableEntities = new ArrayList<>(0);
        for (var it = CACHE.getEntityCache().getEntities().values().iterator(); it.hasNext(); ) {
            var entity = it.next();
            if (entity == CACHE.getPlayerCache().getThePlayer()) continue;
            if (!(entity instanceof EntityLiving entityLiving)) continue;
            var otherScoreboardName = entity.getUuid().toString();
            if (entity instanceof EntityPlayer entityPlayer) {
                var playerListEntry = CACHE.getTabListCache().get(entityPlayer.getUuid());
                if (playerListEntry.isPresent()) {
                    if (playerListEntry.get().getGameMode() == GameMode.SPECTATOR) continue;
                    otherScoreboardName = playerListEntry.get().getName();
                }
            }
            var otherTeam = CACHE.getTeamCache().getTeamsByPlayer().get(otherScoreboardName);
            var otherCollisionRule = otherTeam == null ? CollisionRule.ALWAYS : otherTeam.getCollisionRule();
            if (otherCollisionRule == CollisionRule.NEVER) {
                continue;
            } else {
                var teamsAllied = selfTeam != null && selfTeam.equals(otherTeam);
                if ((selfCollisionRule == CollisionRule.PUSH_OWN_TEAM || otherCollisionRule == CollisionRule.PUSH_OWN_TEAM) && teamsAllied) {
                    continue;
                } else {
                    if ((selfCollisionRule == CollisionRule.PUSH_OTHER_TEAMS || otherCollisionRule == CollisionRule.PUSH_OTHER_TEAMS) && !teamsAllied) {
                        continue;
                    }
                }
            }
            if (CACHE.getPlayerCache().distanceSqToSelf(entity) > 16.0) continue;
            EntityType entityType = entityLiving.getEntityType();
            if (entityType == EntityType.HORSE
                || entityType == EntityType.CAMEL
                || entityType == EntityType.DONKEY
                || entityType == EntityType.LLAMA
                || entityType == EntityType.MULE
                || entityType == EntityType.SKELETON_HORSE
                || entityType == EntityType.TRADER_LLAMA
                || entityType == EntityType.ZOMBIE_HORSE
            ) {
                // todo: cache passenger data in entity cache
                //  we should be store both its has passengers, and if it is a passenger to which other entity
                boolean hasPassenger = CACHE.getEntityCache().getEntities().values().stream()
                    .anyMatch(e -> e.isInVehicle() && e.getVehicleId() == entityLiving.getEntityId());
                if (hasPassenger) continue;
                else pushableEntities.add(entityLiving);
            }
            if (entityType == EntityType.MINECART
                || entityType == EntityType.CHEST_MINECART
                || entityType == EntityType.COMMAND_BLOCK_MINECART
                || entityType == EntityType.FURNACE_MINECART
                || entityType == EntityType.HOPPER_MINECART
                || entityType == EntityType.TNT_MINECART
                || entityType == EntityType.SPAWNER_MINECART
            ) {
                boolean hasPassenger = entityType == EntityType.MINECART
                    && CACHE.getEntityCache().getEntities().values().stream()
                        .anyMatch(e -> e.isInVehicle() && e.getVehicleId() == entityLiving.getEntityId());
                if (hasPassenger) continue;
                pushableEntities.add(entityLiving);
            }
            if (entityType == EntityType.ARMOR_STAND) continue;
            if (entityType == EntityType.BAT) continue;
            if (entityType.toString().endsWith("_BOAT") || entityType.toString().endsWith("_RAFT")) {
                boolean hasPassenger = CACHE.getEntityCache().getEntities().values().stream()
                    .anyMatch(e -> e.isInVehicle() && e.getVehicleId() == entityLiving.getEntityId());
                if (hasPassenger) continue;
                pushableEntities.add(entityLiving);
            }
            if (entityType == EntityType.PARROT) {
                pushableEntities.add(entityLiving);
            }
            EntityData entityData = ENTITY_DATA.getEntityData(entityType);
            if (entityData == null) continue;
            if (entityData.livingEntity()) {
                boolean isSpectator = false;
                if (entityLiving instanceof EntityPlayer player) {
                    isSpectator = CACHE.getTabListCache().get(player.getUuid())
                        .map(PlayerListEntry::getGameMode)
                        .filter(gm -> gm == GameMode.SPECTATOR)
                        .isPresent();
                }
                if (entityLiving.isAlive() && !isSpectator && !World.onClimbable(entityLiving)) {
                    pushableEntities.add(entityLiving);
                }
            }
        }
        if (pushableEntities.isEmpty()) return;
        var playerCB = getPlayerCollisionBox().inflate(0.2, -0.1, 0.2);
        for (int i = 0; i < pushableEntities.size(); i++) {
            var entity = pushableEntities.get(i);
            var entityCB = ENTITY_DATA.getCollisionBox(entity);
            if (!playerCB.intersects(entityCB)) continue;
            double xDiff = entity.getX() - getX();
            double zDiff = entity.getZ() - getZ();
            double maxAbsDiff = MathHelper.absMax(xDiff, zDiff);
            if (maxAbsDiff >= 0.01) {
                maxAbsDiff = Math.sqrt(maxAbsDiff);
                xDiff /= maxAbsDiff;
                zDiff /= maxAbsDiff;
                double inside = Math.min(1.0, 1.0 / maxAbsDiff);
                xDiff *= inside;
                zDiff *= inside;
                xDiff *= 0.05;
                zDiff *= 0.05;
                velocity.add(-xDiff, 0, -zDiff);
            }
        }
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
        Block inBlock = World.getBlock(MathHelper.floorI(x), MathHelper.floorI(y), MathHelper.floorI(z));
        float inBlockSpeedFactor = getBlockSpeedFactor(inBlock);
        if (inBlockSpeedFactor != 1.0f || World.isWater(inBlock)) return inBlockSpeedFactor;
        int blockX, blockY, blockZ;
        if (supportingBlockPos.isPresent()) {
            BlockPos pos = supportingBlockPos.get();
            blockX = pos.x();
            blockY = MathHelper.floorI(y - 0.500001);
            blockZ = pos.z();
        } else {
            blockX = MathHelper.floorI(x);
            blockY = MathHelper.floorI(y - 0.500001);
            blockZ = MathHelper.floorI(z);
        }
        Block underPlayer = World.getBlock(blockX, blockY, blockZ);
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
        var knockback = packet.getPlayerKnockback();
        if (knockback != null) {
            this.velocity.add(knockback.getX(), knockback.getY(), knockback.getZ());
        }
    }

    public void syncFromCache(boolean full) {
        this.x = this.lastX = CACHE.getPlayerCache().getX();
        this.y = this.lastY = CACHE.getPlayerCache().getY();
        this.z = this.lastZ = CACHE.getPlayerCache().getZ();
        this.yaw = this.lastYaw = this.requestedYaw = CACHE.getPlayerCache().getYaw();
        this.pitch = this.lastPitch = this.requestedPitch = CACHE.getPlayerCache().getPitch();
        this.onGround = this.lastOnGround = true; // todo: cache
        this.velocity.set(0, 0, 0);
        this.supportingBlockPos = Optional.empty();
        this.onGroundNoBlocks = false;
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
        double lavaSpeedMult = currentDim != null && currentDim == DimensionRegistry.THE_NETHER.get()
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
            if (vehicle != null && World.isBoat(vehicle.getEntityType())) {
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

    // todo: handle lava and water next to each other?
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
                    var fluidState = World.getFluidState(blockState.id());
                    if (fluidState == null) continue;
                    if (waterFluid) {
                        if (!World.isWater(blockState.block())) continue;
                    } else {
                        if (blockState.block() != BlockRegistry.LAVA) continue;
                    }
                    float fluidHeight = World.getFluidHeight(fluidState);
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
        if (waterFluid) {
            waterHeight = topFluidHDelta;
        } else {
            lavaHeight = topFluidHDelta;
        }
        return touched;
    }

    private boolean onClimbable() {
        var inBlock = World.getBlock(MathHelper.floorI(x), MathHelper.floorI(y), MathHelper.floorI(z));
        if (inBlock.blockTags().contains(BlockTags.CLIMBABLE)) {
            return true;
        } else if (inBlock.name().endsWith("_trapdoor")) {
            int blockStateId = World.getBlockStateId(MathHelper.floorI(x), MathHelper.floorI(y), MathHelper.floorI(z));
            var openProperty = World.getBlockStateProperty(blockStateId, BlockStateProperties.OPEN);
            if (openProperty != null && openProperty) {
                int blockStateIdBelow = World.getBlockStateId(MathHelper.floorI(x), MathHelper.floorI(y) - 1, MathHelper.floorI(z));
                Block blockBelow = World.getBlock(blockStateIdBelow);
                if (blockBelow == BlockRegistry.LADDER) {
                    var ladderFacing = World.getBlockStateProperty(blockStateIdBelow, BlockStateProperties.FACING);
                    var trapdoorFacing = World.getBlockStateProperty(blockStateId, BlockStateProperties.FACING);
                    return ladderFacing == trapdoorFacing;
                }
            }
        }
        return false;
    }

    private boolean resyncTeleport() {
        // can occur when a connected player disconnects in an unusual way like crashing
        if (!CACHE.getPlayerCache().getTeleportQueue().isEmpty()) {
            warn("Detected teleport desync, resyncing. queueSize: {}", CACHE.getPlayerCache().getTeleportQueue().size());
            int count = 0;
            while (!CACHE.getPlayerCache().getTeleportQueue().isEmpty() && count++ < 25) {
                var packet = CACHE.getPlayerCache().getTeleportQueue().poll();
                var cache = CACHE.getPlayerCache();
                cache
                    .setRespawning(false)
                    .setX((packet.getRelatives().contains(PositionElement.X) ? cache.getX() : 0.0d) + packet.getX())
                    .setY((packet.getRelatives().contains(PositionElement.Y) ? cache.getY() : 0.0d) + packet.getY())
                    .setZ((packet.getRelatives().contains(PositionElement.Z) ? cache.getZ() : 0.0d) + packet.getZ())
                    .setVelX((packet.getRelatives().contains(PositionElement.DELTA_X) ? cache.getVelX() : 0.0d) + packet.getDeltaX())
                    .setVelY((packet.getRelatives().contains(PositionElement.DELTA_Y) ? cache.getVelY() : 0.0d) + packet.getDeltaY())
                    .setVelZ((packet.getRelatives().contains(PositionElement.DELTA_Z) ? cache.getVelZ() : 0.0d) + packet.getDeltaZ())
                    .setYaw((packet.getRelatives().contains(PositionElement.Y_ROT) ? cache.getYaw() : 0.0f) + packet.getYaw())
                    .setPitch((packet.getRelatives().contains(PositionElement.X_ROT) ? cache.getPitch() : 0.0f) + packet.getPitch());
                debug("Sending queued teleport: {}", packet.getId());
                syncFromCache(true);
                sendClientPacketAwait(new ServerboundAcceptTeleportationPacket(packet.getId()));
                sendClientPacketAwait(new ServerboundMovePlayerPosRotPacket(false, false, x, y, z, yaw, pitch));
            }
        }
        return !CACHE.getPlayerCache().getTeleportQueue().isEmpty();
    }

    public void updateAttributes() {
        this.speedAttribute = getClonedAttribute(AttributeType.Builtin.MOVEMENT_SPEED, DEFAULT_SPEED_ATTRIBUTE);
        this.speed = getLocalAttributeValue(speedAttribute, 0.10000000149011612f);
        applySprintingSpeedAttributeModifier();
        this.movementEfficiency = getAttributeValue(AttributeType.Builtin.MOVEMENT_EFFICIENCY, 0.0f);
        this.waterMovementEfficiency = getAttributeValue(AttributeType.Builtin.WATER_MOVEMENT_EFFICIENCY, 0.0f);
        this.stepHeight = getAttributeValue(AttributeType.Builtin.STEP_HEIGHT, 0.6f);
        this.gravity = getAttributeValue(AttributeType.Builtin.GRAVITY, 0.08f);
        this.jumpStrength = getAttributeValue(AttributeType.Builtin.JUMP_STRENGTH, 0.42f);
        this.sneakSpeed = getAttributeValue(AttributeType.Builtin.SNEAKING_SPEED, 0.3f);
    }

    public float getAttributeValue(final AttributeType.Builtin attributeType, float defaultValue) {
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

    // todo: better solution for local attribute cache
    public float getLocalAttributeValue(final Attribute attribute, float defaultValue) {
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

    private Attribute getClonedAttribute(final AttributeType attributeType, final Attribute defaultAttribute) {
        var attribute = CACHE.getPlayerCache().getThePlayer().getAttributes().get(attributeType);
        if (attribute == null) return defaultAttribute;
        return new Attribute(attribute.getType(), attribute.getValue(), Lists.newArrayList(attribute.getModifiers()));
    }

    private boolean canPlayerFitWithinBlocksAndEntitiesWhen(CollisionBox poseCb) {
        var sneakingCb = new LocalizedCollisionBox(poseCb, x, y, z).inflate(-1.0E-7, -1.0E-7, -1.0E-7);
        var levelCbs = World.getIntersectingCollisionBoxes(sneakingCb);
        for (int i = 0; i < levelCbs.size(); i++) {
            final var cb = levelCbs.get(i);
            if (sneakingCb.intersects(cb)) {
                return false;
            }
        }
        return true;
    }

    public double getEyeY() {
        return playerCollisionBox.maxY() - 0.18;
    }

    public double getBlockReachDistance() {
        return MathHelper.clamp(getAttributeValue(AttributeType.Builtin.BLOCK_INTERACTION_RANGE, 4.5f) + CONFIG.client.extra.click.additionalBlockReach, 0, Float.MAX_VALUE);
    }

    public double getEntityInteractDistance() {
        return MathHelper.clamp(getAttributeValue(AttributeType.Builtin.ENTITY_INTERACTION_RANGE, 3.0f) + CONFIG.client.extra.click.additionalEntityReach, 0, Float.MAX_VALUE);
    }

    public BlockPos blockPosition() {
        return new BlockPos(MathHelper.floorI(x), MathHelper.floorI(y), MathHelper.floorI(z));
    }
}
