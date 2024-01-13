package com.zenith.module.impl;

import com.github.steveice10.mc.protocol.data.game.entity.EquipmentSlot;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.github.steveice10.mc.protocol.data.game.entity.object.ProjectileData;
import com.github.steveice10.mc.protocol.data.game.entity.player.Hand;
import com.github.steveice10.mc.protocol.data.game.entity.type.EntityType;
import com.github.steveice10.mc.protocol.data.game.inventory.ContainerActionType;
import com.github.steveice10.mc.protocol.data.game.inventory.MoveToHotbarAction;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.inventory.ServerboundContainerClickPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundSetCarriedItemPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundUseItemPacket;
import com.zenith.cache.data.PlayerCache;
import com.zenith.cache.data.entity.Entity;
import com.zenith.cache.data.entity.EntityStandard;
import com.zenith.event.module.ClientTickEvent;
import com.zenith.event.module.EntityFishHookSpawnEvent;
import com.zenith.event.module.SplashSoundEffectEvent;
import com.zenith.module.Module;
import com.zenith.util.Maps;
import com.zenith.util.TickTimer;
import com.zenith.util.math.MathHelper;
import org.slf4j.Logger;

import java.time.Instant;
import java.util.function.Supplier;

import static com.zenith.Shared.*;
import static com.zenith.event.SimpleEventBus.pair;
import static java.util.Objects.nonNull;
import static org.slf4j.LoggerFactory.getLogger;

public class AutoFish extends Module {
    private static final Logger LOGGER = getLogger(AutoFish.class);
    private final TickTimer castTimer = new TickTimer();
    private int fishHookEntityId = -1;
    private Hand rodHand = Hand.MAIN_HAND;
    private int delay = 0;
    private boolean swapping = false;
    public static final int MOVEMENT_PRIORITY = 10;
    private Instant castTime = Instant.EPOCH;
    private int fishingRodId = ITEMS_MANAGER.getItemId("fishing_rod");

    @Override
    public void subscribeEvents() {
        EVENT_BUS.subscribe(this,
            pair(EntityFishHookSpawnEvent.class, this::handleEntityFishHookSpawnEvent),
            pair(SplashSoundEffectEvent.class, this::handleSplashSoundEffectEvent),
            pair(ClientTickEvent.class, this::handleClientTick)
        );
    }

    @Override
    public Supplier<Boolean> shouldBeEnabled() {
        return () -> CONFIG.client.extra.autoFish.enabled;
    }

    @Override
    public void clientTickStarting() {
        reset();
    }

    @Override
    public void clientTickStopped() {
        reset();
    }

    private void reset() {
        fishHookEntityId = -1;
        castTimer.reset();
        delay = 0;
        swapping = false;
        castTime = Instant.EPOCH;
    }

    public void handleEntityFishHookSpawnEvent(final EntityFishHookSpawnEvent event) {
        try {
            ProjectileData data = (ProjectileData) event.fishHookObject().getObjectData();
            if (data.getOwnerId() != CACHE.getPlayerCache().getEntityId()) return;
            fishHookEntityId = event.fishHookObject().getEntityId();
        } catch (final Exception e) {
            LOGGER.error("Failed to handle EntityFishHookSpawnEvent", e);
        }
    }

    public void handleSplashSoundEffectEvent(final SplashSoundEffectEvent event) {
        if (isFishing()) {
            // reel in
            sendClientPacketAsync(new ServerboundUseItemPacket(rodHand, CACHE.getPlayerCache().getActionId().incrementAndGet()));
            castTimer.reset();
            fishHookEntityId = -1;
        }
    }

    public void handleClientTick(final ClientTickEvent event) {
        if (delay > 0) {
            delay--;
            return;
        }
        if (swapping) {
            PlayerCache.sync();
            delay = 5;
            swapping = false;
            return;
        }
        if (!isFishing() && switchToFishingRod() && castTimer.tick(CONFIG.client.extra.autoFish.castDelay, true)) {
            // cast
            cast();
        }
        if (isFishing() && Instant.now().getEpochSecond() - castTime.getEpochSecond() > 60) {
            // something's wrong, probably don't have hook in water
            CLIENT_LOG.warn("AutoFish: probably don't have hook in water. reeling in");
            fishHookEntityId = -1;
            sendClientPacketAsync(new ServerboundUseItemPacket(rodHand, CACHE.getPlayerCache().getActionId().incrementAndGet()));
            castTimer.reset();
        }
    }

    private void cast() {
        // rotate to water if needed
        float yawDiff = Math.abs(MathHelper.wrapPitch(CACHE.getPlayerCache()
                                                      .getYaw()) - MathHelper.wrapDegrees(CONFIG.client.extra.autoFish.yaw));
        float pitchDiff = Math.abs(MathHelper.wrapPitch(CACHE.getPlayerCache().getPitch()) - MathHelper.wrapDegrees(CONFIG.client.extra.autoFish.pitch));
        if ((yawDiff > 1.0 && yawDiff < 180.0f)
            || (pitchDiff > 1.0 && pitchDiff < 90.0f)) {
            PATHING.rotate(CONFIG.client.extra.autoFish.yaw, CONFIG.client.extra.autoFish.pitch, MOVEMENT_PRIORITY);
            delay = 5;
            return;
        }
        sendClientPacketAsync(new ServerboundUseItemPacket(rodHand, CACHE.getPlayerCache().getActionId().incrementAndGet()));
        castTime = Instant.now();
    }

    public boolean switchToFishingRod() {
        // check if offhand has rod
        final ItemStack offhandStack = CACHE.getPlayerCache().getThePlayer().getEquipment().get(EquipmentSlot.OFF_HAND);
        if (nonNull(offhandStack)) {
            if (offhandStack.getId() == fishingRodId) {
                rodHand = Hand.OFF_HAND;
                return true;
            }
        }
        // check mainhand
        final ItemStack mainHandStack = CACHE.getPlayerCache().getThePlayer().getEquipment().get(EquipmentSlot.MAIN_HAND);
        if (nonNull(mainHandStack)) {
            if (mainHandStack.getId() == fishingRodId) {
                rodHand = Hand.MAIN_HAND;
                return true;
            }
        }

        // find next rod and switch it into our hotbar slot
        final ItemStack[] inventory = CACHE.getPlayerCache().getInventory();
        for (int i = 44; i >= 9; i--) {
            final ItemStack stack = inventory[i];
            if (nonNull(stack) && stack.getId() == fishingRodId) {
                sendClientPacketAsync(new ServerboundContainerClickPacket(0,
                                                                          CACHE.getPlayerCache().getActionId().incrementAndGet(),
                                                                          i,
                                                                          ContainerActionType.MOVE_TO_HOTBAR_SLOT,
                                                                          MoveToHotbarAction.SLOT_3,
                                                                          null,
                                                                          Maps.of(
                                                                              i, inventory[38],
                                                                              38, stack
                                                                          )));
                if (CACHE.getPlayerCache().getHeldItemSlot() != 2) {
                    sendClientPacketAsync(new ServerboundSetCarriedItemPacket(2));
                }
                delay = 5;
                swapping = true;
                rodHand = Hand.MAIN_HAND;
                return false;
            }
        }
        // no rod
        return false;
    }

    private boolean isFishing() {
        final Entity cachedEntity = CACHE.getEntityCache().get(fishHookEntityId);
        return cachedEntity instanceof EntityStandard standard
            && standard.getEntityType() == EntityType.FISHING_BOBBER
            && ((ProjectileData) standard.getObjectData()).getOwnerId() == CACHE.getPlayerCache().getEntityId();
    }

}
