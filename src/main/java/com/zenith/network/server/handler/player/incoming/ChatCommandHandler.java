package com.zenith.network.server.handler.player.incoming;

import com.zenith.network.codec.PacketHandler;
import com.zenith.network.server.ServerSession;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundChatCommandPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundChatCommandSignedPacket;

import static com.zenith.Globals.*;

public class ChatCommandHandler implements PacketHandler<ServerboundChatCommandPacket, ServerSession> {
    @Override
    public ServerboundChatCommandPacket apply(final ServerboundChatCommandPacket packet, final ServerSession session) {
        final String command = packet.getCommand();
        if (command.isBlank()) return packet;
        if (CONFIG.inGameCommands.slashCommands && CONFIG.inGameCommands.enable) {
            var zenithHandled = IN_GAME_COMMAND.handleInGameCommand(command, session, CONFIG.inGameCommands.slashCommandsReplacesServerCommands);
            if (zenithHandled || CONFIG.inGameCommands.slashCommandsReplacesServerCommands) {
                return null;
            }
            if (CONFIG.client.chatSigning.signCommands && CACHE.getChatCache().isSignableCommand(command)) {
                // if we overwrite a server command with a zenith command, the signing state won't match
                // for example: `/say hello` is both a zenith and server command
                // from the client perspective, their command tree (now from zenith) says it's no longer a signed arg
                session.callPacketReceived(new ServerboundChatCommandSignedPacket(command));
                return null;
            }
        }
        return packet;
    }
}
