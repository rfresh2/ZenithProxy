package com.zenith.module.impl;

import com.github.rfresh2.EventConsumer;
import com.zenith.Proxy;
import com.zenith.event.client.ClientBotTick;
import com.zenith.feature.inventory.InventoryActionRequest;
import com.zenith.feature.player.ClickTarget;
import com.zenith.feature.player.Input;
import com.zenith.feature.player.InputRequest;
import com.zenith.mc.block.Direction;
import com.zenith.mc.item.ItemData;
import com.zenith.mc.item.ItemRegistry;
import com.zenith.util.RequestFuture;
import com.zenith.util.math.MathHelper;
import com.zenith.util.timer.Timer;
import com.zenith.util.timer.Timers;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import org.geysermc.mcprotocollib.protocol.data.game.entity.Effect;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.GameMode;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.PlayerAction;
import org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundPlayerActionPacket;

import java.util.List;
import java.util.Objects;

import static com.github.rfresh2.EventConsumer.of;
import static com.zenith.Globals.*;

public class AutoOmen extends AbstractInventoryModule {
    private int delay = 0;
    private boolean isEating = false;
    private static final List<Effect> OMEN_EFFECTS = List.of(
        Effect.BAD_OMEN,
        Effect.RAID_OMEN,
        Effect.TRIAL_OMEN
    );
    private final Timer omenActiveTimer = Timers.tickTimer();
    private final Timer raidActiveTimer = Timers.tickTimer();
    private final Timer constantTimer = Timers.tickTimer();
    RequestFuture swapFuture = RequestFuture.rejected;

    public AutoOmen() {
        super(HandRestriction.EITHER, 3);
    }

    @Override
    public boolean enabledSetting() {
        return CONFIG.client.extra.autoOmen.enabled;
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
        return Objects.requireNonNullElse(CONFIG.client.extra.autoOmen.priority, 10000);
    }

    public void handleClientTick(final ClientBotTick e) {
        if (!CACHE.getPlayerCache().getThePlayer().isAlive()
            || CACHE.getPlayerCache().getGameMode() == GameMode.CREATIVE
            || CACHE.getPlayerCache().getGameMode() == GameMode.SPECTATOR
        ) {
            isEating = false;
            delay = 0;
            constantTimer.reset();
            return;
        }
        switch (CONFIG.client.extra.autoOmen.mode) {
            case EFFECT_AND_RAID_INACTIVE -> {
                if (hasOmenEffect()) {
                    omenActiveTimer.reset();
                }
                if (isRaidActive()) {
                    raidActiveTimer.reset();
                }
                // grace period for server to send us updated states. e.g. from drink until omen effect packet
                var stateChangeGracePeriodTicks = MathHelper.ceilI((Proxy.getInstance().getClient().getPing() / 50.0) * 2) + MathHelper.ceilI(((20.0 / TPS.getTPSValue()) * 10));
                var raidActive = !raidActiveTimer.tick(stateChangeGracePeriodTicks, false);
                var omenActive = !omenActiveTimer.tick(stateChangeGracePeriodTicks, false);
                if (raidActive || omenActive) {
                    if (isEating) {
                        sendClientPacketAsync(new ServerboundPlayerActionPacket(
                            PlayerAction.RELEASE_USE_ITEM,
                            0, 0, 0,
                            Direction.DOWN.mcpl(),
                            0
                        ));
                        debug("Cancelling omen drink because omen or raid now active");
                    }
                    delay = 0;
                    isEating = false;
                    return;
                }
            }
            case CONSTANT -> {
                if (!constantTimer.tick(CONFIG.client.extra.autoOmen.constantTicks, false)) {
                    delay = 0;
                    isEating = false;
                    return;
                }
            }
        }

        if (delay > 0) {
            delay--;
            if (isEating) {
                INPUTS.submit(InputRequest.noInput(this, getPriority()));
                INVENTORY.submit(InventoryActionRequest.noAction(this, getPriority()));
            }
            return;
        }
        if (isEating) {
            // we completed eating successfully
            constantTimer.reset();
        }
        isEating = false;
        if (!swapFuture.isDone()) {
            INPUTS.submit(InputRequest.noInput(this, getPriority()));
            return;
        }
        var invActionResult = doInventoryActionsV2();
        switch (invActionResult.state()) {
            case ITEM_IN_HAND -> {
                delay = invActionResult.expectedDelay();
                startEating(); // if accepted, will set delay to 50 (the eating duration ticks)
                INVENTORY.submit(InventoryActionRequest.noAction(this, getPriority()));
            }
            case NO_ITEM -> {}
            case SWAPPING -> {
                swapFuture = invActionResult.inventoryActionFuture();
            }
        }
    }

    public void startEating() {
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
                debug("Drinking Omen");
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
        raidActiveTimer.reset();
        omenActiveTimer.reset();
        constantTimer.reset();
        swapFuture = RequestFuture.rejected;
    }

    @Override
    public boolean itemPredicate(final ItemStack itemStack) {
        ItemData itemData = ItemRegistry.REGISTRY.get(itemStack.getId());
        return itemData != null
            && itemData == ItemRegistry.OMINOUS_BOTTLE
            && (CONFIG.client.extra.autoOmen.consumeFullOmenStack || itemStack.getAmount() > 1);
    }

    private boolean hasOmenEffect() {
        for (int i = 0; i < OMEN_EFFECTS.size(); i++) {
            if (CACHE.getPlayerCache().getThePlayer().getPotionEffectMap().containsKey(OMEN_EFFECTS.get(i))) {
                return true;
            }
        }
        return false;
    }

    private boolean isRaidActive() {
        for (var bossBar : CACHE.getBossBarCache().getBossBars().values()) {
            if (isRaidActiveComponent(bossBar.getTitle())) return true;
        }
        return false;
    }

    private boolean isRaidActiveComponent(final Component component) {
        if (component instanceof TranslatableComponent translatableComponent) {
            var key = translatableComponent.key();
            return key.startsWith("event.minecraft.raid") && !key.contains("victory");
        } else {
            for (var child : component.children()) {
                if (isRaidActiveComponent(child)) {
                    return true;
                }
            }
        }
        return false;
    }
}
