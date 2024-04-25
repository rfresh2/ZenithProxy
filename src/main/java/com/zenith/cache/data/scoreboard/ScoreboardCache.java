package com.zenith.cache.data.scoreboard;

import com.zenith.cache.CachedData;
import lombok.Data;
import lombok.NonNull;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.data.game.scoreboard.ScoreboardPosition;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.scoreboard.ClientboundSetDisplayObjectivePacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.scoreboard.ClientboundSetObjectivePacket;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Data
public class ScoreboardCache implements CachedData {
    protected final Map<String, Objective> cachedObjectives = new ConcurrentHashMap<>();
    protected final Map<ScoreboardPosition, String> cachedPositionObjectives = new EnumMap<>(ScoreboardPosition.class);

    @Override
    public void getPackets(@NonNull Consumer<Packet> consumer) {
        for (final Objective objective : this.cachedObjectives.values()) {
            objective.addPackets(consumer);
        }
        this.cachedPositionObjectives.forEach((pos, objective) -> consumer.accept(new ClientboundSetDisplayObjectivePacket(pos, objective)));
    }

    @Override
    public void reset(boolean full) {
        if (full) {
            this.cachedObjectives.clear();
            this.cachedPositionObjectives.clear();
        }
    }

    @Override
    public String getSendingMessage() {
        return String.format("Sending %d scoreboard objectives", this.cachedObjectives.size());
    }

    public void setPositionObjective(ScoreboardPosition position, String objective) {
        this.cachedPositionObjectives.put(position, objective);
    }

    public void add(@NonNull ClientboundSetObjectivePacket packet) {
        this.cachedObjectives.put(
                packet.getName(),
                new Objective(packet.getName())
                        .setDisplayName(packet.getDisplayName())
                        .setScoreType(packet.getType())
        );
    }

    public void remove(@NonNull ClientboundSetObjectivePacket packet) {
        this.cachedObjectives.remove(packet.getName());
    }

    public void removeEntry(@NonNull String owner) {
        for (final Objective objective : this.cachedObjectives.values()) {
            objective.getScores().remove(owner);
        }
    }

    public Objective get(@NonNull String name) {
        return this.cachedObjectives.get(name);
    }
}
