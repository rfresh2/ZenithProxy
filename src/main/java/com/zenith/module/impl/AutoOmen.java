package com.zenith.module.impl;

import com.github.rfresh2.EventConsumer;
import com.zenith.event.client.ClientBotTick;
import com.zenith.feature.inventory.InventoryActionRequest;
import com.zenith.feature.player.ClickTarget;
import com.zenith.feature.player.Input;
import com.zenith.feature.player.InputRequest;
import com.zenith.mc.block.Direction;
import com.zenith.mc.item.ItemData;
import com.zenith.mc.item.ItemRegistry;
import com.zenith.util.RequestFuture;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import org.geysermc.mcprotocollib.protocol.data.game.entity.Effect;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.GameMode;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.PlayerAction;
import org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundPlayerActionPacket;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

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
    private long lastHadOmen = 0L;
    private long lastRaidActive = 0L;
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
        if (hasOmenEffect()) {
            lastHadOmen = System.nanoTime();
        }
        if (isRaidActive()) {
            lastRaidActive = System.nanoTime();
        }
        if (CACHE.getPlayerCache().getThePlayer().isAlive()
            && CACHE.getPlayerCache().getGameMode() != GameMode.CREATIVE
            && CACHE.getPlayerCache().getGameMode() != GameMode.SPECTATOR
        ) {
            var raidActive = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - lastRaidActive) <= CONFIG.client.extra.autoOmen.raidCooldownMs;
            var omenActive = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - lastHadOmen) <= CONFIG.client.extra.autoOmen.omenCooldownMs;
            var omenOrRaidActive = ((raidActive && !CONFIG.client.extra.autoOmen.whileRaidActive) || (omenActive && !CONFIG.client.extra.autoOmen.whileOmenActive));
            if (delay > 0) {
                delay--;
                if (isEating) {
                    if (omenOrRaidActive) {
                        sendClientPacketAsync(new ServerboundPlayerActionPacket(
                            PlayerAction.RELEASE_USE_ITEM,
                            0, 0, 0,
                            Direction.DOWN.mcpl(),
                            0
                        ));
                        debug("Cancelling omen drink because omen or raid now active");
                        delay = 0;
                        isEating = false;
                        return;
                    }
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
            if (omenOrRaidActive) {
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
                default -> throw new IllegalStateException("Unexpected action state: " + invActionResult.state());
            }
        } else {
            isEating = false;
            delay = 0;
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
        lastHadOmen = 0L;
        lastRaidActive = 0L;
        swapFuture = RequestFuture.rejected;
    }

    @Override
    public boolean itemPredicate(final ItemStack itemStack) {
        ItemData itemData = ItemRegistry.REGISTRY.get(itemStack.getId());
        return itemData != null && itemData == ItemRegistry.OMINOUS_BOTTLE;
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
