package com.zenith.cache.data.bossbar;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundBossEventPacket;
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

    public void add(@NonNull ClientboundBossEventPacket packet) {
        this.cachedBossBars.put(
                packet.getUuid(),
                new BossBar(packet.getUuid())
                        .setTitle(packet.getTitle())
                        .setHealth(packet.getHealth())
                        .setColor(packet.getColor())
                        .setDivision(packet.getDivision())
                        .setDarkenSky(packet.isDarkenSky())
                        .setPlayEndMusic(packet.isPlayEndMusic())
        );
    }

    public void remove(@NonNull ClientboundBossEventPacket packet) {
        this.cachedBossBars.remove(packet.getUuid());
    }

    public BossBar get(@NonNull ClientboundBossEventPacket packet) {
        BossBar bossBar = this.cachedBossBars.get(packet.getUuid());
        if (bossBar == null)    {
            return new BossBar(packet.getUuid());
        }
        return bossBar;
    }
}
