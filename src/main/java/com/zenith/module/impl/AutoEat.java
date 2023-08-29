package com.zenith.module.impl;

import com.zenith.Proxy;
import com.zenith.cache.data.PlayerCache;
import com.zenith.event.Subscription;
import com.zenith.event.module.ClientTickEvent;
import com.zenith.feature.food.FoodManager;
import com.zenith.module.Module;
import lombok.Getter;

import java.time.Duration;
import java.time.Instant;
import java.util.function.Supplier;

import static com.zenith.Shared.*;

public class AutoEat extends Module {
    private int actionId = 0; // todo: might need to track this in cache. this will be inaccurate incrementing in many cases
    private final FoodManager foodManager = new FoodManager();
    private int delay = 0;
    private boolean swapping = false;
    private Instant lastAutoEatOutOfFoodWarning = Instant.EPOCH;
    @Getter
    private boolean isEating = false;

    public AutoEat() {
        super();
    }

    @Override
    public Subscription subscribeEvents() {
        return EVENT_BUS.subscribe(
            ClientTickEvent.class, this::handleClientTick
        );
    }

    @Override
    public Supplier<Boolean> shouldBeEnabled() {
        return () -> CONFIG.client.extra.autoEat.enabled;
    }

    public void handleClientTick(final ClientTickEvent e) {
        if (CACHE.getPlayerCache().getThePlayer().getHealth() > 0
                && playerHealthBelowThreshold()
                && !Proxy.getInstance().isInQueue()
                && Instant.now().minus(Duration.ofSeconds(10)).isAfter(Proxy.getInstance().getConnectTime())
                && !MODULE_MANAGER.getModule(AutoTotem.class).map(AutoTotem::isActive).orElse(false)) {
            if (delay > 0) {
                delay--;
                return;
            } else {
                if (swapping) {
                    PlayerCache.sync();
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
        } else {
            isEating = false;
        }
    }

    public boolean switchToFood() {
        // todo: new packets
//        // check if offhand has a food
//        final ItemStack offhandStack = CACHE.getPlayerCache().getThePlayer().getEquipment().get(EquipmentSlot.OFF_HAND);
//        if (nonNull(offhandStack)) {
//            if (foodManager.isSafeFood(offhandStack.getId(), offhandStack.getData())) {
//                return true;
//            }
//        }
//
//        // check if selected hotbar item has a food
//        final ItemStack mainHandStack = CACHE.getPlayerCache().getThePlayer().getEquipment().get(EquipmentSlot.MAIN_HAND);
//        if (nonNull(mainHandStack)) {
//            if (foodManager.isSafeFood(mainHandStack.getId(), mainHandStack.getData())) {
//                return true;
//            }
//        }
//
//        // find next food and switch it to our hotbar slot
//        final ItemStack[] inventory = CACHE.getPlayerCache().getInventory();
//        for (int i = 44; i >= 9; i--) {
//            ItemStack itemStack = inventory[i];
//            if (nonNull(itemStack)) {
//                if (foodManager.isSafeFood(itemStack.getId(), itemStack.getData())) {
//                    sendClientPacketAsync(new ClientWindowActionPacket(0, actionId++, i, new ItemStack(0, 0), WindowAction.MOVE_TO_HOTBAR_SLOT, MoveToHotbarParam.SLOT_1));
//                    if (CACHE.getPlayerCache().getHeldItemSlot() != 0) {
//                        sendClientPacketAsync(new ClientPlayerChangeHeldItemPacket(0));
//                    }
//                    delay = 5;
//                    swapping = true;
//                    return false;
//                }
//            }
//        }
//
//        if (CONFIG.client.extra.autoEat.warning && Instant.now().minus(Duration.ofHours(7)).isAfter(lastAutoEatOutOfFoodWarning)) {
//            EVENT_BUS.postAsync(new AutoEatOutOfFoodEvent());
//            lastAutoEatOutOfFoodWarning = Instant.now();
//        }

        return false;
    }

    public void startEating() {
        // todo: new packets
//        final ItemStack offhandStack = CACHE.getPlayerCache().getThePlayer().getEquipment().get(EquipmentSlot.OFF_HAND);
//        if (nonNull(offhandStack)) {
//            if (foodManager.isFood(offhandStack.getId())) {
//                isEating = true;
//                delay = 50;
//                sendClientPacketAsync(new ClientPlayerUseItemPacket(Hand.OFF_HAND));
//                return;
//            }
//        }
//
//        final ItemStack mainHandStack = CACHE.getPlayerCache().getThePlayer().getEquipment().get(EquipmentSlot.MAIN_HAND);
//        if (nonNull(mainHandStack)) {
//            if (foodManager.isFood(mainHandStack.getId())) {
//                isEating = true;
//                delay = 50;
//                sendClientPacketAsync(new ClientPlayerUseItemPacket(Hand.MAIN_HAND));
//            }
//        }
    }

    @Override
    public void clientTickStopping() {
        swapping = false;
        actionId = 0;
        delay = 0;
        lastAutoEatOutOfFoodWarning = Instant.EPOCH;
        isEating = false;
    }

    private boolean playerHealthBelowThreshold() {
        return CACHE.getPlayerCache().getThePlayer().getHealth() <= CONFIG.client.extra.autoEat.healthThreshold
                || CACHE.getPlayerCache().getThePlayer().getFood() <= CONFIG.client.extra.autoEat.hungerThreshold;
    }
}
