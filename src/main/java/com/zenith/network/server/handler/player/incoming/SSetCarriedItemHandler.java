package com.zenith.network.server.handler.player.incoming;

import com.zenith.network.codec.PacketHandler;
import com.zenith.network.server.ServerSession;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundSetCarriedItemPacket;

import static com.zenith.Globals.CACHE;

public class SSetCarriedItemHandler implements PacketHandler<ServerboundSetCarriedItemPacket, ServerSession> {
    @Override
    public ServerboundSetCarriedItemPacket apply(final ServerboundSetCarriedItemPacket packet, final ServerSession session) {
        var isClientLoaded = session.isClientLoaded() || (System.nanoTime() >= session.getClientLoadedTimeout());
        if (!isClientLoaded && CACHE.getPlayerCache().isClientLoaded()) {
            return null;
        }
        return packet;
    }
}
