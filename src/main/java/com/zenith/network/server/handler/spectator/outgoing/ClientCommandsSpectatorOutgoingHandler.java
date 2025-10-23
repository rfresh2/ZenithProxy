package com.zenith.network.server.handler.spectator.outgoing;

import com.zenith.network.codec.PacketHandler;
import com.zenith.network.server.ServerSession;
import org.geysermc.mcprotocollib.protocol.data.game.command.CommandNode;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundCommandsPacket;

import static com.zenith.Globals.*;

public class ClientCommandsSpectatorOutgoingHandler implements PacketHandler<ClientboundCommandsPacket, ServerSession> {
    @Override
    public ClientboundCommandsPacket apply(final ClientboundCommandsPacket packet, final ServerSession session) {
        if (CONFIG.inGameCommands.enable && CONFIG.inGameCommands.slashCommands && CONFIG.server.spectator.fullCommandsEnabled && CONFIG.server.spectator.fullCommandsAcceptSlashCommands) {
            if (CONFIG.server.spectator.fullCommandsRequireRegularWhitelist && !PLAYER_LISTS.getWhitelist().contains(session.getProfileCache().getProfile().getId())) {
                return null;
            }
            CommandNode[] zenithCommandNodes = COMMAND.getMcplCommandNodes();
            return new ClientboundCommandsPacket(
                zenithCommandNodes,
                0
            );
        }
        return null;
    }
}
