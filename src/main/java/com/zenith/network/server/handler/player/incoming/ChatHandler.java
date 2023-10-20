package com.zenith.network.server.handler.player.incoming;

import com.github.steveice10.mc.protocol.packet.ingame.serverbound.ServerboundChatPacket;
import com.zenith.network.registry.IncomingHandler;
import com.zenith.network.server.ServerConnection;
import lombok.NonNull;

import static com.zenith.Shared.*;

public class ChatHandler implements IncomingHandler<ServerboundChatPacket, ServerConnection> {
    @Override
    public boolean apply(@NonNull ServerboundChatPacket packet, @NonNull ServerConnection session) {
        if (CONFIG.inGameCommands.enable) {
            final String message = packet.getMessage();
            if (IN_GAME_COMMAND_MANAGER.getCommandPattern().matcher(message).find()) {
                SCHEDULED_EXECUTOR_SERVICE.execute(() -> IN_GAME_COMMAND_MANAGER.handleInGameCommand(message.substring(CONFIG.inGameCommands.prefix.length()), session));
                return false;
            }
        }
        return true;
    }
}
