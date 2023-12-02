package com.zenith.cache.data.config;

import com.github.steveice10.mc.protocol.packet.common.clientbound.ClientboundResourcePackPushPacket;
import com.github.steveice10.mc.protocol.packet.common.clientbound.ClientboundUpdateTagsPacket;
import com.github.steveice10.mc.protocol.packet.configuration.clientbound.ClientboundRegistryDataPacket;
import com.github.steveice10.mc.protocol.packet.configuration.clientbound.ClientboundUpdateEnabledFeaturesPacket;
import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.github.steveice10.packetlib.packet.Packet;
import com.zenith.cache.CachedData;
import lombok.Data;
import lombok.NonNull;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Configuration phase registry and features cache
 */
@Data
public class ConfigurationCache implements CachedData {

    private CompoundTag registry = new CompoundTag(":");
    protected String[] enabledFeatures = new String[]{"minecraft:vanilla"};
    private Map<UUID, ResourcePack> resourcePacks = new ConcurrentHashMap<>();
    private Map<String, Map<String, int[]>> tags = new ConcurrentHashMap<>();

    @Override
    public void getPackets(@NonNull final Consumer<Packet> consumer) {
        consumer.accept(new ClientboundRegistryDataPacket(this.registry));
        consumer.accept(new ClientboundUpdateEnabledFeaturesPacket(this.enabledFeatures));
        resourcePacks.forEach((uuid, resourcePack) -> consumer.accept(new ClientboundResourcePackPushPacket(
            resourcePack.id(),
            resourcePack.url(),
            resourcePack.hash(),
            resourcePack.required(),
            resourcePack.prompt()
        )));
        consumer.accept(new ClientboundUpdateTagsPacket(this.tags));
    }

    @Override
    public void reset(final boolean full) {
        if (full) {
            this.registry = new CompoundTag(":");
            this.enabledFeatures = new String[]{"minecraft:vanilla"};
            this.resourcePacks = new ConcurrentHashMap<>();
            this.tags = new ConcurrentHashMap<>();
        }
    }
}
