package com.zenith.cache.data.chunk;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.level.ClientboundSetTimePacket;

public class WorldTimeData {
    private long lastUpdate;
    private long worldAge;
    private long time;

    public void update(ClientboundSetTimePacket packet) {
        this.lastUpdate = System.nanoTime();
        this.worldAge = packet.getWorldAge();
        this.time = packet.getTime();
    }

    public ClientboundSetTimePacket toPacket() {
        // The amount of ticks that have passed since the last time packet was received.
        final long offset = (System.nanoTime() - this.lastUpdate) / 50000000;

        long worldAge = this.worldAge;

        if (worldAge > 0) {
            worldAge += offset;
        }

        long time = this.time;

        // If time is negative, the daylight cycle is disabled (e.g. from the "doDaylightCycle" gamerule being false)
        if (time >= 0) {
            time += offset;
            time %= 24000;
        }

        return new ClientboundSetTimePacket(worldAge, time);
    }
}
