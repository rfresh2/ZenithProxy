package com.zenith.network.server.handler.spectator.incoming;

import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.zenith.Proxy;
import com.zenith.cache.data.entity.Entity;
import com.zenith.feature.spectator.SpectatorEntityRegistry;
import com.zenith.feature.spectator.SpectatorSync;
import com.zenith.network.registry.PacketHandler;
import com.zenith.network.server.ServerConnection;
import com.zenith.util.ComponentSerializer;
import net.kyori.adventure.text.Component;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundSetCameraPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundSystemChatPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundRemoveEntitiesPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundChatPacket;

import java.util.Optional;

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
                    var chatMessage = ComponentSerializer.minedown("&c" + session.getProfileCache().getProfile().getName() + " > " + packet.getMessage() + "&r");
                    SERVER_LOG.info("{}", ComponentSerializer.serializeJson(chatMessage));
                    var connections = Proxy.getInstance().getActiveConnections().getArray();
                    for (int i = 0; i < connections.length; i++) {
                        var connection = connections[i];
                        connection.send(new ClientboundSystemChatPacket(chatMessage, false));
                    }
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
                session.send(new ClientboundSystemChatPacket(ComponentSerializer.minedown("&aPrefix : \"" + CONFIG.inGameCommands.prefix + "\""), false));
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
                    var connections = Proxy.getInstance().getActiveConnections().getArray();
                    for (int i = 0; i < connections.length; i++) {
                        var connection = connections[i];
                        connection.send(new ClientboundRemoveEntitiesPacket(new int[]{session.getSpectatorEntityId()}));
                        if (!connection.equals(session) || session.isShowSelfEntity()) {
                            connection.send(session.getEntitySpawnPacket());
                            connection.send(session.getEntityMetadataPacket());
                            SpectatorSync.updateSpectatorPosition(session);
                        }
                    }
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
                    var connections = Proxy.getInstance().getActiveConnections().getArray();
                    for (int i = 0; i < connections.length; i++) {
                        var connection = connections[i];
                        connection.send(new ClientboundRemoveEntitiesPacket(new int[]{session.getSpectatorEntityId()}));
                    }
                    session.send(new ClientboundSystemChatPacket(ComponentSerializer.minedown("&9Entered playercam!&r"), false));
                }
            }
            case "swap" -> {
                var spectatorProfile = session.getProfileCache().getProfile();
                if (spectatorProfile == null) return;
                if (!PLAYER_LISTS.getWhitelist().contains(spectatorProfile.getId())) {
                    session.send(new ClientboundSystemChatPacket(ComponentSerializer.minedown("&cYou are not whitelisted!&r"), false));
                    return;
                }
                if (Proxy.getInstance().getActivePlayer() != null) {
                    session.send(new ClientboundSystemChatPacket(ComponentSerializer.minedown("&cSomeone is already controlling the player!&r"), false));
                    return;
                }
                if (CONFIG.server.viaversion.enabled) {
                    Optional<ProtocolVersion> viaClientProtocolVersion = Via.getManager().getConnectionManager().getConnectedClients().values().stream()
                        .filter(client -> client.getChannel() == session.getSession().getChannel())
                        .map(con -> con.getProtocolInfo().protocolVersion())
                        .findFirst();
                    if (viaClientProtocolVersion.isPresent() && viaClientProtocolVersion.get().olderThan(ProtocolVersion.v1_20_5)) {
                        session.send(new ClientboundSystemChatPacket(ComponentSerializer.minedown("&cUnsupported Client MC Version&r"), false));
                        return;
                    }
                }
                session.transferToControllingPlayer(CONFIG.server.getProxyAddressForTransfer(), CONFIG.server.getProxyPortForTransfer());
            }
            default -> handleCommandInput("help", session);
        }
    }
}
