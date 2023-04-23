package com.zenith.server.handler.spectator.incoming.movement;

import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerPositionRotationPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntityHeadLookPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntityTeleportPacket;
import com.zenith.server.ServerConnection;
import com.zenith.util.handler.HandlerRegistry;
import com.zenith.util.spectator.SpectatorHelper;
import com.zenith.util.spectator.entity.mob.SpectatorEntityEnderDragon;
import lombok.NonNull;

public class PlayerPositionRotationSpectatorHandler implements HandlerRegistry.IncomingHandler<ClientPlayerPositionRotationPacket, ServerConnection> {
    @Override
    public boolean apply(@NonNull ClientPlayerPositionRotationPacket packet, @NonNull ServerConnection session) {
        session.getSpectatorPlayerCache()
                .setX(packet.getX())
                .setY(packet.getY())
                .setZ(packet.getZ())
                .setYaw((float) packet.getYaw())
                .setPitch((float) packet.getPitch());
        PlayerPositionRotationSpectatorHandler.updateSpectatorPosition(session);
        SpectatorHelper.checkSpectatorPositionOutOfRender(session);
        return false;
    }

    @Override
    public Class<ClientPlayerPositionRotationPacket> getPacketClass() {
        return ClientPlayerPositionRotationPacket.class;
    }

    // might move this elsewhere, kinda awkward being here
    public static void updateSpectatorPosition(final ServerConnection selfSession) {
        if (selfSession.isPlayerCam()) {
            return;
        }
        float yaw = getYaw(selfSession);
        selfSession.getProxy().getActiveConnections().stream()
                .filter(connection -> !connection.equals(selfSession))
                .forEach(connection -> {
                    connection.send(new ServerEntityTeleportPacket(
                            selfSession.getSpectatorEntityId(),
                            selfSession.getSpectatorPlayerCache().getX(),
                            selfSession.getSpectatorPlayerCache().getY(),
                            selfSession.getSpectatorPlayerCache().getZ(),
                            yaw,
                            selfSession.getSpectatorPlayerCache().getPitch(),
                            false
                    ));
                    connection.send(new ServerEntityHeadLookPacket(
                            selfSession.getSpectatorEntityId(),
                            yaw
                    ));
                });
        selfSession.send(new ServerEntityTeleportPacket(
                selfSession.getSpectatorEntityId(),
                selfSession.getSpectatorPlayerCache().getX(),
                selfSession.getSpectatorPlayerCache().getY(),
                selfSession.getSpectatorPlayerCache().getZ(),
                yaw,
                selfSession.getSpectatorPlayerCache().getPitch(),
                false
        ));
        selfSession.send(new ServerEntityHeadLookPacket(
                selfSession.getSpectatorEntityId(),
                yaw
        ));
    }

    public static float getYaw(final ServerConnection serverConnection) {
        // idk why but dragon is 180 degrees off from what you'd expect
        if (serverConnection.getSpectatorEntity() instanceof SpectatorEntityEnderDragon) {
            return serverConnection.getSpectatorPlayerCache().getYaw() - 180f;
        } else {
            return serverConnection.getSpectatorPlayerCache().getYaw();
        }
    }
}
