package com.zenith.network.server.handler.spectator.incoming;

import com.zenith.Proxy;
import com.zenith.command.api.CommandContext;
import com.zenith.network.codec.PacketHandler;
import com.zenith.network.server.ServerSession;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundChatCommandSignedPacket;

import static com.zenith.Globals.*;

public class SignedChatCommandSpectatorHandler implements PacketHandler<ServerboundChatCommandSignedPacket, ServerSession> {
    @Override
    public ServerboundChatCommandSignedPacket apply(final ServerboundChatCommandSignedPacket packet, final ServerSession session) {
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
