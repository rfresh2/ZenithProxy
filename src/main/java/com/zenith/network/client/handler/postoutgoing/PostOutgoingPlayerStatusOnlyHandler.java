package com.zenith.network.client.handler.postoutgoing;

import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundMovePlayerStatusOnlyPacket;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.ClientEventLoopPacketHandler;

public class PostOutgoingPlayerStatusOnlyHandler implements ClientEventLoopPacketHandler<ServerboundMovePlayerStatusOnlyPacket, ClientSession> {
    @Override
    public boolean applyAsync(final ServerboundMovePlayerStatusOnlyPacket packet, final ClientSession session) {
        // todo: cache onground
//        CLIENT_LOG.info("Client set onGround: {}", packet.isOnGround());
        return true;
    }
}
