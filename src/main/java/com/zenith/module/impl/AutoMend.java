package com.zenith.module.impl;

import com.zenith.event.module.ClientBotTick;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.EnchantmentType;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.ItemStack;

import static com.github.rfresh2.EventConsumer.of;
import static com.zenith.Shared.*;

public class AutoMend extends AbstractInventoryModule {

    int delay = 0;

    public AutoMend() {
        super(true, 0, 50);
    }

    @Override
    public void subscribeEvents() {
        EVENT_BUS.subscribe(
            this,
            of(ClientBotTick.class, this::handleBotTick)
        );
    }

    private void handleBotTick(ClientBotTick event) {
        if (delay > 0) {
            delay--;
            return;
        }
        if (CACHE.getPlayerCache().getThePlayer().isAlive()) {
            delay = doInventoryActions();
        }
    }

    @Override
    public boolean shouldBeEnabled() {
        return CONFIG.client.extra.autoMend.enabled;
    }

    @Override
    public boolean itemPredicate(final ItemStack itemStack) {
        if (itemStack.getNbt() == null) return false;
        if (!itemStack.getEnchantments().containsKey(EnchantmentType.MENDING)) return false;
        var nbt = itemStack.getCompoundTag();
        if (!nbt.contains("Damage")) return false;
        var damage = nbt.getInt("Damage");
        return damage > 0;
    }
}
