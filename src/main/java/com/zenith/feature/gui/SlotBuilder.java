package com.zenith.feature.gui;

import com.zenith.feature.api.sessionserver.SessionServerApi;
import com.zenith.feature.gui.elements.*;
import com.zenith.mc.item.ItemData;
import com.zenith.mc.item.ItemRegistry;
import net.kyori.adventure.text.Component;
import org.geysermc.mcprotocollib.auth.GameProfile;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.ResolvableProfile;
import org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentType;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentTypes;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponents;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import static com.zenith.Globals.SERVER_LOG;

@NullMarked
public class SlotBuilder {
    private int itemId = 0;
    private int amount = 1;
    private @Nullable ButtonClickHandler buttonClickHandler = null;
    private ItemSlotTickHandler itemSlotTickHandler = ItemSlot.emptyTickHandler;
    private final DataComponents dataComponents = new DataComponents(new HashMap<>());

    private SlotBuilder() {}

    public static SlotBuilder create() {
        return new SlotBuilder();
    }

    public SlotBuilder item(int itemId) {
        this.itemId = itemId;
        return this;
    }

    public SlotBuilder item(ItemData itemData) {
        this.itemId = itemData.id();
        return this;
    }

    public SlotBuilder amount(int amount) {
        this.amount = amount;
        return this;
    }

    public SlotBuilder name(Component name) {
        return dataComponent(DataComponentTypes.ITEM_NAME, name);
    }

    public SlotBuilder lore(List<Component> lore) {
        return dataComponent(DataComponentTypes.LORE, lore);
    }

    /**
     * Looking for player head textures? try these: <a href="https://minecraft-heads.com/">minecraft-heads</a>
     */
    public SlotBuilder playerHead(String playerName, String customTextureData) {
        item(ItemRegistry.PLAYER_HEAD);
        var profile = new GameProfile(UUID.randomUUID(), playerName);
        profile.setProperties(List.of(
            new GameProfile.Property("textures", customTextureData)
        ));
        dataComponent(DataComponentTypes.PROFILE, new ResolvableProfile(profile));
        return this;
    }

    /**
     * WARNING: makes a blocking network request to retrieve the player's skin and name
     */
    @ApiStatus.Experimental
    public SlotBuilder playerHead(UUID uuid) {
        item(ItemRegistry.PLAYER_HEAD);
        var profileAndSkin = SessionServerApi.INSTANCE.getProfileAndSkin(uuid);
        if (profileAndSkin.isPresent()) {
            var profile = profileAndSkin.get();
            dataComponent(DataComponentTypes.PROFILE, new ResolvableProfile(profile));
        } else {
            SERVER_LOG.error("Failed getting player head skin for uuid: {}", uuid);
            dataComponent(DataComponentTypes.PROFILE, new ResolvableProfile(new GameProfile(uuid, uuid.toString())));
        }
        return this;
    }

    public <T> SlotBuilder dataComponent(DataComponentType<T> dataComponentType, @Nullable T data) {
        dataComponents.put(dataComponentType, data);
        return this;
    }

    public SlotBuilder tickHandler(ItemSlotTickHandler itemSlotTickHandler) {
        this.itemSlotTickHandler = itemSlotTickHandler;
        return this;
    }

    public SlotBuilder buttonClickHandler(ButtonClickHandler handler) {
        this.buttonClickHandler = handler;
        return this;
    }

    public SlotBuilder nextPageButton() {
        return buttonClickHandler((button, gui, page, containerClick) -> {
            if (containerClick.isLeftOrRightClick()) {
                gui.nextPage();
            }
        });
    }

    public SlotBuilder previousPageButton() {
        return buttonClickHandler((button, gui, page, containerClick) -> {
            if (containerClick.isLeftOrRightClick()) {
                gui.previousPage();
            }
        });
    }

    public Slot build() {
        var itemStack = new ItemStack(itemId, amount, dataComponents);
        if (buttonClickHandler != null) {
            return new Button(itemStack, buttonClickHandler, itemSlotTickHandler);
        } else {
            return new ItemSlot(itemStack, itemSlotTickHandler);
        }
    }
}
