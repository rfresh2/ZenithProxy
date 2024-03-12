package com.zenith.module.impl;

import com.github.steveice10.mc.protocol.data.game.entity.EquipmentSlot;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.github.steveice10.mc.protocol.data.game.entity.player.Hand;
import com.github.steveice10.mc.protocol.data.game.inventory.ContainerActionType;
import com.github.steveice10.mc.protocol.data.game.inventory.MoveToHotbarAction;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundSetCarriedItemPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundUseItemPacket;
import com.zenith.Proxy;
import com.zenith.event.module.AutoEatOutOfFoodEvent;
import com.zenith.event.module.ClientTickEvent;
import com.zenith.feature.items.ContainerClickAction;
import com.zenith.module.Module;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static com.zenith.Shared.*;
import static java.util.Objects.nonNull;

public class AutoEat extends Module {
    private int delay = 0;
    private boolean swapping = false;
    private Instant lastAutoEatOutOfFoodWarning = Instant.EPOCH;
    private boolean isEating = false;
    private static final int MOVEMENT_PRIORITY = 1000;

    public AutoEat() {
        super();
    }

    @Override
    public void subscribeEvents() {
        EVENT_BUS.subscribe(this,
            ClientTickEvent.class, this::handleClientTick
        );
    }

    public boolean isEating() {
        return CONFIG.client.extra.autoEat.enabled && (isEating || swapping);
    }

    @Override
    public boolean shouldBeEnabled() {
        return CONFIG.client.extra.autoEat.enabled;
    }

    public void handleClientTick(final ClientTickEvent e) {
        if (CACHE.getPlayerCache().getThePlayer().isAlive()
                && playerHealthBelowThreshold()
                && Instant.now().minus(Duration.ofSeconds(10)).isAfter(Proxy.getInstance().getConnectTime())) {
            if (delay > 0) {
                delay--;
                return;
            } else {
                if (swapping) {
                    delay = 5;
                    swapping = false;
                    return;
                }
                // todo: possible to optimize this so we don't re-check inventory every tick
                //  inventory will only change on certain events
                if (switchToFood()) {
                    startEating();
                } else {
                    isEating = false;
                }
            }
            if (isEating) {
                PATHING.stop(MOVEMENT_PRIORITY);
            }
        } else {
            isEating = false;
        }
    }

    public boolean switchToFood() {
        // check if offhand has a food
        final ItemStack offhandStack = CACHE.getPlayerCache().getEquipment(EquipmentSlot.OFF_HAND);
        if (nonNull(offhandStack)) {
            if (FOOD_MANAGER.isSafeFood(offhandStack.getId())) {
                return true;
            }
        }

        // check if selected hotbar item has a food
        final ItemStack mainHandStack = CACHE.getPlayerCache().getEquipment(EquipmentSlot.MAIN_HAND);
        if (nonNull(mainHandStack)) {
            if (FOOD_MANAGER.isSafeFood(mainHandStack.getId())) {
              return true;
            }
        }

        // find next food and switch it to our hotbar slot
        final List<ItemStack> inventory = CACHE.getPlayerCache().getPlayerInventory();
        for (int i = 44; i >= 9; i--) {
            ItemStack itemStack = inventory.get(i);
            if (nonNull(itemStack)) {
                if (FOOD_MANAGER.isSafeFood(itemStack.getId())) {
                    PLAYER_INVENTORY_MANAGER.invActionReq(
                        this,
                        new ContainerClickAction(i, ContainerActionType.MOVE_TO_HOTBAR_SLOT, MoveToHotbarAction.SLOT_1),
                        MOVEMENT_PRIORITY
                    );
                    if (CACHE.getPlayerCache().getHeldItemSlot() != 0) {
                        sendClientPacketAsync(new ServerboundSetCarriedItemPacket(0));
                    }
                    delay = 5;
                    swapping = true;
                    return false;
                }
            }
        }

        if (CONFIG.client.extra.autoEat.warning && Instant.now().minus(Duration.ofHours(7)).isAfter(lastAutoEatOutOfFoodWarning)) {
            EVENT_BUS.postAsync(new AutoEatOutOfFoodEvent());
            lastAutoEatOutOfFoodWarning = Instant.now();
        }

        return false;
    }

    public void startEating() {
        final ItemStack offhandStack = CACHE.getPlayerCache().getEquipment(EquipmentSlot.OFF_HAND);
        if (nonNull(offhandStack)) {
            if (FOOD_MANAGER.isFood(offhandStack.getId())) {
                isEating = true;
                delay = 50;
                sendClientPacketAsync(new ServerboundUseItemPacket(Hand.OFF_HAND, CACHE.getPlayerCache().getActionId().incrementAndGet()));
                CLIENT_LOG.debug("AutoEat: Eating {} from offhand", offhandStack.getId());
                return;
            }
        }

        final ItemStack mainHandStack = CACHE.getPlayerCache().getEquipment(EquipmentSlot.MAIN_HAND);
        if (nonNull(mainHandStack)) {
            if (FOOD_MANAGER.isFood(mainHandStack.getId())) {
                isEating = true;
                delay = 50;
                sendClientPacketAsync(new ServerboundUseItemPacket(Hand.MAIN_HAND, CACHE.getPlayerCache().getActionId().incrementAndGet()));
                CLIENT_LOG.debug("AutoEat: Eating {} from mainhand", mainHandStack.getId());
            }
        }
    }

    @Override
    public void clientTickStopped() {
        swapping = false;
        delay = 0;
        lastAutoEatOutOfFoodWarning = Instant.EPOCH;
        isEating = false;
    }

    private boolean playerHealthBelowThreshold() {
        return CACHE.getPlayerCache().getThePlayer().getHealth() <= CONFIG.client.extra.autoEat.healthThreshold
            || CACHE.getPlayerCache().getThePlayer().getFood() <= CONFIG.client.extra.autoEat.hungerThreshold;
    }
}
