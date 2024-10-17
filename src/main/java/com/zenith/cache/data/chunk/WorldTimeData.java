package com.zenith.cache.data.chunk;

import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundSetTimePacket;

// todo: verify correct
public class WorldTimeData {
    private long lastUpdate;
    private long gameTime;
    private long dayTime;
    private boolean tickDayTime;

    public void update(ClientboundSetTimePacket packet) {
        this.lastUpdate = System.nanoTime();
        this.gameTime = packet.getGameTime();
        this.dayTime = packet.getDayTime();
        this.tickDayTime = packet.isTickDayTime();
    }

    public ClientboundSetTimePacket toPacket() {
        // The amount of ticks that have passed since the last time packet was received.
        final long offset = (System.nanoTime() - this.lastUpdate) / 50000000;

        long worldAge = this.gameTime;

        if (worldAge > 0) {
            worldAge += offset;
        }

        long time = this.dayTime;

        // If time is negative, the daylight cycle is disabled (e.g. from the "doDaylightCycle" gamerule being false)
        if (time >= 0) {
            time += offset;
            time %= 24000;
        }

        return new ClientboundSetTimePacket(worldAge, time, tickDayTime);
    }
}
