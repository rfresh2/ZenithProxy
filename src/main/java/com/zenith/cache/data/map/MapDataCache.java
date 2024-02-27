package com.zenith.cache.data.map;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.level.ClientboundMapItemDataPacket;
import com.github.steveice10.packetlib.packet.Packet;
import com.zenith.cache.CachedData;
import lombok.Data;
import lombok.NonNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Data
public class MapDataCache implements CachedData {
    Map<Integer, StoredMapData> mapDataMap = new ConcurrentHashMap<>();

    public void upsert(final ClientboundMapItemDataPacket serverMapDataPacket) {
        mapDataMap.compute(serverMapDataPacket.getMapId(), (key, oldValue) -> {
            if (oldValue == null) {
                return new StoredMapData(
                        serverMapDataPacket.getMapId(),
                        serverMapDataPacket.getScale(),
                        serverMapDataPacket.isLocked());
            } else {
                oldValue.setScale(serverMapDataPacket.getScale());
                oldValue.setLocked(serverMapDataPacket.isLocked());
                oldValue.addIcons(serverMapDataPacket.getIcons());
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
    public void reset(boolean full) {
        if (full) {
            mapDataMap.clear();
        }
    }

    @Override
    public String getSendingMessage() {
        return String.format("Sending %d maps", this.mapDataMap.size());
    }
}
