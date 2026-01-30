package com.zenith.module.impl;

import com.github.rfresh2.EventConsumer;
import com.zenith.event.client.ClientBotTick;
import com.zenith.event.module.AutoEatOutOfFoodEvent;
import com.zenith.feature.inventory.InventoryActionRequest;
import com.zenith.feature.inventory.util.InventoryUtil;
import com.zenith.feature.player.ClickTarget;
import com.zenith.feature.player.Input;
import com.zenith.feature.player.InputRequest;
import com.zenith.mc.food.FoodData;
import com.zenith.mc.food.FoodRegistry;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.GameMode;
import org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

import static com.github.rfresh2.EventConsumer.of;
import static com.zenith.Globals.*;

public class AutoEat extends AbstractInventoryModule {
    private int delay = 0;
    private Instant lastAutoEatOutOfFoodWarning = Instant.EPOCH;
    private boolean isEating = false;
    private boolean isStartingToEat = false;

    public AutoEat() {
        super(HandRestriction.EITHER, 0);
    }

    @Override
    public List<EventConsumer<?>> registerEvents() {
        return List.of(
            of(ClientBotTick.class, this::handleClientTick),
            of(ClientBotTick.Starting.class, this::handleBotTickStarting),
            of(ClientBotTick.Stopped.class, this::handleBotTickStopped)
        );
    }

    @Override
    public int getPriority() {
        return Objects.requireNonNullElse(CONFIG.client.extra.autoEat.priority, 11000);
    }

    public boolean isEating() {
        return enabledSetting() && isEating;
    }

    // currently swapping item to hand, or in the process of eating
    public boolean isStartingToEat() {
        return enabledSetting() && isStartingToEat;
    }

    @Override
    public boolean enabledSetting() {
        return CONFIG.client.extra.autoEat.enabled;
    }

    void handleClientTick(final ClientBotTick e) {
        if (CACHE.getPlayerCache().getThePlayer().isAlive()
            && CACHE.getPlayerCache().getGameMode() != GameMode.CREATIVE
            && CACHE.getPlayerCache().getGameMode() != GameMode.SPECTATOR
            && playerHealthBelowThreshold()
        ) {
            if (delay > 0) {
                delay--;
                if (isEating || isStartingToEat) {
                    INPUTS.submit(InputRequest.noInput(this, getPriority()));
                    INVENTORY.submit(InventoryActionRequest.noAction(this, getPriority()));
                }
                return;
            }
            if (InventoryUtil.searchPlayerInventory(this::hasFoodIgnoreHunger) == -1) {
                if (CONFIG.client.extra.autoEat.warning && Instant.now().minus(Duration.ofHours(7)).isAfter(lastAutoEatOutOfFoodWarning)) {
                    warn("Out of food");
                    EVENT_BUS.postAsync(new AutoEatOutOfFoodEvent());
                    lastAutoEatOutOfFoodWarning = Instant.now();
                }
                isEating = false;
                isStartingToEat = false;
                return;
            }
            isEating = false;
            if (switchToFood()) {
                startEating();
            }
        } else {
            isEating = false;
            isStartingToEat = false;
        }
    }

    boolean switchToFood() {
        delay = doInventoryActions();
        final boolean shouldStartEating = getHand() != null && delay == 0;
        isStartingToEat = getHand() != null || delay != 0;
        return shouldStartEating;
    }

    void startEating() {
        var hand = getHand();
        if (hand == null) {
            // shouldn't ever get here but just in case
            isEating = false;
            isStartingToEat = false;
            return;
        }
        INPUTS.submit(InputRequest.builder()
                .owner(this)
                .input(Input.builder()
                    .rightClick(true)
                    .clickTarget(ClickTarget.None.INSTANCE)
                    .clickRequiresRotation(false)
                    .build())
                .priority(getPriority())
                .build())
            .addInputExecutedListener(future -> {
                isEating = true;
                delay = 50;
            });
    }

    public void onEnable() {
        reset();
    }

    public void onDisable() {
        reset();
    }


    void handleBotTickStarting(final ClientBotTick.Starting event) {
        reset();
    }

    void handleBotTickStopped(final ClientBotTick.Stopped event) {
        reset();
    }

    void reset() {
        delay = 0;
        lastAutoEatOutOfFoodWarning = Instant.EPOCH;
        isEating = false;
        isStartingToEat = false;
    }

    boolean playerHealthBelowThreshold() {
        return CACHE.getPlayerCache().getThePlayer().getHealth() <= CONFIG.client.extra.autoEat.healthThreshold
            || CACHE.getPlayerCache().getThePlayer().getFood() <= CONFIG.client.extra.autoEat.hungerThreshold;
    }

    @Override
    public boolean itemPredicate(final ItemStack itemStack) {
        return hasFood(itemStack);
    }

    boolean hasFood(boolean ignoreHunger, ItemStack itemStack) {
        FoodData foodData = FoodRegistry.REGISTRY.get(itemStack.getId());
        boolean canEat = ignoreHunger || CACHE.getPlayerCache().getThePlayer().getFood() < 20;
        return foodData != null
            && (CONFIG.client.extra.autoEat.allowUnsafeFood || foodData.isSafeFood())
            && (canEat || foodData.canAlwaysEat());
    }

    boolean hasFoodIgnoreHunger(ItemStack itemStack) {
        return hasFood(true, itemStack);
    }

    boolean hasFood(ItemStack itemStack) {
        return hasFood(false, itemStack);
    }
}
