package com.zenith.network.client.handler.postoutgoing;

import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundMovePlayerStatusOnlyPacket;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.PostOutgoingAsyncHandler;

public class PostOutgoingPlayerStatusOnlyHandler implements PostOutgoingAsyncHandler<ServerboundMovePlayerStatusOnlyPacket, ClientSession> {
    @Override
    public void acceptAsync(final ServerboundMovePlayerStatusOnlyPacket packet, final ClientSession session) {
        // todo: cache onground

//        CLIENT_LOG.info("Client set onGround: {}", packet.isOnGround());
    }
}
