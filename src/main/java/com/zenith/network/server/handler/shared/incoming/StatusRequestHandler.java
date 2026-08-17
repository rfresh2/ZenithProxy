package com.zenith.network.server.handler.shared.incoming;

import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.zenith.network.codec.PacketHandler;
import com.zenith.network.server.ServerSession;
import com.zenith.network.server.ZenithServerInfoBuilder;
import org.geysermc.mcprotocollib.protocol.data.status.ServerStatusInfo;
import org.geysermc.mcprotocollib.protocol.packet.status.clientbound.ClientboundStatusResponsePacket;
import org.geysermc.mcprotocollib.protocol.packet.status.serverbound.ServerboundStatusRequestPacket;

import static com.zenith.Globals.CONFIG;
import static com.zenith.Globals.SERVER_LOG;

public class StatusRequestHandler implements PacketHandler<ServerboundStatusRequestPacket, ServerSession> {
    @Override
    public ServerboundStatusRequestPacket apply(final ServerboundStatusRequestPacket packet, final ServerSession session) {
        if (session.isStatusRequested()) {
            session.disconnect("Duplicate status request");
            return null;
        }
        session.setStatusRequested(true);
        if (CONFIG.server.ping.logPings)
            SERVER_LOG.info("[Ping] Request from: {} [{}] to: {}:{}",
                            session.getRemoteAddress(),
                            ProtocolVersion.getProtocol(session.getProtocolVersionId()).getName(),
                            session.getConnectingServerAddress(),
                            session.getConnectingServerPort());
        ServerStatusInfo info = ZenithServerInfoBuilder.INSTANCE.buildInfo(session);
        if (info == null) {
            session.disconnect("bye");
        } else {
            session.send(new ClientboundStatusResponsePacket(info));
        }
        return null;
    }
}
