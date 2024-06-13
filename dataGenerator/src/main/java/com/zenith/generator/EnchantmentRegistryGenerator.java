package com.zenith.generator;

import com.squareup.javapoet.CodeBlock;
import com.zenith.DataGenerator;
import com.zenith.mc.enchantment.EnchantmentData;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.ArrayList;
import java.util.List;

public class EnchantmentRegistryGenerator extends RegistryGenerator<EnchantmentData> {

    public EnchantmentRegistryGenerator() {
        super(EnchantmentData.class, EnchantmentData.class.getPackage().getName(), "EnchantmentRegistry");
    }

    @Override
    public List<EnchantmentData> buildDataList() {
        final List<EnchantmentData> enchants = new ArrayList<>();
        Registry<Enchantment> registry = DataGenerator.SERVER_INSTANCE.registryAccess()
            .registry(Registries.ENCHANTMENT)
            .get();
        registry.stream().forEach(enchant -> enchants.add(new EnchantmentData(
            registry.getId(enchant),
            registry.getKey(enchant).getPath()
        )));
        return enchants;
    }

    @Override
    public CodeBlock dataInitializer(final EnchantmentData enchant) {
        return CodeBlock.of("new $T($L, $S)",
                            this.dataType,
                            enchant.id(),
                            enchant.name());
    }
}
