package com.zenith.cache.data.map;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.geysermc.mcprotocollib.protocol.data.game.level.map.MapData;
import org.geysermc.mcprotocollib.protocol.data.game.level.map.MapIcon;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundMapItemDataPacket;

@Data
@AllArgsConstructor
public class StoredMapData {
    private int mapId;
    private byte scale;
    private boolean locked;
    private MapIcon[] icons;
    private byte[] data;

    public StoredMapData(ClientboundMapItemDataPacket packet) {
        this.mapId = packet.getMapId();
        this.scale = packet.getScale();
        this.locked = packet.isLocked();
        this.icons = packet.getIcons();
        this.data = new byte[16384];
        addData(packet.getData());
    }

    public ClientboundMapItemDataPacket getPacket() {
        return new ClientboundMapItemDataPacket(mapId, scale, locked, icons, new MapData(128, 128, 0, 0, data));
    }

    public void addData(final MapData mapData) {
        if (mapData == null) return;
        for (int i = 0; i < mapData.getColumns(); i++) {
            for (int j = 0; j < mapData.getRows(); j++) {
                data[mapData.getX() + i + (mapData.getY() + j) * 128] = mapData.getData()[i + j * mapData.getColumns()];
            }
        }
    }
}
