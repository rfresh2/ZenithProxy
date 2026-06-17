package com.zenith.network.client.handler.incoming.scoreboard;

import com.zenith.network.client.ClientSession;
import com.zenith.network.codec.ClientEventLoopPacketHandler;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.scoreboard.ClientboundSetPlayerTeamPacket;
import org.jspecify.annotations.NonNull;

import java.util.Collections;

import static com.zenith.Globals.CACHE;

public class TeamHandler implements ClientEventLoopPacketHandler<ClientboundSetPlayerTeamPacket, ClientSession> {
    @Override
    public boolean applyAsync(@NonNull ClientboundSetPlayerTeamPacket packet, @NonNull ClientSession session) {
        switch (packet.getAction()) {
            case CREATE -> CACHE.getTeamCache().add(packet);
            case REMOVE -> CACHE.getTeamCache().getTeamsByName().get(packet.getTeamName());
            case UPDATE -> {
                var team = CACHE.getTeamCache().getTeamsByName().get(packet.getTeamName());
                if (team != null) {
                    team.setDisplayName(packet.getDisplayName())
                        .setPrefix(packet.getPlayerPrefix())
                        .setSuffix(packet.getPlayerSuffix())
                        .setFriendlyFire(packet.isFriendlyFire())
                        .setSeeFriendlyInvisibles(packet.isSeeFriendlyInvisibles())
                        .setNameTagVisibility(packet.getNameTagVisibility())
                        .setCollisionRule(packet.getCollisionRule())
                        .setColor(packet.getColor());
                }
            }
            case ADD_PLAYER -> {
                var team = CACHE.getTeamCache().getTeamsByName().get(packet.getTeamName());
                if (team != null) {
                    Collections.addAll(team.getPlayers(), packet.getPlayers());
                    for (var player : packet.getPlayers()) {
                        CACHE.getTeamCache().getTeamsByPlayer().put(player, team);
                    }
                }
            }
            case REMOVE_PLAYER -> {
                var team = CACHE.getTeamCache().getTeamsByName().get(packet.getTeamName());
                if (team != null) {
                    var players = team.getPlayers();
                    for (String p : packet.getPlayers()) {
                        players.remove(p);
                        CACHE.getTeamCache().getTeamsByPlayer().remove(p);
                    }
                }
            }
        }
        return true;
    }
}
