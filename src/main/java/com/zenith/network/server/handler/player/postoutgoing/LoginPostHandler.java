package com.zenith.network.server.handler.player.postoutgoing;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundLoginPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundSystemChatPacket;
import com.zenith.Proxy;
import com.zenith.cache.DataCache;
import com.zenith.event.proxy.ProxyClientLoggedInEvent;
import com.zenith.network.registry.PostOutgoingPacketHandler;
import com.zenith.network.server.ServerConnection;
import com.zenith.util.ComponentSerializer;
import lombok.NonNull;

import static com.zenith.Shared.*;

public class LoginPostHandler implements PostOutgoingPacketHandler<ClientboundLoginPacket, ServerConnection> {
    @Override
    public void accept(@NonNull ClientboundLoginPacket packet, @NonNull ServerConnection session) {
        if (CONFIG.server.extra.whitelist.enable && !session.isWhitelistChecked()) {
            // we shouldn't be able to get to this point without whitelist checking, but just in case
            session.disconnect("Login without whitelist check?");
            return;
        }
        // todo: move this after cache is sent
        //  queue packets received in the meantime to be sent after cache is sent
        session.setLoggedIn(); // allows server packets to start being sent to player
        EVENT_BUS.postAsync(new ProxyClientLoggedInEvent(session));
        // send cached data
        DataCache.sendCacheData(CACHE.getAllData(), session);
        session.initializeTeam();
        session.syncTeamMembers();
        // init any active spectators
        Proxy.getInstance().getActiveConnections().stream()
                .filter(connection -> !connection.equals(session))
                .filter(connection -> !connection.hasCameraTarget())
                .forEach(connection -> {
                    session.send(connection.getEntitySpawnPacket());
                    session.send(connection.getEntityMetadataPacket());
                });
        // add spectators and self to team
        if (CONFIG.client.extra.chat.hideChat) {
            session.send(new ClientboundSystemChatPacket(ComponentSerializer.minedown("&7Chat is currently disabled. To enable chat, type &c/togglechat&7."), false));
        }
        if (CONFIG.client.extra.chat.hideWhispers) {
            session.send(new ClientboundSystemChatPacket(ComponentSerializer.minedown("&7Whispers are currently disabled. To enable whispers, type &c/toggleprivatemsgs&7."), false));
        }
        if (CONFIG.client.extra.chat.showConnectionMessages) {
            session.send(new ClientboundSystemChatPacket(ComponentSerializer.minedown("&7Connection messages enabled. To disable, type &c/toggleconnectionmsgs&7."), false));
        }
        if (CONFIG.client.extra.chat.hideDeathMessages) {
            session.send(new ClientboundSystemChatPacket(ComponentSerializer.minedown("&7Death messages are currently disabled. To enable death messages, type &c/toggledeathmsgs&7."), false));
        }
        session.sendAsyncAlert("&2Connected to &r&c" + CACHE.getProfileCache().getProfile().getName());
        if (CONFIG.inGameCommands.enable && !CONFIG.inGameCommands.slashCommands) {
            session.send(new ClientboundSystemChatPacket(ComponentSerializer.minedown("&2Command Prefix : \"" + CONFIG.inGameCommands.prefix + "\""), false));
            session.send(new ClientboundSystemChatPacket(ComponentSerializer.minedown("&chelp &7- &8List Commands"), false));
        }
    }
}
