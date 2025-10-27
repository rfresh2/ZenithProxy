package com.zenith.cache.data.map;

import com.zenith.cache.CacheResetType;
import com.zenith.cache.CachedData;
import lombok.Data;
import lombok.experimental.Accessors;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.network.tcp.TcpSession;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundMapItemDataPacket;
import org.jspecify.annotations.NonNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import static com.zenith.Globals.CACHE_LOG;
import static com.zenith.Globals.CONFIG;

@Data
@Accessors(chain = true)
public class MapDataCache implements CachedData {
    Map<Integer, StoredMapData> mapDataMap = new ConcurrentHashMap<>();

    public void upsert(final ClientboundMapItemDataPacket serverMapDataPacket) {
        mapDataMap.compute(serverMapDataPacket.getMapId(), (key, oldValue) -> {
            if (oldValue == null) {
                if (mapDataMap.size() > CONFIG.debug.server.cache.maxCachedMaps) {
                    CACHE_LOG.debug("Map cache size limit: {} reached, skipping map: {}", CONFIG.debug.server.cache.maxCachedMaps, serverMapDataPacket.getMapId());
                    return null;
                }
                return new StoredMapData(serverMapDataPacket);
            } else {
                oldValue.setScale(serverMapDataPacket.getScale());
                oldValue.setLocked(serverMapDataPacket.isLocked());
                // map icons will not be resent by the server unless changed
                if (serverMapDataPacket.getIcons().length != 0)
                    oldValue.setIcons(serverMapDataPacket.getIcons());
                oldValue.addData(serverMapDataPacket.getData());
                return oldValue;
            }
        });
    }

    @Override
    public void getPackets(@NonNull Consumer<Packet> consumer, final @NonNull TcpSession session) {
        mapDataMap.values().forEach(storedMapData -> consumer.accept(storedMapData.getPacket()));
    }

    @Override
    public void reset(CacheResetType type) {
        mapDataMap.clear();
    }

    @Override
    public String getSendingMessage() {
        return String.format("Sending %d maps", this.mapDataMap.size());
    }
}
