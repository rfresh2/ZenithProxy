package com.zenith.cache.data.map;

import com.zenith.cache.CacheResetType;
import com.zenith.cache.CachedData;
import lombok.Data;
import lombok.NonNull;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundMapItemDataPacket;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Data
public class MapDataCache implements CachedData {
    Map<Integer, StoredMapData> mapDataMap = new ConcurrentHashMap<>();

    public void upsert(final ClientboundMapItemDataPacket serverMapDataPacket) {
        mapDataMap.compute(serverMapDataPacket.getMapId(), (key, oldValue) -> {
            if (oldValue == null) {
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
    public void getPackets(@NonNull Consumer<Packet> consumer) {
        mapDataMap.values().forEach(storedMapData -> consumer.accept(storedMapData.getPacket()));
    }

    @Override
    public void reset(CacheResetType type) {
        if (type == CacheResetType.FULL || type == CacheResetType.LOGIN || type == CacheResetType.PROTOCOL_SWITCH) {
            mapDataMap.clear();
        }
    }

    @Override
    public String getSendingMessage() {
        return String.format("Sending %d maps", this.mapDataMap.size());
    }
}
