package com.zenith.network.server.handler.spectator.postoutgoing;

import com.zenith.Proxy;
import com.zenith.event.proxy.ProxySpectatorLoggedInEvent;
import com.zenith.feature.spectator.SpectatorSync;
import com.zenith.network.registry.PostOutgoingPacketHandler;
import com.zenith.network.server.ServerConnection;
import com.zenith.util.ComponentSerializer;
import lombok.NonNull;
import org.geysermc.mcprotocollib.protocol.data.game.PlayerListEntry;
import org.geysermc.mcprotocollib.protocol.data.game.PlayerListEntryAction;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundLoginPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundPlayerInfoUpdatePacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundSystemChatPacket;

import java.util.EnumSet;

import static com.zenith.Shared.*;
import static org.geysermc.mcprotocollib.protocol.data.game.entity.player.GameMode.SPECTATOR;

public class LoginSpectatorPostHandler implements PostOutgoingPacketHandler<ClientboundLoginPacket, ServerConnection> {
    @Override
    public void accept(@NonNull ClientboundLoginPacket packet, @NonNull ServerConnection session) {
        if (CONFIG.server.extra.whitelist.enable && !session.isWhitelistChecked()) {
            // we shouldn't be able to get to this point without whitelist checking, but just in case
            session.disconnect("Login without whitelist check?");
            return;
        }
        session.sendAsync(new ClientboundPlayerInfoUpdatePacket(
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
        EVENT_BUS.postAsync(new ProxySpectatorLoggedInEvent(session));
        SpectatorSync.initSpectator(session, () -> CACHE.getAllDataSpectator(session.getSpectatorPlayerCache()));
        //send cached data
        var connections = Proxy.getInstance().getActiveConnections().getArray();
        for (int i = 0; i < connections.length; i++) {
            var connection = connections[i];
            if (connection.equals(session)) continue;
            connection.sendAsync(new ClientboundSystemChatPacket(
                ComponentSerializer.minedown("&9" + session.getProfileCache().getProfile().getName() + " connected!&r"), false
            ));
            if (connection.equals(Proxy.getInstance().getCurrentPlayer().get())) {
                connection.sendAsync(new ClientboundSystemChatPacket(
                    ComponentSerializer.minedown("&9Send private messages: \"!m <message>\"&r"), false
                ));
            }
        }
        session.setLoggedIn();
        ServerConnection currentPlayer = Proxy.getInstance().getCurrentPlayer().get();
        if (currentPlayer != null) currentPlayer.syncTeamMembers();
        SpectatorSync.syncPlayerEquipmentWithSpectatorsFromCache();
        // send command help
        session.sendAsyncAlert("&aSpectating &r&c" + CACHE.getProfileCache().getProfile().getName());
        if (CONFIG.inGameCommands.enable) {
            session.sendAsync(new ClientboundSystemChatPacket(ComponentSerializer.minedown("&aCommand Prefix : \"" + CONFIG.inGameCommands.prefix + "\""), false));
            session.sendAsync(new ClientboundSystemChatPacket(ComponentSerializer.minedown("&chelp &7- &8List Commands"), false));
        }
    }
}
