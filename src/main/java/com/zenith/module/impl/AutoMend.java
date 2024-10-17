package com.zenith.module.impl;

import com.zenith.event.module.ClientBotTick;
import com.zenith.mc.enchantment.EnchantmentRegistry;
import org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentType;

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
    public boolean enabledSetting() {
        return CONFIG.client.extra.autoMend.enabled;
    }

    @Override
    public boolean itemPredicate(final ItemStack itemStack) {
        var dataComponents = itemStack.getDataComponents();
        if (dataComponents == null) return false;
        var enchantmentComponents = dataComponents.get(DataComponentType.ENCHANTMENTS);
        if (enchantmentComponents == null) return false;
        if (!enchantmentComponents.getEnchantments().containsKey(EnchantmentRegistry.MENDING.id())) return false;
        var damageComponent = dataComponents.get(DataComponentType.DAMAGE);
        if (damageComponent == null) return false;
        return damageComponent > 0;
    }
}
