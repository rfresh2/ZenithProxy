package com.zenith.feature.coordobf.handlers.outbound;

import com.zenith.module.impl.CoordObfuscation;
import com.zenith.network.codec.PacketHandler;
import com.zenith.network.server.ServerSession;
import org.geysermc.mcprotocollib.protocol.data.game.WeightedList;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundExplodePacket;

import java.util.Collections;

import static com.zenith.Globals.MODULE;

public class COExplodeHandler implements PacketHandler<ClientboundExplodePacket, ServerSession> {
    @Override
    public ClientboundExplodePacket apply(final ClientboundExplodePacket packet, final ServerSession session) {
        CoordObfuscation coordObf = MODULE.get(CoordObfuscation.class);
        return new ClientboundExplodePacket(
            coordObf.getCoordOffset(session).offsetX(packet.getCenterX()),
            packet.getCenterY(),
            coordObf.getCoordOffset(session).offsetZ(packet.getCenterZ()),
            packet.getRadius(),
            packet.getBlockCount(),
            packet.getPlayerKnockback(),
            coordObf.getCoordOffset(session).offsetParticle(packet.getExplosionParticle()),
            packet.getExplosionSound(),
            new WeightedList<>(Collections.emptyList())
        );
    }
}
