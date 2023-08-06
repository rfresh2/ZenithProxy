package com.zenith.network.server.handler.player.postoutgoing;

import com.github.steveice10.mc.protocol.packet.ingame.server.ServerChatPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerJoinGamePacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerPluginMessagePacket;
import com.zenith.cache.DataCache;
import com.zenith.network.registry.PostOutgoingHandler;
import com.zenith.network.server.ServerConnection;
import com.zenith.util.RefStrings;
import lombok.NonNull;

import static com.zenith.Shared.CACHE;
import static com.zenith.Shared.CONFIG;

public class JoinGamePostHandler implements PostOutgoingHandler<ServerJoinGamePacket, ServerConnection> {
    @Override
    public void accept(@NonNull ServerJoinGamePacket packet, @NonNull ServerConnection session) {
        session.send(new ServerPluginMessagePacket("MC|Brand", RefStrings.BRAND_SUPPLIER.get()));


        session.setLoggedIn(true); // allows server packets to start being sent to player
        // send cached data
        DataCache.sendCacheData(CACHE.getAllData(), session);
        // init any active spectators
        session.getProxy().getActiveConnections().stream()
                .filter(connection -> !connection.equals(session))
                .filter(connection -> !connection.isPlayerCam())
                .forEach(connection -> {
                    session.send(connection.getEntitySpawnPacket());
                    session.send(connection.getEntityMetadataPacket());
                });
        if (CONFIG.client.extra.chat.hideChat) {
            session.send(new ServerChatPacket("§7Chat is currently disabled. To enable chat, type §c/togglechat§7.", true));
        }
        if (CONFIG.client.extra.chat.hideWhispers) {
            session.send(new ServerChatPacket("§7Whispers are currently disabled. To enable whispers, type §c/toggleprivatemsgs§7.", true));
        }
        if (CONFIG.client.extra.chat.showConnectionMessages) {
            session.send(new ServerChatPacket("§7Connection messages enabled. To disable, type §c/toggleconnectionmsgs§7.", true));
        }
        if (CONFIG.client.extra.chat.hideDeathMessages) {
            session.send(new ServerChatPacket("§7Death messages are currently disabled. To enable death messages, type §c/toggledeathmsgs§7.", true));
        }
    }

    @Override
    public Class<ServerJoinGamePacket> getPacketClass() {
        return ServerJoinGamePacket.class;
    }
}
