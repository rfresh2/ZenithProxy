package com.zenith.module.impl;

import com.github.rfresh2.EventConsumer;
import com.zenith.event.client.ClientBotTick;
import com.zenith.feature.inventory.InventoryActionRequest;
import com.zenith.feature.player.ClickTarget;
import com.zenith.feature.player.Input;
import com.zenith.feature.player.InputRequest;
import com.zenith.mc.food.FoodRegistry;
import com.zenith.util.RequestFuture;
import org.geysermc.mcprotocollib.protocol.data.game.entity.Effect;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.MetadataTypes;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.GameMode;
import org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack;

import java.util.List;
import java.util.Objects;

import static com.github.rfresh2.EventConsumer.of;
import static com.zenith.Globals.CACHE;
import static com.zenith.Globals.CONFIG;
import static com.zenith.Globals.INPUTS;
import static com.zenith.Globals.INVENTORY;

public class AutoGap extends AbstractInventoryModule {
    private int delay = 0;
    private boolean isEating = false;
    private RequestFuture swapFuture = RequestFuture.rejected;

    public AutoGap() {
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
        return Objects.requireNonNullElse(CONFIG.client.extra.autoGap.priority, 11500);
    }

    @Override
    public boolean enabledSetting() {
        return CONFIG.client.extra.autoGap.enabled;
    }

    public boolean isEating() {
        return enabledSetting() && isEating;
    }

    void handleClientTick(final ClientBotTick e) {
        if (CACHE.getPlayerCache().getThePlayer().isAlive()
            && CACHE.getPlayerCache().getGameMode() != GameMode.CREATIVE
            && CACHE.getPlayerCache().getGameMode() != GameMode.SPECTATOR
        ) {
            if (delay > 0) {
                delay--;
                if (isEating) {
                    INPUTS.submit(InputRequest.noInput(this, getPriority()));
                    INVENTORY.submit(InventoryActionRequest.noAction(this, getPriority()));
                }
                return;
            }
            isEating = false;
            if (!swapFuture.isDone()) {
                INPUTS.submit(InputRequest.noInput(this, getPriority()));
                return;
            }
            if (!shouldEatGap()) {
                return;
            }
            var invActionResult = doInventoryActionsV2();
            switch (invActionResult.state()) {
                case ITEM_IN_HAND -> {
                    delay = invActionResult.expectedDelay();
                    startEating();
                    INVENTORY.submit(InventoryActionRequest.noAction(this, getPriority()));
                }
                case SWAPPING -> swapFuture = invActionResult.inventoryActionFuture();
                case NO_ITEM -> {
                }
                default -> throw new IllegalStateException("Unexpected action state: " + invActionResult.state());
            }
        } else {
            isEating = false;
            delay = 0;
        }
    }

    void startEating() {
        if (!isItemEquipped()) return;
        var hand = getHand();
        INPUTS.submit(InputRequest.builder()
                .owner(this)
                .input(Input.builder()
                    .rightClick(true)
                    .hand(hand)
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
        isEating = false;
        swapFuture = RequestFuture.rejected;
    }

    boolean shouldEatGap() {
        return isLowHealth() || shouldEatFromOnFire();
    }

    boolean isLowHealth() {
        return CACHE.getPlayerCache().getThePlayer().getHealth() <= CONFIG.client.extra.autoGap.healthThreshold;
    }

    boolean hasFireResistance() {
        return CACHE.getPlayerCache().getThePlayer().getPotionEffectMap().containsKey(Effect.FIRE_RESISTANCE);
    }

    boolean shouldEatFromOnFire() {
        return CONFIG.client.extra.autoGap.onFire && isPlayerOnFire() && !hasFireResistance();
    }

    boolean isPlayerOnFire() {
        Byte flagsByte = CACHE.getPlayerCache().getThePlayer().getMetadataValue(0, MetadataTypes.BYTE, Byte.class);
        return isOnFireFlags(flagsByte);
    }

    static boolean isOnFireFlags(final Byte flagsByte) {
        return flagsByte != null && (flagsByte & 0x01) != 0;
    }

    @Override
    public boolean itemPredicate(final ItemStack itemStack) {
        final boolean requireEnchantedGap = shouldEatFromOnFire() && !isLowHealth();
        return shouldUseItem(itemStack, requireEnchantedGap, hasEnchantedGapInInventory());
    }

    boolean shouldUseItem(final ItemStack itemStack, final boolean requireEnchantedGap, final boolean hasEnchantedGapInInventory) {
        if (requireEnchantedGap) {
            return isEnchantedGap(itemStack);
        }
        if (isEnchantedGap(itemStack)) return true;
        return isRegularGap(itemStack) && !hasEnchantedGapInInventory;
    }

    boolean hasEnchantedGapInInventory() {
        var inventory = CACHE.getPlayerCache().getPlayerInventory();
        for (int i = 9; i <= 44; i++) {
            if (isEnchantedGap(inventory.get(i))) {
                return true;
            }
        }
        return false;
    }

    boolean isRegularGap(ItemStack itemStack) {
        return itemStack != null && itemStack.getId() == FoodRegistry.GOLDEN_APPLE.id();
    }

    boolean isEnchantedGap(ItemStack itemStack) {
        return itemStack != null && itemStack.getId() == FoodRegistry.ENCHANTED_GOLDEN_APPLE.id();
    }
}
