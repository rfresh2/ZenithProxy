package com.zenith.cache.data.scoreboard;

import lombok.Data;
import lombok.NonNull;
import lombok.experimental.Accessors;
import net.kyori.adventure.text.Component;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.data.game.chat.numbers.NumberFormat;
import org.geysermc.mcprotocollib.protocol.data.game.scoreboard.ObjectiveAction;
import org.geysermc.mcprotocollib.protocol.data.game.scoreboard.ScoreType;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.scoreboard.ClientboundSetObjectivePacket;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Data
@Accessors(chain = true)
public class Objective {
    @NonNull
    protected final String name;

    protected Component displayName;
    protected ScoreType scoreType;
    protected NumberFormat numberFormat;

    protected final Map<String, Score> scores = new ConcurrentHashMap<>();

    public void addPackets(Consumer<Packet> consumer) {
        consumer.accept(new ClientboundSetObjectivePacket(
                this.name,
                ObjectiveAction.ADD,
                this.displayName,
                this.scoreType,
                this.numberFormat
        ));
        for (var score : this.scores.values()) {
            consumer.accept(score.toPacket(this.name));
        }
    }
}
