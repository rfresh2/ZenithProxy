package com.zenith.network.server.handler.player.incoming;

import com.github.steveice10.mc.protocol.packet.ingame.serverbound.ServerboundCommandSuggestionPacket;
import com.zenith.network.registry.PacketHandler;
import com.zenith.network.server.ServerConnection;

import static com.zenith.Shared.CONFIG;

public class CommandSuggestionHandler implements PacketHandler<ServerboundCommandSuggestionPacket, ServerConnection> {
    @Override
    public ServerboundCommandSuggestionPacket apply(final ServerboundCommandSuggestionPacket packet, final ServerConnection session) {
        if (CONFIG.inGameCommands.slashCommands) return null;
        return packet;
    }
}
