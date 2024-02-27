package com.zenith.cache.data.scoreboard;

import com.github.steveice10.mc.protocol.data.game.chat.numbers.NumberFormat;
import com.github.steveice10.mc.protocol.data.game.scoreboard.ObjectiveAction;
import com.github.steveice10.mc.protocol.data.game.scoreboard.ScoreType;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.scoreboard.ClientboundSetObjectivePacket;
import com.github.steveice10.packetlib.packet.Packet;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import lombok.Data;
import lombok.NonNull;
import lombok.experimental.Accessors;
import net.kyori.adventure.text.Component;

import java.util.function.Consumer;

@Data
@Accessors(chain = true)
public class Objective {
    @NonNull
    protected final String name;

    protected Component displayName;
    protected ScoreType scoreType;
    protected NumberFormat numberFormat;

    protected final Object2ObjectMap<String, Score> scores = new Object2ObjectOpenHashMap<>();

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
