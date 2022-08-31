package com.zenith.server.handler.spectator.incoming;

import com.github.steveice10.mc.protocol.packet.ingame.client.ClientChatPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerChatPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerSwitchCameraPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntityDestroyPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.player.ServerPlayerPositionRotationPacket;
import com.zenith.server.ServerConnection;
import com.zenith.util.handler.HandlerRegistry;
import com.zenith.util.spectator.SpectatorEntityRegistry;

import static com.zenith.util.Constants.CACHE;

public class ServerChatSpectatorHandler implements HandlerRegistry.IncomingHandler<ClientChatPacket, ServerConnection> {

    @Override
    public boolean apply(ClientChatPacket packet, ServerConnection session) {
        if (packet.getMessage().startsWith("!help")) {
            session.send(new ServerChatPacket("§9§lSpectator commands:", true));
            session.send(new ServerChatPacket("§2Prefix : \"!\"", true));
            session.send(new ServerChatPacket("", true));
            session.send(new ServerChatPacket("§7§chelp §7- §8Display help menu", true));
            session.send(new ServerChatPacket("§7§cplayercam §7- §8Set camera to the player", true));
            session.send(new ServerChatPacket("§7§cetoggle §7- §8Hide your entity from yourself", true));
            session.send(new ServerChatPacket("§7§ce §7- §8List spectator entities. Change with \"!e <entity>\"", true));
        } else if (packet.getMessage().startsWith("!m")) {
            session.getProxy().getClient().send(new ClientChatPacket(packet.getMessage().substring(2).trim()));
        } else if (packet.getMessage().toLowerCase().startsWith("!etoggle")) {
            session.setShowSelfEntity(!session.isShowSelfEntity());
            if (session.isShowSelfEntity()) {
                session.send(session.getEntitySpawnPacket());
                session.send(session.getEntityMetadataPacket());
            } else {
                session.send(new ServerEntityDestroyPacket(session.getSpectatorEntityId()));
            }
        } else if (packet.getMessage().startsWith("!e")) {
            String entityId = packet.getMessage().substring(2).trim();
            boolean spectatorEntitySet = session.setSpectatorEntity(entityId);
            if (spectatorEntitySet) {
                // respawn entity on all connections
                session.getProxy().getServerConnections().forEach(connection -> {
                    connection.send(new ServerEntityDestroyPacket(session.getSpectatorEntityId()));
                    if (!connection.equals(session) || session.isShowSelfEntity()) {
                        connection.send(session.getEntitySpawnPacket());
                        connection.send(session.getEntityMetadataPacket());
                    }
                });
                session.send(new ServerChatPacket("§9Updated entity to: " + entityId + "§r", true));
            } else {
                session.send(new ServerChatPacket("§cNo entity found with id: " + entityId + "§r", true));
                session.send(new ServerChatPacket("§cValid id's: " + String.join(", ", SpectatorEntityRegistry.getEntityIdentifiers()) + "§r", true));
            }
        } else if (packet.getMessage().toLowerCase().startsWith("!playercam")) {
            session.setPlayerCam(!session.isPlayerCam());
            if (session.isPlayerCam()) {
                session.send(new ServerSwitchCameraPacket(CACHE.getPlayerCache().getEntityId()));
                session.getProxy().getServerConnections().forEach(connection -> {
                    connection.send(new ServerEntityDestroyPacket(session.getSpectatorEntityId()));
                });
            } else {
                session.send(new ServerSwitchCameraPacket(session.getSpectatorSelfEntityId()));
                session.setAllowSpectatorServerPlayerPosRotate(true);
                session.send(new ServerPlayerPositionRotationPacket(
                        CACHE.getPlayerCache().getX(),
                        CACHE.getPlayerCache().getY(),
                        CACHE.getPlayerCache().getZ(),
                        CACHE.getPlayerCache().getYaw(),
                        CACHE.getPlayerCache().getPitch(),
                        12345678
                ));
                session.setAllowSpectatorServerPlayerPosRotate(false);
                session.getProxy().getServerConnections().forEach(connection -> {
                    if (!connection.equals(session) || session.isShowSelfEntity()) {
                        connection.send(session.getEntitySpawnPacket());
                        connection.send(session.getEntityMetadataPacket());
                    }
                });
            }
        } else {
            session.getProxy().getServerConnections().forEach(connection -> {
                connection.send(new ServerChatPacket("§c" + session.getProfileCache().getProfile().getName() + " > " + packet.getMessage() + "§r", true));
            });
        }
        return false;
    }

    @Override
    public Class<ClientChatPacket> getPacketClass() {
        return ClientChatPacket.class;
    }
}
