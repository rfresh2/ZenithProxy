package com.zenith.server.handler.spectator.postoutgoing;

import com.github.steveice10.mc.protocol.data.game.PlayerListEntry;
import com.github.steveice10.mc.protocol.data.game.PlayerListEntryAction;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerChatPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerJoinGamePacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerPlayerListEntryPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerPluginMessagePacket;
import com.zenith.server.ServerConnection;
import com.zenith.util.RefStrings;
import com.zenith.util.handler.HandlerRegistry;
import com.zenith.util.spectator.SpectatorHelper;
import lombok.NonNull;

import static com.github.steveice10.mc.protocol.data.game.entity.player.GameMode.SPECTATOR;
import static com.zenith.util.Constants.CACHE;

public class JoinGameSpectatorPostHandler implements HandlerRegistry.PostOutgoingHandler<ServerJoinGamePacket, ServerConnection> {
    @Override
    public void accept(@NonNull ServerJoinGamePacket packet, @NonNull ServerConnection session) {
        session.send(new ServerPluginMessagePacket("MC|Brand", RefStrings.BRAND_ENCODED));
        session.send(new ServerPlayerListEntryPacket(
                PlayerListEntryAction.ADD_PLAYER,
                new PlayerListEntry[]{new PlayerListEntry(session.getProfileCache().getProfile(), SPECTATOR)}
        ));
        SpectatorHelper.initSpectator(session, () -> CACHE.getAllDataSpectator(session.getSpectatorPlayerCache()));
        //send cached data
        session.getProxy().getServerConnections().stream()
                .filter(connection -> !connection.equals(session))
                .forEach(connection -> {
                    connection.send(new ServerChatPacket(
                            "§9" + session.getProfileCache().getProfile().getName() + " connected!§r", true
                    ));
                    if (connection.equals(session.getProxy().getCurrentPlayer().get())) {
                        connection.send(new ServerChatPacket(
                                "§9Send private messages: \"!m <message>\"§r", true
                        ));
                    }
                });
        session.setLoggedIn(true);
    }

    @Override
    public Class<ServerJoinGamePacket> getPacketClass() {
        return ServerJoinGamePacket.class;
    }
}
