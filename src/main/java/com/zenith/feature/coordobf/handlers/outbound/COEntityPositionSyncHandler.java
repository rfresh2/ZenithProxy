package com.zenith.feature.coordobf.handlers.outbound;

import com.zenith.cache.data.entity.EntityStandard;
import com.zenith.module.impl.CoordObfuscation;
import com.zenith.network.codec.PacketHandler;
import com.zenith.network.server.ServerSession;
import org.geysermc.mcprotocollib.protocol.data.game.entity.type.EntityType;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundEntityPositionSyncPacket;

import static com.zenith.Globals.CACHE;
import static com.zenith.Globals.MODULE;

public class COEntityPositionSyncHandler implements PacketHandler<ClientboundEntityPositionSyncPacket, ServerSession> {
    @Override
    public ClientboundEntityPositionSyncPacket apply(final ClientboundEntityPositionSyncPacket packet, final ServerSession session) {
        var coordObf = MODULE.get(CoordObfuscation.class);
        var entity = CACHE.getEntityCache().get(packet.getId());
        if (entity == null && !coordObf.getSpectatorEntityIds().contains(packet.getId())) {
            return null;
        }
        if (entity instanceof EntityStandard e) {
            if (e.getEntityType() == EntityType.EYE_OF_ENDER) {
                return null;
            }
        }

        if (entity != null && !entity.getPassengerIds().isEmpty()
            && entity.getPassengerIds().contains(CACHE.getPlayerCache().getEntityId())
        ) {
            var endX = packet.getEndPosition() != null
                ? packet.getEndPosition().getX()
                : packet.getSteps().getLast().position().getX();
            var endZ = packet.getEndPosition() != null
                ? packet.getEndPosition().getZ()
                : packet.getSteps().getLast().position().getZ();
            coordObf.playerMovePos(session, endX, endZ);
        }
        return new ClientboundEntityPositionSyncPacket(
            packet.getId(),
            packet.isStepped(),
            packet.getEndPosition() != null
                ? coordObf.getCoordOffset(session).offsetVector(packet.getEndPosition())
                : null,
            packet.getSteps() != null
                ? packet.getSteps().stream()
                    .map(s -> new ClientboundEntityPositionSyncPacket.PositionStep(coordObf.getCoordOffset(session).offsetVector(s.position()), s.tickOffset()))
                    .toList()
                : null,
            packet.getYaw(),
            packet.getPitch(),
            packet.isOnGround()
        );
    }
}
