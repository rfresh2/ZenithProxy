package com.zenith.cache.data.team;

import com.zenith.cache.CacheResetType;
import com.zenith.cache.CachedData;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import lombok.Data;
import lombok.experimental.Accessors;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.network.tcp.TcpSession;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.scoreboard.ClientboundSetPlayerTeamPacket;
import org.jspecify.annotations.NonNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Data
@Accessors(chain = true)
public class TeamCache implements CachedData {
     protected final Map<String, Team> teamsByName = new ConcurrentHashMap<>();
     protected final Map<String, Team> teamsByPlayer = new ConcurrentHashMap<>();

    @Override
    public void getPackets(@NonNull Consumer<Packet> consumer, final @NonNull TcpSession session) {
        this.teamsByName.values().stream().map(Team::toPacket).forEach(consumer);
    }

    @Override
    public void reset(CacheResetType type) {
        if (type == CacheResetType.FULL || type == CacheResetType.LOGIN || type == CacheResetType.PROTOCOL_SWITCH) {
            this.teamsByName.clear();
            this.teamsByPlayer.clear();
        }
    }

    @Override
    public String getSendingMessage() {
        return String.format("Sending %d teams", this.teamsByName.size());
    }

    public void add(@NonNull ClientboundSetPlayerTeamPacket packet) {
        var team = new Team(packet.getTeamName())
            .setDisplayName(packet.getDisplayName())
            .setPrefix(packet.getPlayerPrefix())
            .setSuffix(packet.getPlayerSuffix())
            .setFriendlyFire(packet.isFriendlyFire())
            .setSeeFriendlyInvisibles(packet.isSeeFriendlyInvisibles())
            .setNameTagVisibility(packet.getNameTagVisibility())
            .setCollisionRule(packet.getCollisionRule())
            .setColor(packet.getColor())
            .setPlayers(ObjectArraySet.of(packet.getPlayers()));
        this.teamsByName.put(packet.getTeamName(), team);
        for (var player : packet.getPlayers()) {
            this.teamsByPlayer.put(player, team);
        }
    }
}
