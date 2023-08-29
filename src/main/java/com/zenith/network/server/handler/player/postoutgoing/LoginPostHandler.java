package com.zenith.network.server.handler.player.postoutgoing;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundCustomPayloadPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundLoginPacket;
import com.zenith.cache.DataCache;
import com.zenith.network.registry.PostOutgoingHandler;
import com.zenith.network.server.ServerConnection;
import com.zenith.util.RefStrings;
import lombok.NonNull;

import static com.zenith.Shared.CACHE;

public class LoginPostHandler implements PostOutgoingHandler<ClientboundLoginPacket, ServerConnection> {
    @Override
    public void accept(@NonNull ClientboundLoginPacket packet, @NonNull ServerConnection session) {
        // todo: verify channel is correct
        session.send(new ClientboundCustomPayloadPacket("brand", RefStrings.BRAND_SUPPLIER.get()));


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
        // todo: implement with new chat packets and formatting
//        if (CONFIG.client.extra.chat.hideChat) {
//            session.send(new ServerChatPacket("§7Chat is currently disabled. To enable chat, type §c/togglechat§7.", true));
//        }
//        if (CONFIG.client.extra.chat.hideWhispers) {
//            session.send(new ServerChatPacket("§7Whispers are currently disabled. To enable whispers, type §c/toggleprivatemsgs§7.", true));
//        }
//        if (CONFIG.client.extra.chat.showConnectionMessages) {
//            session.send(new ServerChatPacket("§7Connection messages enabled. To disable, type §c/toggleconnectionmsgs§7.", true));
//        }
//        if (CONFIG.client.extra.chat.hideDeathMessages) {
//            session.send(new ServerChatPacket("§7Death messages are currently disabled. To enable death messages, type §c/toggledeathmsgs§7.", true));
//        }
    }

    @Override
    public Class<ClientboundLoginPacket> getPacketClass() {
        return ClientboundLoginPacket.class;
    }
}
