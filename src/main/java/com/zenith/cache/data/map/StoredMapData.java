package com.zenith.cache.data.map;

import com.github.steveice10.mc.protocol.data.game.world.map.MapData;
import com.github.steveice10.mc.protocol.data.game.world.map.MapIcon;
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerMapDataPacket;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Objects.nonNull;

@AllArgsConstructor
@Getter
@Setter
public class StoredMapData {
    private int mapId;
    private byte scale;
    private boolean trackingPosition;
    private Set<MapIcon> icons;
    private byte[] data;

    public StoredMapData(final int mapId, final byte scale, final boolean trackingPosition) {
        this.mapId = mapId;
        this.scale = scale;
        this.trackingPosition = trackingPosition;
        this.icons = new HashSet<>();
        this.data = new byte[16384];
    }

    public ServerMapDataPacket getPacket() {
        return new ServerMapDataPacket(mapId, scale, trackingPosition, icons.toArray(new MapIcon[0]), new MapData(128, 128, 0, 0, data));
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
        this.getIcons().addAll(Arrays.stream(icons).collect(Collectors.toSet()));
    }
}
