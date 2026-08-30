package com.zenith.network.server.handler.spectator.incoming;

import com.zenith.Proxy;
import com.zenith.command.api.CommandContext;
import com.zenith.network.codec.PacketHandler;
import com.zenith.network.server.ServerSession;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundChatCommandPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundChatCommandSignedPacket;

import static com.zenith.Globals.*;

public class ChatCommandSpectatorHandler implements PacketHandler<ServerboundChatCommandPacket, ServerSession> {
    @Override
    public ServerboundChatCommandPacket apply(final ServerboundChatCommandPacket packet, final ServerSession session) {
        final String command = packet.getCommand();
        if (command.isBlank()) return packet;
        if (CONFIG.inGameCommands.slashCommands
            && CONFIG.inGameCommands.enable
            && CONFIG.server.spectator.fullCommandsEnabled
            && CONFIG.server.spectator.fullCommandsAcceptSlashCommands
            && (CONFIG.server.spectator.fullCommandsRequireRegularWhitelist
                ? PLAYER_LISTS.getWhitelist().contains(session.getProfileCache().getProfile().getId())
                : true)) {
            if (CONFIG.server.spectator.fullCommandsServerCommands) {
                if (IN_GAME_COMMAND.matchesServerCommand(command, CommandContext.createSpectatorContext(command, session))) {
                    if (CONFIG.client.chatSigning.signCommands && CACHE.getChatCache().isSignableCommand(command)) {
                        // if we overwrite a server command with a zenith command, the signing state won't match
                        // for example: `/say hello` is both a zenith and server command
                        // from the client perspective, their command tree (now from zenith) says it's no longer a signed arg
                        session.callPacketReceived(new ServerboundChatCommandSignedPacket(command));
                        return null;
                    }
                    Proxy.getInstance().getClient().sendAsync(packet);
                    return null;
                }
            }
            EXECUTOR.execute(() -> IN_GAME_COMMAND.handleInGameCommandSpectator(
                command,
                session,
                CONFIG.inGameCommands.slashCommandsReplacesServerCommands
            ));
        }
        return null;
    }
}
