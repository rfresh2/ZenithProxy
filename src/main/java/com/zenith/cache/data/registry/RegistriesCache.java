package com.zenith.cache.data.registry;

import com.viaversion.nbt.io.MNBTIO;
import com.viaversion.nbt.tag.CompoundTag;
import com.viaversion.nbt.tag.StringTag;
import com.zenith.cache.CacheResetType;
import com.zenith.cache.CachedData;
import com.zenith.mc.Registry;
import com.zenith.mc.biome.Biome;
import com.zenith.mc.biome.BiomeRegistry;
import com.zenith.mc.chat_type.ChatType;
import com.zenith.mc.chat_type.ChatTypeRegistry;
import com.zenith.mc.damage_type.DamageType;
import com.zenith.mc.damage_type.DamageTypeRegistry;
import com.zenith.mc.dimension.DimensionData;
import com.zenith.mc.dimension.DimensionRegistry;
import com.zenith.mc.enchantment.EnchantmentData;
import com.zenith.mc.enchantment.EnchantmentRegistry;
import lombok.Data;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.network.tcp.TcpSession;
import org.geysermc.mcprotocollib.protocol.data.game.RegistryEntry;
import org.geysermc.mcprotocollib.protocol.packet.configuration.clientbound.ClientboundRegistryDataPacket;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import static com.zenith.Globals.CACHE_LOG;

@NullMarked
@Data
public class RegistriesCache implements CachedData {
    // data directly as sent by the server
    // zenith's internal registry classes store a slimmed down version of select registry types
    private final Map<String, List<RegistryEntry>> registryEntries = new ConcurrentHashMap<>();

    public void initialize(String registryName, List<RegistryEntry> entries) {
        this.registryEntries.put(registryName, entries);
        try {
            switch (registryName) {
                case "minecraft:dimension_type" -> initializeDimensionTypeRegistry(entries);
                case "minecraft:chat_type" -> initializeChatTypeRegistry(entries);
                case "minecraft:enchantment" -> initializeEnchantmentRegistry(entries);
                case "minecraft:worldgen/biome" -> initializeBiomeRegistry(entries);
                case "minecraft:damage_type" -> initializeDamageTypeRegistry(entries);
                default -> {}
            }
        } catch (Exception e) {
            CACHE_LOG.error("Error initializing registry: {}", registryName, e);
        }
    }

    private void initializeDamageTypeRegistry(final List<RegistryEntry> entries) {
        Registry<DamageType> registry = new Registry<>(entries.size());
        for (int i = 0; i < entries.size(); i++) {
            var entry = entries.get(i);
            String key = getKey(entry);
            var damageType = new DamageType(i, key);
            registry.register(damageType);
        }
        CACHE_LOG.debug("Loaded {} damage types", registry.size());
        DamageTypeRegistry.REGISTRY.set(registry);
    }

    private void initializeEnchantmentRegistry(final List<RegistryEntry> entries) {
        Registry<EnchantmentData> registry = new Registry<>(entries.size());
        for (int i = 0; i < entries.size(); i++) {
            final var entry = entries.get(i);
            String key = getKey(entry);
            if (entry.getData() == null) {
                CACHE_LOG.error("Null data for enchantment registry key: {}", key);
                continue;
            }
            var nbt = (CompoundTag) MNBTIO.read(entry.getData());
            int maxLevel = nbt.getInt("max_level");
            var enchantData = new EnchantmentData(i, key, maxLevel);
            registry.register(enchantData);
        }
        CACHE_LOG.debug("Loaded {} enchantments", registry.size());
        EnchantmentRegistry.REGISTRY.set(registry);
    }

    private void initializeDimensionTypeRegistry(List<RegistryEntry> entries) {
        Registry<DimensionData> registry = new Registry<>(entries.size());
        for (int i = 0; i < entries.size(); i++) {
            final var entry = entries.get(i);
            String key = getKey(entry);
            if (entry.getData() == null) {
                CACHE_LOG.error("Null data for dimension registry key: {}", key);
                continue;
            }
            CompoundTag dataTag = (CompoundTag) MNBTIO.read(entry.getData());
            int minY = dataTag.getInt("min_y");
            int height = dataTag.getInt("height");
            int buildHeight = minY + height;
            DimensionData dimensionData = new DimensionData(i, key, minY, buildHeight, height);
            registry.register(dimensionData);
        }
        CACHE_LOG.debug("Loaded {} dimensions", registry.size());
        DimensionRegistry.REGISTRY.set(registry);
    }

    private void initializeChatTypeRegistry(List<RegistryEntry> entries) {
        Registry<ChatType> registry = new Registry<>(entries.size());
        for (int i = 0; i < entries.size(); i++) {
            final var entry = entries.get(i);
            String key = getKey(entry);
            if (entry.getData() == null) {
                CACHE_LOG.error("Null data for chat type registry key: {}", key);
                continue;
            }
            var topTag = (CompoundTag) MNBTIO.read(entry.getData());
            var chatTag = topTag.getCompoundTag("chat");
            var translationKey = chatTag.getString("translation_key");
            var parametersList = chatTag.getListTag("parameters", StringTag.class);
            var parameters = parametersList.stream().map(StringTag::getValue).toList();
            var chatData = new ChatType(i, key, translationKey, parameters);
            registry.register(chatData);
        }
        ChatTypeRegistry.REGISTRY.set(registry);
    }

    private void initializeBiomeRegistry(final List<RegistryEntry> entries) {
        Registry<Biome> registry = new Registry<>(entries.size());
        for (int i = 0; i < entries.size(); i++) {
            final var entry = entries.get(i);
            String key = getKey(entry);
            if (entry.getData() == null) {
                CACHE_LOG.error("Null data for biome registry key: {}", key);
                continue;
            }
            var biome = new Biome(i, key);
            registry.register(biome);
        }
        BiomeRegistry.REGISTRY.set(registry);
    }

    @Override
    public void getPackets(@NonNull final Consumer<Packet> consumer, final @NonNull TcpSession session) {}

    public void getRegistryPackets(@NonNull final Consumer<Packet> consumer, final @NonNull TcpSession session) {
        registryEntries.forEach((registry, entries) -> consumer.accept(new ClientboundRegistryDataPacket(registry, entries)));
    }

    private String getKey(RegistryEntry entry) {
        String key = entry.getId();
        if (key.contains(":")) {
            key = key.split(":")[1];
        }
        return key;
    }

    @Override
    public void reset(final CacheResetType type) {
        if (type == CacheResetType.FULL) {
            this.registryEntries.clear();
            DimensionRegistry.REGISTRY.reset();
            EnchantmentRegistry.REGISTRY.reset();
            ChatTypeRegistry.REGISTRY.reset();
            BiomeRegistry.REGISTRY.reset();
            DamageTypeRegistry.REGISTRY.reset();
        }
    }
}
