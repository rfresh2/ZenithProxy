package com.zenith.mc.item;

import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentTypes;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the assumption the bot's use state depends on: the bundled item data must carry the
 * consumable component, otherwise a food item would be mistaken for a held-open use and the bot
 * would stay slowed forever.
 */
public class ItemUseDataTest {

    @Test
    public void foodItemsExposeConsumableComponent() {
        final ItemData apple = ItemRegistry.APPLE;
        assertNotNull(apple, "apple item data");
        final var consumable = apple.components().get(DataComponentTypes.CONSUMABLE);
        assertNotNull(consumable, "apple must have a consumable component");
        assertTrue(consumable.consumeSeconds() > 0,
            "apple consume time must be positive, was " + consumable.consumeSeconds());
    }

    @Test
    public void bowIsNotConsumable() {
        // a bow stays in use until released, so it must not report a finite consume duration
        final ItemData bow = ItemRegistry.BOW;
        assertNotNull(bow, "bow item data");
        assertTrue(bow.components().get(DataComponentTypes.CONSUMABLE) == null,
            "bow should not have a consumable component");
    }
}

