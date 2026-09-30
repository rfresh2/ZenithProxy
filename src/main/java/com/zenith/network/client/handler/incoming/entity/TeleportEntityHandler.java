package com.zenith.network.client.handler.incoming.entity;

import com.zenith.cache.data.entity.Entity;
import com.zenith.feature.spectator.SpectatorSync;
import com.zenith.network.client.ClientSession;
import com.zenith.network.codec.ClientEventLoopPacketHandler;
import com.zenith.util.math.MathHelper;
import com.zenith.util.math.MutableVec3d;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.PositionElement;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundTeleportEntityPacket;
import org.jspecify.annotations.NonNull;

import static com.zenith.Globals.CACHE;
import static com.zenith.Globals.CLIENT_LOG;

public class TeleportEntityHandler implements ClientEventLoopPacketHandler<ClientboundTeleportEntityPacket, ClientSession> {
    @Override
    public boolean applyAsync(@NonNull ClientboundTeleportEntityPacket packet, @NonNull ClientSession session) {
        Entity entity = CACHE.getEntityCache().get(packet.getId());
        if (entity != null) {
            var relatives = packet.getRelatives();
            var isPos = relatives.contains(PositionElement.X)
                || relatives.contains(PositionElement.Y)
                || relatives.contains(PositionElement.Z);
            var isControlledByLocalInstance = entity.isControlledByLocalInstance();
            var shouldLerp = !isControlledByLocalInstance || isPos;
            var xRel = relatives.contains(PositionElement.X) ? packet.getX() : 0;
            var yRel = relatives.contains(PositionElement.Y) ? packet.getY() : 0;
            var zRel = relatives.contains(PositionElement.Z) ? packet.getZ() : 0;
            var yawRel = relatives.contains(PositionElement.Y_ROT) ? packet.getYaw() : 0;
            var pitchRel = relatives.contains(PositionElement.X_ROT) ? packet.getPitch() : 0;
            var xPos = xRel + entity.getX();
            var yPos = yRel + entity.getY();
            var zPos = zRel + entity.getZ();
            var yaw = yawRel + entity.getYaw();
            var pitch = pitchRel + entity.getPitch();
            var deltaMoveVec = new MutableVec3d(packet.getDeltaX(), packet.getDeltaY(), packet.getDeltaZ());
            if (relatives.contains(PositionElement.ROTATE_DELTA)) {
                var yawRads = Math.toRadians(entity.getYaw() - yaw);
                var pitchRads = Math.toRadians(entity.getPitch() - pitch);
                deltaMoveVec.yRot(yawRads);
                deltaMoveVec.xRot(pitchRads);
            }
            deltaMoveVec.set(
                relatives.contains(PositionElement.DELTA_X) ? deltaMoveVec.getX() + packet.getDeltaX() : packet.getDeltaX(),
                relatives.contains(PositionElement.DELTA_Y) ? deltaMoveVec.getY() + packet.getDeltaY() : packet.getDeltaY(),
                relatives.contains(PositionElement.DELTA_Z) ? deltaMoveVec.getZ() + packet.getDeltaZ() : packet.getDeltaZ()
            );

            var farMovement = MathHelper.distanceSq3d(entity.lerpTargetX(), entity.lerpTargetY(), entity.lerpTargetZ(), xPos, yPos, zPos) > 4096.0;
            if (shouldLerp && !farMovement) {
                entity.lerpTo(xPos, yPos, zPos, yaw, pitch, 3);
                entity
                    .setVelX(deltaMoveVec.getX())
                    .setVelY(deltaMoveVec.getY())
                    .setVelZ(deltaMoveVec.getZ());
            } else {
                entity
                    .setX(packet.getX())
                    .setY(packet.getY())
                    .setZ(packet.getZ())
                    .setVelX(packet.getDeltaX())
                    .setVelY(packet.getDeltaY())
                    .setVelZ(packet.getDeltaZ())
                    .setYaw(packet.getYaw())
                    .setPitch(packet.getPitch());
            }

            if (isControlledByLocalInstance) { // todo: should also apply if player is an indirect passenger
                CACHE.getPlayerCache().getThePlayer()
                    .setX(packet.getX())
                    .setY(packet.getY())
                    .setZ(packet.getZ());
                SpectatorSync.syncPlayerPositionWithSpectators();
            }
            return true;
        } else {
            CLIENT_LOG.debug("Received ServerEntityTeleportPacket for invalid entity (id={})", packet.getId());
            return true;
        }
    }
}
