package com.zenith.network.server.handler.player.incoming;

import com.zenith.network.registry.PacketHandler;
import com.zenith.network.server.ServerConnection;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundCommandSuggestionPacket;

import static com.zenith.Shared.CONFIG;

public class CommandSuggestionHandler implements PacketHandler<ServerboundCommandSuggestionPacket, ServerConnection> {
    @Override
    public ServerboundCommandSuggestionPacket apply(final ServerboundCommandSuggestionPacket packet, final ServerConnection session) {
        if (CONFIG.inGameCommands.enable
            && CONFIG.inGameCommands.slashCommands
            && CONFIG.inGameCommands.slashCommandsReplacesServerCommands) return null;
        return packet;
    }
}
