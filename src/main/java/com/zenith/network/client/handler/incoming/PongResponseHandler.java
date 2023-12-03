package com.zenith.network.client.handler.incoming;

import com.github.steveice10.mc.protocol.MinecraftConstants;
import com.github.steveice10.mc.protocol.data.status.handler.ServerPingTimeHandler;
import com.github.steveice10.mc.protocol.packet.status.clientbound.ClientboundPongResponsePacket;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.PacketHandler;

public class PongResponseHandler implements PacketHandler<ClientboundPongResponsePacket, ClientSession> {
    @Override
    public ClientboundPongResponsePacket apply(final ClientboundPongResponsePacket packet, final ClientSession session) {
        long time = System.currentTimeMillis() - packet.getPingTime();
        ServerPingTimeHandler handler = session.getFlag(MinecraftConstants.SERVER_PING_TIME_HANDLER_KEY);
        if (handler != null) {
            handler.handle(session, time);
        }
        session.disconnect("Finished");
        return null;
    }
}
