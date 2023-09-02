package com.zenith.network.server.handler.spectator.postoutgoing;

import com.github.steveice10.mc.protocol.data.game.PlayerListEntry;
import com.github.steveice10.mc.protocol.data.game.PlayerListEntryAction;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundCustomPayloadPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundLoginPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundPlayerInfoUpdatePacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundSystemChatPacket;
import com.zenith.feature.spectator.SpectatorUtils;
import com.zenith.network.registry.PostOutgoingHandler;
import com.zenith.network.server.ServerConnection;
import com.zenith.util.RefStrings;
import de.themoep.minedown.adventure.MineDown;
import lombok.NonNull;

import java.util.EnumSet;

import static com.github.steveice10.mc.protocol.data.game.entity.player.GameMode.SPECTATOR;
import static com.zenith.Shared.CACHE;

public class JoinGameSpectatorPostHandler implements PostOutgoingHandler<ClientboundLoginPacket, ServerConnection> {
    @Override
    public void accept(@NonNull ClientboundLoginPacket packet, @NonNull ServerConnection session) {
        // todo: verify brand channel
        session.send(new ClientboundCustomPayloadPacket("brand", RefStrings.BRAND_SUPPLIER.get()));
        session.send(new ClientboundPlayerInfoUpdatePacket(
            EnumSet.of(PlayerListEntryAction.ADD_PLAYER),
            new PlayerListEntry[]{new PlayerListEntry(
                session.getProfileCache().getProfile().getId(),
                session.getProfileCache().getProfile(),
                true,
                0,
                SPECTATOR,
                null,
                null,
                0,
                null,
                null
            )}
        ));
        SpectatorUtils.initSpectator(session, () -> CACHE.getAllDataSpectator(session.getSpectatorPlayerCache()));
        //send cached data
        session.getProxy().getActiveConnections().stream()
                .filter(connection -> !connection.equals(session))
                .forEach(connection -> {
                    connection.send(new ClientboundSystemChatPacket(
                        MineDown.parse("&9" + session.getProfileCache().getProfile().getName() + " connected!&r"), false
                    ));
                    if (connection.equals(session.getProxy().getCurrentPlayer().get())) {
                        connection.send(new ClientboundSystemChatPacket(
                            MineDown.parse("&9Send private messages: \"!m <message>\"&r"), false
                        ));
                    }
                });
        session.setLoggedIn(true);
    }

    @Override
    public Class<ClientboundLoginPacket> getPacketClass() {
        return ClientboundLoginPacket.class;
    }
}
