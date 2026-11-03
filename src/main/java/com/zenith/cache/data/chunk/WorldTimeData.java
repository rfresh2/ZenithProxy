package com.zenith.cache.data.chunk;

import com.zenith.util.math.MathHelper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.geysermc.mcprotocollib.protocol.data.game.level.ClockNetworkState;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundSetTimePacket;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Data
public class WorldTimeData {
    private long lastUpdate;
    private long gameTime;
    private long lastTickGameTime;
    private Map<Integer, ClockState> clockStates = new ConcurrentHashMap<>();

    public void update(ClientboundSetTimePacket packet) {
        this.lastUpdate = System.currentTimeMillis();
        tick(packet.getGameTime());
        for (var entry : packet.getClockUpdates().entrySet()) {
            clockStates.put(entry.getKey(), ClockState.fromNetworkState(entry.getValue()));
        }
    }

    public ClockState getClockInstance(int id) {
        return this.clockStates.computeIfAbsent(id, k -> new ClockState());
    }

    public void tick(long gameTime) {
        long gameTimeDelta = gameTime - this.lastTickGameTime;
        this.lastTickGameTime = gameTime;

        for (var clock : this.clockStates.values()) {
            double newPartialTicks = clock.partialTick + (double)gameTimeDelta * clock.rate;
            long fullTicks = MathHelper.floorI(newPartialTicks);
            clock.partialTick = (float)(newPartialTicks - fullTicks);
            clock.totalTicks += fullTicks;
        }
        this.gameTime = gameTime;
    }

    public ClientboundSetTimePacket toPacket() {
        // The amount of ticks that have passed since the last time packet was received.
        final long offset = (System.currentTimeMillis() - this.lastUpdate) / 50;

        long worldAge = this.lastTickGameTime;

        if (worldAge > 0) {
            worldAge += offset;
        }
        var networkClocks = new LinkedHashMap<Integer, ClockNetworkState>();
        for (var entry : this.clockStates.entrySet()) {
            networkClocks.put(entry.getKey(), entry.getValue().toNetworkState());
        }
        return new ClientboundSetTimePacket(worldAge, networkClocks);
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Accessors(fluent = true)
    public static class ClockState {
        private long totalTicks = 0;
        private float partialTick = 0;
        private float rate = 1.0f;

        public static ClockState fromNetworkState(ClockNetworkState networkState) {
            return new ClockState(networkState.totalTicks(), networkState.partialTick(), networkState.rate());
        }

        public ClockNetworkState toNetworkState() {
            return new ClockNetworkState(totalTicks, partialTick, rate);
        }
    }
}
