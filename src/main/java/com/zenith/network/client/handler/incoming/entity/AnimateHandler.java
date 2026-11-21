package com.zenith.network.client.handler.incoming.entity;

import com.zenith.Proxy;
import com.zenith.network.client.ClientSession;
import com.zenith.network.codec.ClientEventLoopPacketHandler;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.Hand;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundAnimatePacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundSwingPacket;

import static com.zenith.Globals.CACHE;

public class AnimateHandler implements ClientEventLoopPacketHandler<ClientboundAnimatePacket, ClientSession> {
    @Override
    public boolean applyAsync(final ClientboundAnimatePacket packet, final ClientSession session) {
        if (packet.getEntityId() != CACHE.getPlayerCache().getEntityId()) return true;
        if (packet.getAnimation() == null) return true;
        if (Proxy.getInstance().hasActivePlayer()) return true;
	    switch (packet.getAnimation()) {
            case SWING_ARM -> session.send(new ServerboundSwingPacket(Hand.MAIN_HAND));
            case SWING_OFFHAND -> session.send(new ServerboundSwingPacket(Hand.OFF_HAND));
        }
        return true;
    }
}
