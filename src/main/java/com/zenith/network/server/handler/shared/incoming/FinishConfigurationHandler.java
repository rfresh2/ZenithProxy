package com.zenith.network.server.handler.shared.incoming;

import com.zenith.network.KeepAliveTask;
import com.zenith.network.registry.PacketHandler;
import com.zenith.network.server.ServerConnection;
import org.geysermc.mcprotocollib.protocol.MinecraftConstants;
import org.geysermc.mcprotocollib.protocol.ServerLoginHandler;
import org.geysermc.mcprotocollib.protocol.data.ProtocolState;
import org.geysermc.mcprotocollib.protocol.packet.configuration.serverbound.ServerboundFinishConfigurationPacket;

import static com.zenith.Shared.EXECUTOR;

public class FinishConfigurationHandler implements PacketHandler<ServerboundFinishConfigurationPacket, ServerConnection> {
    @Override
    public ServerboundFinishConfigurationPacket apply(final ServerboundFinishConfigurationPacket packet, final ServerConnection session) {
        session.getPacketProtocol().setState(ProtocolState.GAME);
        if (!session.isConfigured()) {
            ServerLoginHandler handler = session.getFlag(MinecraftConstants.SERVER_LOGIN_HANDLER_KEY);
            if (handler != null) {
                handler.loggedIn(session);
            }
            if (session.getFlag(MinecraftConstants.AUTOMATIC_KEEP_ALIVE_MANAGEMENT, true)) {
                EXECUTOR.execute(new KeepAliveTask(session));
            }
            return null;
        }
        return packet;
    }
}
