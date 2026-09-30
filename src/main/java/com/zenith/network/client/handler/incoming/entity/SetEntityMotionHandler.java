package com.zenith.network.client.handler.incoming.entity;

import com.zenith.Proxy;
import com.zenith.network.client.ClientSession;
import com.zenith.network.codec.ClientEventLoopPacketHandler;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundSetEntityMotionPacket;

import static com.zenith.Globals.BOT;
import static com.zenith.Globals.CACHE;

public class SetEntityMotionHandler implements ClientEventLoopPacketHandler<ClientboundSetEntityMotionPacket, ClientSession> {
    @Override
    public boolean applyAsync(final ClientboundSetEntityMotionPacket packet, final ClientSession session) {
        if (!Proxy.getInstance().hasActivePlayer() && packet.getEntityId() == CACHE.getPlayerCache().getEntityId()) {
            BOT.handleSetMotion(packet.getMotionX(), packet.getMotionY(), packet.getMotionZ());
        }
        return true;
    }
}
