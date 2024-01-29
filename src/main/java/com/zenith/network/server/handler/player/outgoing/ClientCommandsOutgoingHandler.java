package com.zenith.network.server.handler.player.outgoing;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundCommandsPacket;
import com.zenith.network.registry.PacketHandler;
import com.zenith.network.server.ServerConnection;

import static com.zenith.Shared.COMMAND_MANAGER;
import static com.zenith.Shared.CONFIG;

public class ClientCommandsOutgoingHandler implements PacketHandler<ClientboundCommandsPacket, ServerConnection> {
    @Override
    public ClientboundCommandsPacket apply(final ClientboundCommandsPacket packet, final ServerConnection session) {
        if (CONFIG.inGameCommands.slashCommands) {
            // replaces what's being sent from the command cache or from the server
            return new ClientboundCommandsPacket(
                COMMAND_MANAGER.getMCProtocolLibCommandNodesSupplier().get(),
                0
            );
        }
        return packet;
    }
}
