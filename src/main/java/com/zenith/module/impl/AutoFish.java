package com.zenith.module.impl;

import com.zenith.cache.data.entity.Entity;
import com.zenith.cache.data.entity.EntityStandard;
import com.zenith.event.module.ClientBotTick;
import com.zenith.event.module.EntityFishHookSpawnEvent;
import com.zenith.event.module.SplashSoundEffectEvent;
import com.zenith.util.Timer;
import com.zenith.util.math.MathHelper;
import org.geysermc.mcprotocollib.protocol.data.game.entity.object.ProjectileData;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.Hand;
import org.geysermc.mcprotocollib.protocol.data.game.entity.type.EntityType;
import org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundUseItemPacket;

import java.time.Instant;

import static com.github.rfresh2.EventConsumer.of;
import static com.zenith.Shared.*;

public class AutoFish extends AbstractInventoryModule {
    private final Timer castTimer = Timer.newTickTimer();
    private int fishHookEntityId = -1;
    private Hand rodHand = Hand.MAIN_HAND;
    private int delay = 0;
    public static final int MOVEMENT_PRIORITY = 10;
    private Instant castTime = Instant.EPOCH;
    private final int fishingRodId = ITEMS.getItemId("fishing_rod");

    public AutoFish() {
        super(false, 2, MOVEMENT_PRIORITY);
    }

    @Override
    public void subscribeEvents() {
        EVENT_BUS.subscribe(this,
                            of(EntityFishHookSpawnEvent.class, this::handleEntityFishHookSpawnEvent),
                            of(SplashSoundEffectEvent.class, this::handleSplashSoundEffectEvent),
                            of(ClientBotTick.class, this::handleClientTick),
                            of(ClientBotTick.Starting.class, this::handleBotTickStarting),
                            of(ClientBotTick.Stopped.class, this::handleBotTickStopped)
        );
    }

    @Override
    public boolean shouldBeEnabled() {
        return CONFIG.client.extra.autoFish.enabled;
    }


    public void handleBotTickStarting(final ClientBotTick.Starting event) {
        reset();
    }

    public void handleBotTickStopped(final ClientBotTick.Stopped event) {
        reset();
    }

    private synchronized void reset() {
        fishHookEntityId = -1;
        castTimer.reset();
        delay = 0;
        castTime = Instant.EPOCH;
    }

    public void handleEntityFishHookSpawnEvent(final EntityFishHookSpawnEvent event) {
        try {
            if (event.getOwnerEntityId() != CACHE.getPlayerCache().getEntityId()) return;
            fishHookEntityId = event.fishHookObject().getEntityId();
        } catch (final Exception e) {
            MODULE_LOG.error("Failed to handle EntityFishHookSpawnEvent", e);
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

    public void handleClientTick(final ClientBotTick event) {
        if (MODULE.get(AutoEat.class).isEating() || MODULE.get(KillAura.class).isActive()) return;
        if (delay > 0) {
            delay--;
            return;
        }
        if (!isFishing() && switchToFishingRod() && castTimer.tick(CONFIG.client.extra.autoFish.castDelay)) {
            // cast
            cast();
        }
        if (isFishing() && Instant.now().getEpochSecond() - castTime.getEpochSecond() > 60) {
            // something's wrong, probably don't have hook in water
            CLIENT_LOG.warn("[AutoFish] Probably don't have hook in water. reeling in");
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
        delay = doInventoryActions();
        if (getHand() != null && delay == 0) {
            rodHand = getHand();
            return true;
        }
        return false;
    }

    private boolean isFishing() {
        final Entity cachedEntity = CACHE.getEntityCache().get(fishHookEntityId);
        return cachedEntity instanceof EntityStandard standard
            && standard.getEntityType() == EntityType.FISHING_BOBBER
            && ((ProjectileData) standard.getObjectData()).getOwnerId() == CACHE.getPlayerCache().getEntityId();
    }

    @Override
    public boolean itemPredicate(final ItemStack itemStack) {
        return itemStack.getId() == fishingRodId;
    }
}
