package com.zenith.cache.data.config;

import com.zenith.cache.CacheResetType;
import com.zenith.cache.CachedData;
import lombok.Data;
import lombok.experimental.Accessors;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.network.tcp.TcpSession;
import org.geysermc.mcprotocollib.protocol.packet.common.clientbound.ClientboundUpdateTagsPacket;
import org.geysermc.mcprotocollib.protocol.packet.configuration.clientbound.ClientboundUpdateEnabledFeaturesPacket;
import org.jspecify.annotations.NonNull;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Configuration phase registry and features cache
 */
@Data
@Accessors(chain = true)
public class ConfigurationCache implements CachedData {
    protected String[] enabledFeatures = new String[]{"minecraft:vanilla"};
    private Map<UUID, ResourcePack> resourcePacks = new ConcurrentHashMap<>();
    private Map<String, Map<String, int[]>> tags = new ConcurrentHashMap<>();

    @Override
    public void getPackets(@NonNull final Consumer<Packet> consumer, final @NonNull TcpSession session) {}

    public void getConfigurationPackets(@NonNull final Consumer<Packet> consumer, final @NonNull TcpSession session) {
        consumer.accept(new ClientboundUpdateEnabledFeaturesPacket(this.enabledFeatures));
        consumer.accept(new ClientboundUpdateTagsPacket(this.tags));
        // resource pack sync to players handled by com.zenith.network.server.ResourcePackSync
    }

    @Override
    public void reset(CacheResetType type) {
        if (type == CacheResetType.FULL) {
            this.enabledFeatures = new String[]{"minecraft:vanilla"};
            this.resourcePacks = new ConcurrentHashMap<>();
            this.tags = new ConcurrentHashMap<>();
        }
    }
}
