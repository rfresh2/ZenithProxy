package com.zenith.feature.coordobf.handlers.outbound;

import com.zenith.Proxy;
import com.zenith.module.impl.CoordObfuscation;
import com.zenith.network.codec.PacketHandler;
import com.zenith.network.server.ServerSession;
import org.geysermc.mcprotocollib.protocol.data.game.entity.type.EntityType;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundAddEntityPacket;

import static com.zenith.Globals.CONFIG;
import static com.zenith.Globals.MODULE;

public class COAddEntityHandler implements PacketHandler<ClientboundAddEntityPacket, ServerSession> {
    @Override
    public ClientboundAddEntityPacket apply(final ClientboundAddEntityPacket packet, final ServerSession session) {
        CoordObfuscation coordObf = MODULE.get(CoordObfuscation.class);
        if (CONFIG.client.extra.coordObfuscation.disconnectWhileEyeOfEnderPresent) {
            if (packet.getType() == EntityType.EYE_OF_ENDER) {
                coordObf.disconnect(session, coordObf.genericDisconnectReason, "An eye of ender was spawned in the world");
                return null;
            } else if (packet.getType() == EntityType.ITEM) {
                Proxy.getInstance().getClient().getClientEventLoop().execute(() -> {
                    if (coordObf.isEnderEyeInWorld()) {
                        coordObf.disconnect(session, coordObf.genericDisconnectReason, "An eye of ender is in the world");
                    }
                });
            }
        }
        return new ClientboundAddEntityPacket(
            packet.getEntityId(),
            packet.getUuid(),
            packet.getType(),
            packet.getData(),
            coordObf.getCoordOffset(session).offsetX(packet.getX()),
            packet.getY(),
            coordObf.getCoordOffset(session).offsetZ(packet.getZ()),
            packet.getMovement(),
            packet.getYaw(),
            packet.getHeadYaw(),
            packet.getPitch()
        );
    }
}
