package com.zenith.network.server.handler.player.incoming;

import com.zenith.network.registry.PacketHandler;
import com.zenith.network.server.ServerSession;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundChatCommandSignedPacket;

import static com.zenith.Shared.CONFIG;
import static com.zenith.Shared.IN_GAME_COMMAND;
import static com.zenith.network.server.handler.player.incoming.ChatCommandHandler.replaceExtraChatServerCommands;

public class SignedChatCommandHandler implements PacketHandler<ServerboundChatCommandSignedPacket, ServerSession> {
    @Override
    public ServerboundChatCommandSignedPacket apply(final ServerboundChatCommandSignedPacket packet, final ServerSession session) {
        final String command = packet.getCommand();
        if (command.isBlank()) return packet;
        if (CONFIG.inGameCommands.slashCommands
            && CONFIG.inGameCommands.enable
            && (IN_GAME_COMMAND.handleInGameCommand(
                command,
                session,
                CONFIG.inGameCommands.slashCommandsReplacesServerCommands)
            || CONFIG.inGameCommands.slashCommandsReplacesServerCommands))
            return null;
        return replaceExtraChatServerCommands(command, session) ? packet : null;
    }
}
