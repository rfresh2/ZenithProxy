package com.zenith.cache.data.bossbar;

import com.zenith.cache.CachedData;
import lombok.NonNull;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundBossEventPacket;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class BossBarCache implements CachedData {
    protected final Map<UUID, BossBar> bossBars = new ConcurrentHashMap<>();

    @Override
    public void getPackets(@NonNull Consumer<Packet> consumer) {
        this.bossBars.values().stream().map(BossBar::toMCProtocolLibPacket).forEach(consumer);
    }

    @Override
    public void reset(boolean full) {
        this.bossBars.clear();
    }

    @Override
    public String getSendingMessage() {
        return String.format("Sending %d boss bars", this.bossBars.size());
    }

    public void add(@NonNull ClientboundBossEventPacket packet) {
        this.bossBars.put(
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
        this.bossBars.remove(packet.getUuid());
    }

    public @Nullable BossBar get(@NonNull ClientboundBossEventPacket packet) {
        return this.bossBars.get(packet.getUuid());
    }
}
