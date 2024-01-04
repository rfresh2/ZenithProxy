package com.zenith.cache.data.scoreboard;

import com.github.steveice10.mc.protocol.data.game.scoreboard.ObjectiveAction;
import com.github.steveice10.mc.protocol.data.game.scoreboard.ScoreType;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.scoreboard.ClientboundSetObjectivePacket;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.kyori.adventure.text.Component;

@Getter
@Setter
@Accessors(chain = true)
@RequiredArgsConstructor
public class Objective {
    @NonNull
    protected final String name;

    protected Component displayName;
    protected ScoreType scoreType;

    protected final Object2IntMap<String> scores = new Object2IntOpenHashMap<>();

    public ClientboundSetObjectivePacket toPacket() {
        return new ClientboundSetObjectivePacket(
                this.name,
                ObjectiveAction.ADD,
                this.displayName,
                this.scoreType
        );
    }
}
