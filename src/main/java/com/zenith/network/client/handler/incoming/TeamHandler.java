package com.zenith.network.client.handler.incoming;

import com.github.steveice10.mc.protocol.data.game.scoreboard.TeamAction;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.scoreboard.ClientboundSetPlayerTeamPacket;
import com.zenith.cache.data.team.Team;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.AsyncPacketHandler;
import lombok.NonNull;

import java.util.Arrays;

import static com.zenith.Shared.CACHE;

public class TeamHandler implements AsyncPacketHandler<ClientboundSetPlayerTeamPacket, ClientSession> {
    @Override
    public boolean applyAsync(@NonNull ClientboundSetPlayerTeamPacket packet, @NonNull ClientSession session) {
        if (packet.getAction() == TeamAction.CREATE) {
            CACHE.getTeamCache().add(packet);
        } else if (packet.getAction() == TeamAction.REMOVE) {
            CACHE.getTeamCache().remove(packet);
        } else {
            final Team team = CACHE.getTeamCache().get(packet);
            if (team == null) {
                return false;
            }

            switch (packet.getAction()) {
                case UPDATE -> {
                    team.setDisplayName(packet.getDisplayName());
                    team.setPrefix(packet.getPrefix());
                    team.setSuffix(packet.getSuffix());
                    team.setFriendlyFire(packet.isFriendlyFire());
                    team.setSeeFriendlyInvisibles(packet.isSeeFriendlyInvisibles());
                    team.setNameTagVisibility(packet.getNameTagVisibility());
                    team.setCollisionRule(packet.getCollisionRule());
                    team.setColor(packet.getColor());
                }
                case ADD_PLAYER -> team.getPlayers().addAll(Arrays.asList(packet.getPlayers()));
                case REMOVE_PLAYER -> team.getPlayers().removeAll(Arrays.asList(packet.getPlayers()));
            }
        }
        return true;
    }
}
