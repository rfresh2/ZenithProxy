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
        double playerEyeHeight = 1.62;
        double specEntityEyeHeight = selfSession.getSpectatorEntity().getEyeHeight();
        double specEntityTotalWidth = selfSession.getSpectatorEntity().getTotalWidth();
        float pitch = selfSession.getSpectatorPlayerCache().getPitch();

        double distance = (specEntityTotalWidth * -0.5) - 0.5;
        double yawRadians = Math.toRadians(selfSession.getSpectatorPlayerCache().getYaw());
        double pitchRadians = Math.toRadians(pitch);

        double xOffset = -Math.sin(yawRadians) * Math.cos(pitchRadians) * distance;
        double yOffset = -Math.sin(pitchRadians) * distance;
        double zOffset = Math.cos(yawRadians) * Math.cos(pitchRadians) * distance;

        double newX = selfSession.getSpectatorPlayerCache().getX() + xOffset;
        double newY = selfSession.getSpectatorPlayerCache().getY() + playerEyeHeight - specEntityEyeHeight + yOffset;
        double newZ = selfSession.getSpectatorPlayerCache().getZ() + zOffset;

        selfSession.getProxy().getActiveConnections().stream()
                .filter(connection -> !connection.equals(selfSession))
                .forEach(connection -> {
                    connection.send(new ClientboundTeleportEntityPacket(
                            selfSession.getSpectatorEntityId(),
                            newX,
                            newY,
                            newZ,
                            getDisplayYaw(selfSession),
                            pitch,
                            false
                    ));
                    connection.send(new ClientboundRotateHeadPacket(
                            selfSession.getSpectatorEntityId(),
                            getDisplayYaw(selfSession)
                    ));
                });


        selfSession.send(new ClientboundTeleportEntityPacket(
                selfSession.getSpectatorEntityId(),
                newX,
                newY,
                newZ,
                getDisplayYaw(selfSession),
                pitch,
                false
        ));
        selfSession.send(new ClientboundRotateHeadPacket(
                selfSession.getSpectatorEntityId(),
                getDisplayYaw(selfSession)
        ));
    }

    public static float getDisplayYaw(final ServerConnection serverConnection) {
        // idk why but dragon is displayed 180 degrees off from what you'd expect
        // yes, it is still backwards in 1.20.
        if (serverConnection.getSpectatorEntity() instanceof SpectatorEntityEnderDragon) {
            return serverConnection.getSpectatorPlayerCache().getYaw() - 180f;
        } else {
            return serverConnection.getSpectatorPlayerCache().getYaw();
        }
    }
}
