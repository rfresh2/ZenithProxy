package com.zenith.network.server.handler.shared.incoming;

import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.zenith.network.registry.PacketHandler;
import com.zenith.network.server.ServerConnection;
import org.geysermc.mcprotocollib.protocol.MinecraftConstants;
import org.geysermc.mcprotocollib.protocol.data.status.ServerStatusInfo;
import org.geysermc.mcprotocollib.protocol.data.status.handler.ServerInfoBuilder;
import org.geysermc.mcprotocollib.protocol.packet.status.clientbound.ClientboundStatusResponsePacket;
import org.geysermc.mcprotocollib.protocol.packet.status.serverbound.ServerboundStatusRequestPacket;

import static com.zenith.Shared.CONFIG;
import static com.zenith.Shared.SERVER_LOG;

public class StatusRequestHandler implements PacketHandler<ServerboundStatusRequestPacket, ServerConnection> {
    @Override
    public ServerboundStatusRequestPacket apply(final ServerboundStatusRequestPacket packet, final ServerConnection session) {
        if (CONFIG.server.ping.logPings)
            SERVER_LOG.info("[Ping] Request from: {} [{}]", session.getRemoteAddress(), ProtocolVersion.getProtocol(session.getProtocolVersion()).getName());
        ServerInfoBuilder builder = session.getFlag(MinecraftConstants.SERVER_INFO_BUILDER_KEY);
        if (builder == null) {
            session.disconnect("bye");
            return null;
        }
        ServerStatusInfo info = builder.buildInfo(session);
        if (info == null) session.disconnect("bye");
        else session.send(new ClientboundStatusResponsePacket(info));
        return null;
    }
}
