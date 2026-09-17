package com.zenith.network.server.handler.shared.incoming;

import com.zenith.network.codec.PacketHandler;
import com.zenith.network.server.ServerSession;
import org.geysermc.mcprotocollib.protocol.packet.common.serverbound.ServerboundResourcePackPacket;

public class SResourcePackHandler implements PacketHandler<ServerboundResourcePackPacket, ServerSession> {
    @Override
    public ServerboundResourcePackPacket apply(ServerboundResourcePackPacket packet, ServerSession session) {
        if (session.isConfigured()) return packet;
        session.getResourcePackConfiguration().handleResponse(packet);
        return null;
    }
}
