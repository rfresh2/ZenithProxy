package com.zenith.util.spectator;

import com.github.steveice10.mc.protocol.data.game.entity.EquipmentSlot;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntityEquipmentPacket;
import com.zenith.Proxy;
import com.zenith.server.ServerConnection;

import static com.zenith.util.Constants.CACHE;

public final class SpectatorHelper {

    public static void syncPlayerEquipmentWithSpectatorsFromCache(final Proxy proxy) {
        sendSpectatorsEquipment(proxy, EquipmentSlot.OFF_HAND);
        sendSpectatorsEquipment(proxy, EquipmentSlot.HELMET);
        sendSpectatorsEquipment(proxy, EquipmentSlot.CHESTPLATE);
        sendSpectatorsEquipment(proxy, EquipmentSlot.LEGGINGS);
        sendSpectatorsEquipment(proxy, EquipmentSlot.BOOTS);
        sendSpectatorsEquipment(proxy, EquipmentSlot.MAIN_HAND);
    }

    private static void sendSpectatorsEquipment(final Proxy proxy, final EquipmentSlot equipmentSlot) {
        proxy.getSpectatorConnections().forEach(connection -> {
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
