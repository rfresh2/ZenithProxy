package com.zenith.network.server.handler.spectator.postoutgoing;

import com.github.steveice10.mc.protocol.data.game.PlayerListEntry;
import com.github.steveice10.mc.protocol.data.game.PlayerListEntryAction;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundCustomPayloadPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundLoginPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundPlayerInfoUpdatePacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundSystemChatPacket;
import com.zenith.Proxy;
import com.zenith.feature.spectator.SpectatorUtils;
import com.zenith.network.registry.PostOutgoingHandler;
import com.zenith.network.server.ServerConnection;
import com.zenith.util.RefStrings;
import de.themoep.minedown.adventure.MineDown;
import lombok.NonNull;

import java.util.EnumSet;

import static com.github.steveice10.mc.protocol.data.game.entity.player.GameMode.SPECTATOR;
import static com.zenith.Shared.CACHE;

public class LoginSpectatorPostHandler implements PostOutgoingHandler<ClientboundLoginPacket, ServerConnection> {
    @Override
    public void accept(@NonNull ClientboundLoginPacket packet, @NonNull ServerConnection session) {
        session.send(new ClientboundCustomPayloadPacket("minecraft:brand", RefStrings.BRAND_SUPPLIER.get()));
        session.send(new ClientboundPlayerInfoUpdatePacket(
            EnumSet.of(PlayerListEntryAction.ADD_PLAYER, PlayerListEntryAction.UPDATE_LISTED, PlayerListEntryAction.UPDATE_GAME_MODE),
            new PlayerListEntry[]{new PlayerListEntry(
                session.getSpectatorFakeProfileCache().getProfile().getId(),
                session.getSpectatorFakeProfileCache().getProfile(),
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
        session.setLoggedIn();
        ServerConnection currentPlayer = Proxy.getInstance().getCurrentPlayer().get();
        if (currentPlayer != null) currentPlayer.syncTeamMembers();
        SpectatorUtils.syncPlayerEquipmentWithSpectatorsFromCache();
    }
}
