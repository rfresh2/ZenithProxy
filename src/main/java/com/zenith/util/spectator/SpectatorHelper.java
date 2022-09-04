package com.zenith.util.spectator;

import com.github.steveice10.mc.protocol.data.game.entity.EquipmentSlot;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntityEquipmentPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntityHeadLookPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntityTeleportPacket;
import com.zenith.Proxy;
import com.zenith.server.ServerConnection;

import static com.zenith.util.Constants.CACHE;

public final class SpectatorHelper {

    public static void syncPlayerEquipmentWithSpectatorsFromCache() {
        sendSpectatorsEquipment(EquipmentSlot.OFF_HAND);
        sendSpectatorsEquipment(EquipmentSlot.HELMET);
        sendSpectatorsEquipment(EquipmentSlot.CHESTPLATE);
        sendSpectatorsEquipment(EquipmentSlot.LEGGINGS);
        sendSpectatorsEquipment(EquipmentSlot.BOOTS);
        sendSpectatorsEquipment(EquipmentSlot.MAIN_HAND);
    }

    public static void syncPlayerPositionWithSpectators() {
        Proxy.getInstance().getSpectatorConnections().forEach(connection -> {
            connection.send(new ServerEntityTeleportPacket(
                    CACHE.getPlayerCache().getEntityId(),
                    CACHE.getPlayerCache().getX(),
                    CACHE.getPlayerCache().getY(),
                    CACHE.getPlayerCache().getZ(),
                    CACHE.getPlayerCache().getYaw(),
                    CACHE.getPlayerCache().getPitch(),
                    false // idk if this will break any rendering or not
            ));
            connection.send(new ServerEntityHeadLookPacket(
                    CACHE.getPlayerCache().getEntityId(),
                    CACHE.getPlayerCache().getYaw()
            ));
        });
    }

    private static void sendSpectatorsEquipment(final EquipmentSlot equipmentSlot) {
        Proxy.getInstance().getSpectatorConnections().forEach(connection -> {
            sendSpectatorsEquipment(connection, equipmentSlot);
        });
    }

    private static void sendSpectatorsEquipment(final ServerConnection connection, final EquipmentSlot equipmentSlot) {
        connection.send(new ServerEntityEquipmentPacket(
                CACHE.getPlayerCache().getEntityId(),
                equipmentSlot,
                CACHE.getPlayerCache().getThePlayer().getEquipment().get(equipmentSlot)
        ));
    }
}
