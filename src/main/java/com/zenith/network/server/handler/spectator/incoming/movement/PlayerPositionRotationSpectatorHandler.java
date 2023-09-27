package com.zenith.network.server.handler.spectator.incoming.movement;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.ClientboundRotateHeadPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.ClientboundTeleportEntityPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundMovePlayerPosRotPacket;
import com.zenith.feature.spectator.SpectatorUtils;
import com.zenith.feature.spectator.entity.mob.SpectatorEntityEnderDragon;
import com.zenith.network.registry.IncomingHandler;
import com.zenith.network.server.ServerConnection;
import com.zenith.util.EntityStats;
import com.zenith.util.math.MathHelper;
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
        EntityStats entityStats = new EntityStats();

//        System.out.println(selfSession.getSpectatorEntity().getSelfEntityMetadata());
// I don't know how to properly get the string "cat" or whatever the config is set to. I'm stupid. :)
        double specEntityEyeHeight = entityStats.getEntityData("cat").getEyeHeight();
        double specEntityTotalWidth = entityStats.getEntityData("cat").getTotalWidth();
        float yaw = getYaw(selfSession);
        float pitch = selfSession.getSpectatorPlayerCache().getPitch();


        selfSession.getProxy().getActiveConnections().stream()
                .filter(connection -> !connection.equals(selfSession))
                .forEach(connection -> {
                    connection.send(new ClientboundTeleportEntityPacket(
                            selfSession.getSpectatorEntityId(),
                            selfSession.getSpectatorPlayerCache().getX(),
                            selfSession.getSpectatorPlayerCache().getY()+playerEyeHeight-specEntityEyeHeight,
                            selfSession.getSpectatorPlayerCache().getZ(),
                            yaw,
                            pitch,
                            false
                    ));
                    connection.send(new ClientboundRotateHeadPacket(
                            selfSession.getSpectatorEntityId(),
                            yaw
                    ));
                });

        double[] doubles = MathHelper.translateEntity(selfSession.getSpectatorPlayerCache().getX(),
                selfSession.getSpectatorPlayerCache().getY() + playerEyeHeight - specEntityEyeHeight,
                selfSession.getSpectatorPlayerCache().getZ(),
                yaw, pitch,
                (specEntityTotalWidth*-1)-0.25);
        selfSession.send(new ClientboundTeleportEntityPacket(
                selfSession.getSpectatorEntityId(),
                doubles[0],
                doubles[1],
                doubles[2],
                yaw,
                pitch,
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
