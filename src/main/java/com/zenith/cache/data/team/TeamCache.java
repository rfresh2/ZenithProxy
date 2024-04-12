package com.zenith.cache.data.team;

import com.zenith.cache.CachedData;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import lombok.NonNull;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.scoreboard.ClientboundSetPlayerTeamPacket;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class TeamCache implements CachedData {
    protected final Map<String, Team> cachedTeams = new ConcurrentHashMap<>();

    @Override
    public void getPackets(@NonNull Consumer<Packet> consumer) {
        this.cachedTeams.values().stream().map(Team::toPacket).forEach(consumer);
    }

    @Override
    public void reset(boolean full) {
        if (full) {
            this.cachedTeams.clear();
        }
    }

    @Override
    public String getSendingMessage() {
        return String.format("Sending %d teams", this.cachedTeams.size());
    }

    public void add(@NonNull ClientboundSetPlayerTeamPacket packet) {
        this.cachedTeams.put(
                packet.getTeamName(),
                new Team(packet.getTeamName())
                        .setDisplayName(packet.getDisplayName())
                        .setPrefix(packet.getPrefix())
                        .setSuffix(packet.getSuffix())
                        .setFriendlyFire(packet.isFriendlyFire())
                        .setSeeFriendlyInvisibles(packet.isSeeFriendlyInvisibles())
                        .setNameTagVisibility(packet.getNameTagVisibility())
                        .setCollisionRule(packet.getCollisionRule())
                        .setColor(packet.getColor())
                        .setPlayers(ObjectArraySet.of(packet.getPlayers()))
        );
    }

    public void remove(@NonNull ClientboundSetPlayerTeamPacket packet) {
        this.cachedTeams.remove(packet.getTeamName());
    }

    public Team get(@NonNull ClientboundSetPlayerTeamPacket packet) {
        return this.cachedTeams.get(packet.getTeamName());
    }
}
