package com.zenith.network.server.handler.spectator.postoutgoing;

import com.github.steveice10.mc.protocol.data.game.PlayerListEntry;
import com.github.steveice10.mc.protocol.data.game.PlayerListEntryAction;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundLoginPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundPlayerInfoUpdatePacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundSystemChatPacket;
import com.zenith.Proxy;
import com.zenith.feature.spectator.SpectatorSync;
import com.zenith.network.registry.PostOutgoingPacketHandler;
import com.zenith.network.server.ServerConnection;
import com.zenith.util.ComponentSerializer;
import lombok.NonNull;

import java.util.EnumSet;

import static com.github.steveice10.mc.protocol.data.game.entity.player.GameMode.SPECTATOR;
import static com.zenith.Shared.CACHE;
import static com.zenith.Shared.CONFIG;

public class LoginSpectatorPostHandler implements PostOutgoingPacketHandler<ClientboundLoginPacket, ServerConnection> {
    @Override
    public void accept(@NonNull ClientboundLoginPacket packet, @NonNull ServerConnection session) {
        if (CONFIG.server.extra.whitelist.enable && !session.isWhitelistChecked()) {
            // we shouldn't be able to get to this point without whitelist checking, but just in case
            session.disconnect("Login without whitelist check?");
            return;
        }
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
        SpectatorSync.initSpectator(session, () -> CACHE.getAllDataSpectator(session.getSpectatorPlayerCache()));
        //send cached data
        Proxy.getInstance().getActiveConnections().stream()
                .filter(connection -> !connection.equals(session))
                .forEach(connection -> {
                    connection.send(new ClientboundSystemChatPacket(
                        ComponentSerializer.minedown("&9" + session.getProfileCache().getProfile().getName() + " connected!&r"), false
                    ));
                    if (connection.equals(Proxy.getInstance().getCurrentPlayer().get())) {
                        connection.send(new ClientboundSystemChatPacket(
                            ComponentSerializer.minedown("&9Send private messages: \"!m <message>\"&r"), false
                        ));
                    }
                });
        session.setLoggedIn();
        ServerConnection currentPlayer = Proxy.getInstance().getCurrentPlayer().get();
        if (currentPlayer != null) currentPlayer.syncTeamMembers();
        SpectatorSync.syncPlayerEquipmentWithSpectatorsFromCache();
        // send command help
        session.send(new ClientboundSystemChatPacket(ComponentSerializer.minedown("&7[&9ZenithProxy&7]&r &2Spectating &r&c" + CACHE.getProfileCache().getProfile().getName()), false));
        if (CONFIG.inGameCommands.enable) {
            session.send(new ClientboundSystemChatPacket(ComponentSerializer.minedown("&2Command Prefix : \"" + CONFIG.inGameCommands.prefix + "\""), false));
            session.send(new ClientboundSystemChatPacket(ComponentSerializer.minedown("&chelp &7- &8List Commands"), false));
        }
    }
}
