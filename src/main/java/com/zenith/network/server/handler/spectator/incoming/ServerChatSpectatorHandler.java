package com.zenith.network.server.handler.spectator.incoming;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundSetCameraPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundSystemChatPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.ClientboundRemoveEntitiesPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.ServerboundChatPacket;
import com.zenith.Proxy;
import com.zenith.cache.data.entity.Entity;
import com.zenith.feature.spectator.SpectatorEntityRegistry;
import com.zenith.feature.spectator.SpectatorSync;
import com.zenith.network.registry.PacketHandler;
import com.zenith.network.server.ServerConnection;
import com.zenith.util.ComponentSerializer;
import net.kyori.adventure.text.Component;

import static com.zenith.Shared.*;

public class ServerChatSpectatorHandler implements PacketHandler<ServerboundChatPacket, ServerConnection> {
    @Override
    public ServerboundChatPacket apply(ServerboundChatPacket packet, ServerConnection session) {
        if (CONFIG.inGameCommands.enable) {
            EXECUTOR.execute(() -> {
                if (IN_GAME_COMMAND.getCommandPattern().matcher(packet.getMessage()).find()) {
                    TERMINAL_LOG.info("{} executed spectator command: {}", session.getProfileCache().getProfile().getName(), packet.getMessage());
                    handleCommandInput(packet.getMessage(), session);
                } else {
                    Proxy.getInstance().getActiveConnections().forEach(connection -> {
                        connection.send(new ClientboundSystemChatPacket(ComponentSerializer.minedown("&c" + session.getProfileCache().getProfile().getName() + " > " + packet.getMessage() + "&r"), false));
                    });
                }
            });
        }
        return null;
    }

    private void handleCommandInput(final String message, final ServerConnection session) {
        final String fullCommandAndArgs = message.substring(CONFIG.inGameCommands.prefix.length()).trim(); // cut off the prefix
        final String command = fullCommandAndArgs.split(" ")[0]; // first word is the command
        switch (command) {
            case "help" -> {
                session.send(new ClientboundSystemChatPacket(ComponentSerializer.minedown("&9&lSpectator commands:"), false));
                session.send(new ClientboundSystemChatPacket(ComponentSerializer.minedown("&2Prefix : \"" + CONFIG.inGameCommands.prefix + "\""), false));
                session.send(new ClientboundSystemChatPacket(Component.text(""), false));
                session.send(new ClientboundSystemChatPacket(ComponentSerializer.minedown("&7&chelp &7- &8Display help menu"), false));
                if (CONFIG.server.spectator.spectatorPublicChatEnabled)
                    session.send(new ClientboundSystemChatPacket(ComponentSerializer.minedown("&7&cm &7- &8Send public chats"), false));
                session.send(new ClientboundSystemChatPacket(ComponentSerializer.minedown("&7&cplayercam &7- &8Set camera to the player"), false));
                session.send(new ClientboundSystemChatPacket(ComponentSerializer.minedown("&7&cetoggle &7- &8Hide your entity from yourself"), false));
                session.send(new ClientboundSystemChatPacket(ComponentSerializer.minedown("&7&ce &7- &8List spectator entities. Change with \"!e <entity>\""), false));
            }
            case "m" -> {
                if (CONFIG.server.spectator.spectatorPublicChatEnabled) {
                    String chatMessageContent = fullCommandAndArgs.substring(1).trim();
                    Proxy.getInstance().getClient().send(new ServerboundChatPacket(chatMessageContent));
                } else {
                    session.send(new ClientboundSystemChatPacket(ComponentSerializer.minedown("&cSpectator chat disabled&r"), false));
                }
            }
            case "etoggle" -> {
                session.setShowSelfEntity(!session.isShowSelfEntity());
                if (session.isShowSelfEntity()) {
                    session.send(session.getEntitySpawnPacket());
                    session.send(session.getEntityMetadataPacket());
                } else {
                    session.send(new ClientboundRemoveEntitiesPacket(new int[]{session.getSpectatorEntityId()}));
                }
                session.send(new ClientboundSystemChatPacket(ComponentSerializer.minedown("&9Show self entity toggled " + (session.isShowSelfEntity() ? "on!" : "off!") + "&r"), false));
            }
            case "e" -> {
                String entityId = fullCommandAndArgs.substring(1).trim();
                boolean spectatorEntitySet = session.setSpectatorEntity(entityId);
                if (spectatorEntitySet) {
                    // respawn entity on all connections
                    Proxy.getInstance().getActiveConnections().forEach(connection -> {
                        connection.send(new ClientboundRemoveEntitiesPacket(new int[]{session.getSpectatorEntityId()}));
                        if (!connection.equals(session) || session.isShowSelfEntity()) {
                            connection.send(session.getEntitySpawnPacket());
                            connection.send(session.getEntityMetadataPacket());
                            SpectatorSync.updateSpectatorPosition(session);
                        }
                    });
                    session.send(new ClientboundSystemChatPacket(ComponentSerializer.minedown("&9Updated entity to: " + entityId + "&r"), false));
                } else {
                    session.send(new ClientboundSystemChatPacket(ComponentSerializer.minedown("&cNo entity found with id: " + entityId + "&r"), false));
                    session.send(new ClientboundSystemChatPacket(ComponentSerializer.minedown("&cValid id's: " + String.join(", ", SpectatorEntityRegistry.getEntityIdentifiers()) + "&r"), false));
                }
            }
            case "playercam" -> {
                final Entity existingTarget = session.getCameraTarget();
                if (existingTarget != null) {
                    session.setCameraTarget(null);
                    session.send(new ClientboundSetCameraPacket(session.getSpectatorSelfEntityId()));
                    SpectatorSync.syncSpectatorPositionToEntity(session, existingTarget);
                    session.send(new ClientboundSystemChatPacket(ComponentSerializer.minedown("&9Exited playercam!&r"), false));
                } else {
                    session.setCameraTarget(CACHE.getPlayerCache().getThePlayer());
                    session.send(new ClientboundSetCameraPacket(CACHE.getPlayerCache().getEntityId()));
                    Proxy.getInstance().getActiveConnections().forEach(connection -> {
                        connection.send(new ClientboundRemoveEntitiesPacket(new int[]{session.getSpectatorEntityId()}));
                    });
                    session.send(new ClientboundSystemChatPacket(ComponentSerializer.minedown("&9Entered playercam!&r"), false));
                }
            }
            default -> handleCommandInput("help", session);
        }
    }
}
