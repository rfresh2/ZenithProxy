package com.zenith.cache.data.bossbar;

import com.github.steveice10.mc.protocol.packet.ingame.server.ServerBossBarPacket;
import com.github.steveice10.packetlib.packet.Packet;
import com.zenith.cache.CachedData;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import lombok.NonNull;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public class BossBarCache implements CachedData {
    protected final Map<UUID, BossBar> cachedBossBars = Collections.synchronizedMap(new Object2ObjectOpenHashMap<>());

    @Override
    public void getPackets(@NonNull Consumer<Packet> consumer) {
        this.cachedBossBars.values().stream().map(BossBar::toMCProtocolLibPacket).forEach(consumer);
    }

    @Override
    public void reset(boolean full) {
        this.cachedBossBars.clear();
    }

    @Override
    public String getSendingMessage() {
        return String.format("Sending %d boss bars", this.cachedBossBars.size());
    }

    public void add(@NonNull ServerBossBarPacket packet) {
        this.cachedBossBars.put(
                packet.getUUID(),
                new BossBar(packet.getUUID())
                        .setTitle(packet.getTitle())
                        .setHealth(packet.getHealth())
                        .setColor(packet.getColor())
                        .setDivision(packet.getDivision())
                        .setDarkenSky(packet.getDarkenSky())
                        .setDragonBar(packet.isDragonBar())
        );
    }

    public void remove(@NonNull ServerBossBarPacket packet) {
        this.cachedBossBars.remove(packet.getUUID());
    }

    public BossBar get(@NonNull ServerBossBarPacket packet) {
        BossBar bossBar = this.cachedBossBars.get(packet.getUUID());
        if (bossBar == null)    {
            return new BossBar(packet.getUUID());
        }
        return bossBar;
    }
}
