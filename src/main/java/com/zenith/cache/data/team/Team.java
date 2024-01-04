package com.zenith.cache.data.team;

import com.github.steveice10.mc.protocol.data.game.scoreboard.CollisionRule;
import com.github.steveice10.mc.protocol.data.game.scoreboard.NameTagVisibility;
import com.github.steveice10.mc.protocol.data.game.scoreboard.TeamColor;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.scoreboard.ClientboundSetPlayerTeamPacket;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.kyori.adventure.text.Component;

import java.util.List;


@Getter
@Setter
@Accessors(chain = true)
@RequiredArgsConstructor
public class Team {
    @NonNull
    protected final String teamName;

    protected Component displayName;
    protected Component prefix;
    protected Component suffix;
    protected boolean friendlyFire;
    protected boolean seeFriendlyInvisibles;
    protected NameTagVisibility nameTagVisibility;
    protected CollisionRule collisionRule;
    protected TeamColor color;
    protected List<String> players;

    public ClientboundSetPlayerTeamPacket toPacket() {
        return new ClientboundSetPlayerTeamPacket(
                this.teamName,
                this.displayName,
                this.prefix,
                this.suffix,
                this.friendlyFire,
                this.seeFriendlyInvisibles,
                this.nameTagVisibility,
                this.collisionRule,
                this.color,
                this.players.toArray(new String[0])
        );
    }
}
