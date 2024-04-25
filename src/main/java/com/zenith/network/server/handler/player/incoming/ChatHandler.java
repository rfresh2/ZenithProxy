package com.zenith.network.server.handler.player.incoming;

import com.zenith.network.registry.PacketHandler;
import com.zenith.network.server.ServerConnection;
import lombok.NonNull;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundChatPacket;

import static com.zenith.Shared.*;

public class ChatHandler implements PacketHandler<ServerboundChatPacket, ServerConnection> {
    @Override
    public ServerboundChatPacket apply(@NonNull ServerboundChatPacket packet, @NonNull ServerConnection session) {
        if (CONFIG.inGameCommands.enable) {
            final String message = packet.getMessage();
            if (IN_GAME_COMMAND.getCommandPattern().matcher(message).find()) {
                EXECUTOR.execute(() -> IN_GAME_COMMAND.handleInGameCommand(message.substring(CONFIG.inGameCommands.prefix.length()), session, true));
                return null;
            }
        }
        return packet;
    }
}
