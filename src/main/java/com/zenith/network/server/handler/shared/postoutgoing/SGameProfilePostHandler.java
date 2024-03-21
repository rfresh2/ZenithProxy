package com.zenith.network.server.handler.shared.postoutgoing;

import com.github.steveice10.mc.protocol.MinecraftConstants;
import com.github.steveice10.mc.protocol.ServerLoginHandler;
import com.github.steveice10.mc.protocol.data.ProtocolState;
import com.github.steveice10.mc.protocol.packet.login.clientbound.ClientboundGameProfilePacket;
import com.zenith.network.KeepAliveTask;
import com.zenith.network.registry.PostOutgoingPacketHandler;
import com.zenith.network.server.ServerConnection;

import static com.zenith.Shared.EXECUTOR;

public class SGameProfilePostHandler implements PostOutgoingPacketHandler<ClientboundGameProfilePacket, ServerConnection> {
    @Override
    public void accept(final ClientboundGameProfilePacket packet, final ServerConnection session) {
        session.getPacketProtocol().setState(ProtocolState.GAME);
        ServerLoginHandler handler = session.getFlag(MinecraftConstants.SERVER_LOGIN_HANDLER_KEY);
        if (handler != null) {
            handler.loggedIn(session);
        }
        if (session.getFlag(MinecraftConstants.AUTOMATIC_KEEP_ALIVE_MANAGEMENT, true)) {
            EXECUTOR.execute(new KeepAliveTask(session));
        }
    }
}
