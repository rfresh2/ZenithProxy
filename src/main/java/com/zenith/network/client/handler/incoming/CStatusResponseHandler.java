package com.zenith.network.client.handler.incoming;

import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.PacketHandler;
import org.geysermc.mcprotocollib.protocol.MinecraftConstants;
import org.geysermc.mcprotocollib.protocol.data.status.ServerStatusInfo;
import org.geysermc.mcprotocollib.protocol.data.status.handler.ServerInfoHandler;
import org.geysermc.mcprotocollib.protocol.packet.status.clientbound.ClientboundStatusResponsePacket;
import org.geysermc.mcprotocollib.protocol.packet.status.serverbound.ServerboundPingRequestPacket;

public class CStatusResponseHandler implements PacketHandler<ClientboundStatusResponsePacket, ClientSession> {
    @Override
    public ClientboundStatusResponsePacket apply(final ClientboundStatusResponsePacket packet, final ClientSession session) {
        ServerStatusInfo info = packet.getInfo();
        ServerInfoHandler handler = session.getFlag(MinecraftConstants.SERVER_INFO_HANDLER_KEY);
        if (handler != null) {
            handler.handle(session, info);
        }
        session.send(new ServerboundPingRequestPacket(System.currentTimeMillis()));
        return null;
    }
}
