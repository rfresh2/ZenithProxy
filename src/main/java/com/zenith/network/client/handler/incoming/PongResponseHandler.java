package com.zenith.network.client.handler.incoming;

import com.github.steveice10.mc.protocol.packet.status.clientbound.ClientboundPongResponsePacket;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.PacketHandler;
import com.zenith.util.Config;

import static com.zenith.Shared.CONFIG;

public class PongResponseHandler implements PacketHandler<ClientboundPongResponsePacket, ClientSession> {
    @Override
    public ClientboundPongResponsePacket apply(final ClientboundPongResponsePacket packet, final ClientSession session) {
        if (CONFIG.client.ping.mode == Config.Client.Ping.Mode.PACKET && session.getLastPingId() == packet.getPingTime()) {
            // this is from our own ping task
            session.setPing(System.currentTimeMillis() - session.getLastPingSentTime());
            return null;
        }
        return packet;
    }
}
