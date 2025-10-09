package com.zenith.network.client.handler.incoming;

import com.zenith.network.client.ClientSession;
import com.zenith.network.codec.PacketHandler;
import org.geysermc.mcprotocollib.protocol.packet.configuration.clientbound.ClientboundCodeOfConductPacket;
import org.geysermc.mcprotocollib.protocol.packet.configuration.serverbound.ServerboundAcceptCodeOfConductPacket;

public class CodeOfConductHandler implements PacketHandler<ClientboundCodeOfConductPacket, ClientSession> {
    @Override
    public ClientboundCodeOfConductPacket apply(final ClientboundCodeOfConductPacket packet, final ClientSession session) {
        session.send(ServerboundAcceptCodeOfConductPacket.INSTANCE);
        return null;
    }
}
