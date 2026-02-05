package com.zenith.network.server.handler.player.incoming;

import com.zenith.network.codec.PacketHandler;
import com.zenith.network.server.ServerSession;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundMovePlayerPosRotPacket;

import static com.zenith.Globals.SERVER_LOG;

public class SPlayerPositionRotHandler implements PacketHandler<ServerboundMovePlayerPosRotPacket, ServerSession> {
    @Override
    public ServerboundMovePlayerPosRotPacket apply(final ServerboundMovePlayerPosRotPacket packet, final ServerSession session) {
        if (session.isSpawned()) return packet;
        else {
            session.setSpawned(true);
            session.setSpawning(false);
            SERVER_LOG.debug("[{}] Cancelling pre-spawn position packet: {}", session.getName(), packet);
            return null;
        }
    }
}
