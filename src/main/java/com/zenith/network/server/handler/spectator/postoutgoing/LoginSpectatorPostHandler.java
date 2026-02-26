package com.zenith.network.server.handler.spectator.postoutgoing;

import com.zenith.Proxy;
import com.zenith.event.player.SpectatorLoggedInEvent;
import com.zenith.feature.spectator.SpectatorSync;
import com.zenith.network.codec.PostOutgoingPacketHandler;
import com.zenith.network.server.ServerSession;
import org.geysermc.mcprotocollib.protocol.data.game.PlayerListEntry;
import org.geysermc.mcprotocollib.protocol.data.game.PlayerListEntryAction;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundLoginPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundPlayerInfoUpdatePacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundSetCameraPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundRemoveEntitiesPacket;
import org.jspecify.annotations.NonNull;

import java.util.EnumSet;

import static com.zenith.Globals.*;
import static com.zenith.util.ComponentSerializer.minimessage;
import static org.geysermc.mcprotocollib.protocol.data.game.entity.player.GameMode.SPECTATOR;

public class LoginSpectatorPostHandler implements PostOutgoingPacketHandler<ClientboundLoginPacket, ServerSession> {
    @Override
    public void accept(@NonNull ClientboundLoginPacket packet, @NonNull ServerSession session) {
        if (!session.isWhitelistChecked()) {
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
                false,
                0,
                null,
                0,
                null,
                null
            )}
        ));
        EVENT_BUS.postAsync(new SpectatorLoggedInEvent(session));
        // allows client packets to begin being forwarded to spectator
        // but they will queue behind this handler as its already executing in the event loop
        session.setLoggedIn();
        SpectatorSync.initSpectator(session, () -> CACHE.getAllDataSpectator(session.getSpectatorPlayerCache()));
        if (CONFIG.server.welcomeMessages) {
            var connections = Proxy.getInstance().getActiveConnections().getArray();
            for (int i = 0; i < connections.length; i++) {
                var connection = connections[i];
                if (connection.equals(session)) continue;
                connection.sendAsyncMessage(minimessage("<green>" + session.getName() + " connected!"));
                if (connection.equals(Proxy.getInstance().getCurrentPlayer().get())) {
                    connection.sendAsyncMessage(minimessage("<blue>Send private messages: \"!m \\<message>\""));
                }
            }
        }
        ServerSession currentPlayer = Proxy.getInstance().getCurrentPlayer().get();
        if (currentPlayer != null) currentPlayer.syncTeamMembers();
        SpectatorSync.syncPlayerEquipmentWithSpectatorsFromCache();
        if (CONFIG.server.spectator.playerCamOnJoin) {
            session.setCameraTarget(CACHE.getPlayerCache().getThePlayer());
            session.sendAsync(new ClientboundSetCameraPacket(CACHE.getPlayerCache().getEntityId()));
            var sessions = Proxy.getInstance().getActiveConnections().getArray();
            for (int i = 0; i < sessions.length; i++) {
                var connection = sessions[i];
                connection.sendAsync(new ClientboundRemoveEntitiesPacket(new int[]{session.getSpectatorEntityId()}));
            }
        }
        // send command help
        if (CONFIG.server.welcomeMessages) {
            session.sendAsyncAlert("<green>Spectating <red>" + CACHE.getProfileCache().getProfile().getName());
            if (CONFIG.inGameCommands.enable) {
                session.sendAsyncMessage(minimessage("<green>Command Prefix : \"" + CONFIG.inGameCommands.prefix + "\""));
                session.sendAsyncMessage(minimessage("<red>help <gray>- <dark_gray>List Commands"));
            }
        }
    }
}
