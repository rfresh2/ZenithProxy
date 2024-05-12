package com.zenith.network.client.handler.incoming;

import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.PacketHandler;
import org.geysermc.mcprotocollib.protocol.MinecraftConstants;
import org.geysermc.mcprotocollib.protocol.packet.common.clientbound.ClientboundKeepAlivePacket;
import org.geysermc.mcprotocollib.protocol.packet.common.serverbound.ServerboundKeepAlivePacket;

public class CKeepAliveHandler implements PacketHandler<ClientboundKeepAlivePacket, ClientSession> {
    public static CKeepAliveHandler INSTANCE = new CKeepAliveHandler();
    @Override
    public ClientboundKeepAlivePacket apply(final ClientboundKeepAlivePacket packet, final ClientSession session) {
        if (session.getFlag(MinecraftConstants.AUTOMATIC_KEEP_ALIVE_MANAGEMENT, true)) {
            session.send(new ServerboundKeepAlivePacket(packet.getPingId()));
            return null;
        }
        return packet;
    }
}
