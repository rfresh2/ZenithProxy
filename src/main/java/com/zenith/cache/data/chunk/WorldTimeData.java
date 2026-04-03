package com.zenith.cache.data.chunk;

import lombok.Data;
import org.geysermc.mcprotocollib.protocol.data.game.level.ClockNetworkState;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundSetTimePacket;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Data
public class WorldTimeData {
    private long lastUpdate;
    private long gameTime;
    private Map<Integer, ClockNetworkState> clockStates = new ConcurrentHashMap<>();

    public void update(ClientboundSetTimePacket packet) {
        this.lastUpdate = System.currentTimeMillis();
        this.gameTime = packet.getGameTime();
        this.clockStates.putAll(packet.getClockUpdates());
    }

    public ClientboundSetTimePacket toPacket() {
        // The amount of ticks that have passed since the last time packet was received.
        final long offset = (System.currentTimeMillis() - this.lastUpdate) / 50;

        long worldAge = this.gameTime;

        if (worldAge > 0) {
            worldAge += offset;
        }

        return new ClientboundSetTimePacket(worldAge, clockStates);
    }
}
