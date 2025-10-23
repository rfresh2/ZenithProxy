package com.zenith.network.server.handler.player.incoming;

import com.zenith.network.codec.PacketHandler;
import com.zenith.network.server.ServerSession;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundChatCommandSignedPacket;

import static com.zenith.Globals.CONFIG;
import static com.zenith.Globals.IN_GAME_COMMAND;

public class SignedChatCommandHandler implements PacketHandler<ServerboundChatCommandSignedPacket, ServerSession> {
    @Override
    public ServerboundChatCommandSignedPacket apply(final ServerboundChatCommandSignedPacket packet, final ServerSession session) {
        final String command = packet.getCommand();
        if (command.isBlank()) return packet;
        if (CONFIG.inGameCommands.slashCommands && CONFIG.inGameCommands.enable) {
            var zenithHandled = IN_GAME_COMMAND.handleInGameCommand(command, session, CONFIG.inGameCommands.slashCommandsReplacesServerCommands);
            if (zenithHandled || CONFIG.inGameCommands.slashCommandsReplacesServerCommands) {
                return null;
            }
        }
        return packet;
    }
}
