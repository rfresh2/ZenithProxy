package com.zenith.network.client.handler.incoming;

import com.github.steveice10.mc.protocol.MinecraftConstants;
import com.github.steveice10.mc.protocol.data.status.ServerStatusInfo;
import com.github.steveice10.mc.protocol.data.status.handler.ServerInfoHandler;
import com.github.steveice10.mc.protocol.packet.status.clientbound.ClientboundStatusResponsePacket;
import com.github.steveice10.mc.protocol.packet.status.serverbound.ServerboundPingRequestPacket;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.PacketHandler;

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
