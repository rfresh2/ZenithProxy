package com.zenith.network.server.handler.spectator.incoming.movement;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.ClientboundRotateHeadPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.ClientboundTeleportEntityPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundMovePlayerPosRotPacket;
import com.zenith.feature.spectator.SpectatorUtils;
import com.zenith.feature.spectator.entity.mob.SpectatorEntityEnderDragon;
import com.zenith.network.registry.IncomingHandler;
import com.zenith.network.server.ServerConnection;
import lombok.NonNull;

public class PlayerPositionRotationSpectatorHandler implements IncomingHandler<ServerboundMovePlayerPosRotPacket, ServerConnection> {
    @Override
    public boolean apply(@NonNull ServerboundMovePlayerPosRotPacket packet, @NonNull ServerConnection session) {
        if (!session.isLoggedIn()) return false;
        session.getSpectatorPlayerCache()
                .setX(packet.getX())
                .setY(packet.getY())
                .setZ(packet.getZ())
                .setYaw(packet.getYaw())
                .setPitch(packet.getPitch());
        PlayerPositionRotationSpectatorHandler.updateSpectatorPosition(session);
        SpectatorUtils.checkSpectatorPositionOutOfRender(session);
        return false;
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
                    connection.send(new ClientboundTeleportEntityPacket(
                            selfSession.getSpectatorEntityId(),
                            selfSession.getSpectatorPlayerCache().getX(),
                            selfSession.getSpectatorPlayerCache().getY(),
                            selfSession.getSpectatorPlayerCache().getZ(),
                            yaw,
                            selfSession.getSpectatorPlayerCache().getPitch(),
                            false
                    ));
                    connection.send(new ClientboundRotateHeadPacket(
                            selfSession.getSpectatorEntityId(),
                            yaw
                    ));
                });
        selfSession.send(new ClientboundTeleportEntityPacket(
                selfSession.getSpectatorEntityId(),
                selfSession.getSpectatorPlayerCache().getX(),
                selfSession.getSpectatorPlayerCache().getY(),
                selfSession.getSpectatorPlayerCache().getZ(),
                yaw,
                selfSession.getSpectatorPlayerCache().getPitch(),
                false
        ));
        selfSession.send(new ClientboundRotateHeadPacket(
                selfSession.getSpectatorEntityId(),
                yaw
        ));
    }

    public static float getYaw(final ServerConnection serverConnection) {
        // idk why but dragon is 180 degrees off from what you'd expect
        // todo: is this still true in 1.20?
        if (serverConnection.getSpectatorEntity() instanceof SpectatorEntityEnderDragon) {
            return serverConnection.getSpectatorPlayerCache().getYaw() - 180f;
        } else {
            return serverConnection.getSpectatorPlayerCache().getYaw();
        }
    }
}
