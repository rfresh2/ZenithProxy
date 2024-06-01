package com.zenith.generator;

import com.squareup.javapoet.CodeBlock;
import com.zenith.mc.item.ItemData;
import net.minecraft.core.DefaultedRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.List;

public class ItemRegistryGenerator extends RegistryGenerator<ItemData> {
    public ItemRegistryGenerator() {
        super(ItemData.class, ItemData.class.getPackage().getName(), "ItemRegistry");
    }

    @Override
    public List<ItemData> buildDataList() {
        final List<ItemData> items = new ArrayList<>();
        DefaultedRegistry<Item> registry = BuiltInRegistries.ITEM;
        registry.stream().forEach(item -> items.add(new ItemData(
            registry.getId(item),
            registry.getKey(item).getPath(),
            item.getMaxStackSize())
        ));
        return items;
    }

    @Override
    public CodeBlock dataInitializer(final ItemData item) {
        return CodeBlock.of("new $T($L, $S, $L)",
                            this.dataType,
                            item.id(),
                            item.name(),
                            item.stackSize());
    }
}
