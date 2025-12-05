package com.zenith.network.client.handler.incoming.entity;

import com.zenith.feature.spectator.SpectatorSync;
import com.zenith.network.client.ClientSession;
import com.zenith.network.codec.ClientEventLoopPacketHandler;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntStack;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundMoveVehiclePacket;

import static com.zenith.Globals.BOT;
import static com.zenith.Globals.CACHE;

public class MoveVehicleHandler implements ClientEventLoopPacketHandler<ClientboundMoveVehiclePacket, ClientSession> {
    @Override
    public boolean applyAsync(final ClientboundMoveVehiclePacket packet, final ClientSession session) {
        var vehicleId = CACHE.getPlayerCache().getThePlayer().getVehicleId();
        var vehicleEntity = CACHE.getEntityCache().get(vehicleId);
        IntStack toUpdate = new IntArrayList();
        // todo: sync nested ridden entity positions
        //  currently this is only updating the root vehicle
        while (vehicleEntity != null && vehicleEntity.isInVehicle()) {
            toUpdate.push(vehicleEntity.getEntityId());
            vehicleEntity = CACHE.getEntityCache().get(vehicleEntity.getVehicleId());
        }
        if (vehicleEntity != null) {
            vehicleEntity.setX(packet.getX());
            vehicleEntity.setY(packet.getY());
            vehicleEntity.setZ(packet.getZ());
            vehicleEntity.setYaw(packet.getYaw());
            vehicleEntity.setPitch(packet.getPitch());
        }
        while (!toUpdate.isEmpty()) {
            var passengerId = toUpdate.popInt();
            var passengerEntity = CACHE.getEntityCache().get(passengerId);
            if (passengerEntity == null) continue;
            var passengerAttachmentData = passengerEntity.getEntityData().entityAttachment();
            var riddenEntity = CACHE.getEntityCache().get(passengerEntity.getVehicleId());
            if (riddenEntity == null) continue;
            var riddenAttachmentData = riddenEntity.getEntityData().entityAttachment();
            if (passengerAttachmentData == null || riddenAttachmentData == null) continue;
            var vehicleAttachY = riddenEntity.getY() + riddenAttachmentData.passenger();
            var passengerAttachY = passengerAttachmentData.vehicle();
            passengerEntity.setY(vehicleAttachY - passengerAttachY);
            passengerEntity.setX(riddenEntity.getX());
            passengerEntity.setZ(riddenEntity.getZ());
        }
        BOT.syncFromCache(true);
        SpectatorSync.syncPlayerPositionWithSpectators();
        return true;
    }
}
