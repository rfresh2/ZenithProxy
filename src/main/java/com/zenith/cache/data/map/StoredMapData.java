package com.zenith.cache.data.map;

import com.github.steveice10.mc.protocol.data.game.level.map.MapData;
import com.github.steveice10.mc.protocol.data.game.level.map.MapIcon;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.level.ClientboundMapItemDataPacket;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.HashSet;
import java.util.Set;

import static java.util.Objects.nonNull;

@Data
@AllArgsConstructor
public class StoredMapData {
    private int mapId;
    private byte scale;
    private boolean locked;
    private Set<MapIcon> icons;
    private byte[] data;

    public StoredMapData(final int mapId, final byte scale, final boolean locked) {
        this.mapId = mapId;
        this.scale = scale;
        this.locked = locked;
        this.icons = new HashSet<>();
        this.data = new byte[16384];
    }

    public ClientboundMapItemDataPacket getPacket() {
        return new ClientboundMapItemDataPacket(mapId, scale, locked, icons.toArray(new MapIcon[0]), new MapData(128, 128, 0, 0, data));
    }

    public void addData(final MapData mapData) {
        if (nonNull(mapData)) {
            for (int i = 0; i < mapData.getColumns(); i++) {
                for (int j = 0; j < mapData.getRows(); j++) {
                    data[mapData.getX() + i + (mapData.getY() + j) * 128] = mapData.getData()[i + j * mapData.getColumns()];
                }
            }
        }
    }

    public void addIcons(final MapIcon[] icons) {
        for (MapIcon icon : icons) {
            getIcons().add(icon);
        }
    }
}
