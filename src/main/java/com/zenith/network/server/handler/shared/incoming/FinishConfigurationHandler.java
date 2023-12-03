package com.zenith.network.server.handler.shared.incoming;

import com.github.steveice10.mc.protocol.MinecraftConstants;
import com.github.steveice10.mc.protocol.ServerLoginHandler;
import com.github.steveice10.mc.protocol.data.ProtocolState;
import com.github.steveice10.mc.protocol.packet.configuration.serverbound.ServerboundFinishConfigurationPacket;
import com.zenith.network.KeepAliveTask;
import com.zenith.network.registry.PacketHandler;
import com.zenith.network.server.ServerConnection;

import static com.zenith.Shared.SCHEDULED_EXECUTOR_SERVICE;

public class FinishConfigurationHandler implements PacketHandler<ServerboundFinishConfigurationPacket, ServerConnection> {
    @Override
    public ServerboundFinishConfigurationPacket apply(final ServerboundFinishConfigurationPacket packet, final ServerConnection session) {
        session.getPacketProtocol().setState(ProtocolState.GAME);
        ServerLoginHandler handler = session.getFlag(MinecraftConstants.SERVER_LOGIN_HANDLER_KEY);
        if (handler != null) {
            handler.loggedIn(session);
        }
        if (session.getFlag(MinecraftConstants.AUTOMATIC_KEEP_ALIVE_MANAGEMENT, true)) {
            SCHEDULED_EXECUTOR_SERVICE.execute(new KeepAliveTask(session));
        }
        return null;
    }
}
