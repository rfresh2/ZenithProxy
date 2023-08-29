package com.zenith.cache.data.map;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.level.ClientboundMapItemDataPacket;
import com.github.steveice10.packetlib.packet.Packet;
import com.zenith.cache.CachedData;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import lombok.NonNull;

import java.util.Collections;
import java.util.Map;
import java.util.function.Consumer;

public class MapDataCache implements CachedData {
    Map<Integer, StoredMapData> mapDataMap = Collections.synchronizedMap(new Int2ObjectOpenHashMap<>());

    public void upsert(final ClientboundMapItemDataPacket serverMapDataPacket) {
        if (mapDataMap.containsKey(serverMapDataPacket.getMapId())) {
            mapDataMap.computeIfPresent(serverMapDataPacket.getMapId(), (key, oldValue) -> {
                oldValue.setScale(serverMapDataPacket.getScale());
                oldValue.setLocked(serverMapDataPacket.isLocked());
                oldValue.addIcons(serverMapDataPacket.getIcons());
                oldValue.addData(serverMapDataPacket.getData());
                return oldValue;
            });
        } else {
            mapDataMap.put(serverMapDataPacket.getMapId(), new StoredMapData(
                    serverMapDataPacket.getMapId(),
                    serverMapDataPacket.getScale(),
                    serverMapDataPacket.isLocked()));
            StoredMapData storedMapData = mapDataMap.get(serverMapDataPacket.getMapId());
            storedMapData.addIcons(serverMapDataPacket.getIcons());
            storedMapData.addData(serverMapDataPacket.getData());
        }
    }

    @Override
    public void getPackets(@NonNull Consumer<Packet> consumer) {
        mapDataMap.values().forEach(storedMapData -> consumer.accept(storedMapData.getPacket()));
    }

    @Override
    public void reset(boolean full) {
        if (full) {
            mapDataMap = new Int2ObjectOpenHashMap<>();
        }
    }

    @Override
    public String getSendingMessage() {
        return String.format("Sending %d maps", this.mapDataMap.size());
    }
}
