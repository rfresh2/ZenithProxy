package com.zenith.network.server.handler.shared.incoming;

import com.github.steveice10.mc.protocol.MinecraftProtocol;
import com.github.steveice10.mc.protocol.data.ProtocolState;
import com.github.steveice10.mc.protocol.packet.handshake.serverbound.ClientIntentionPacket;
import com.zenith.network.registry.PacketHandler;
import com.zenith.network.server.ServerConnection;

public class IntentionHandler implements PacketHandler<ClientIntentionPacket, ServerConnection> {
    @Override
    public ClientIntentionPacket apply(final ClientIntentionPacket packet, final ServerConnection session) {
        MinecraftProtocol protocol = session.getPacketProtocol();
        switch (packet.getIntent()) {
            case STATUS -> protocol.setState(ProtocolState.STATUS);
            case LOGIN -> {
                protocol.setState(ProtocolState.LOGIN);
                if (packet.getProtocolVersion() > protocol.getCodec().getProtocolVersion()) {
                    session.disconnect("Outdated server! I'm still on " + protocol.getCodec()
                        .getMinecraftVersion() + ".");
                } else if (packet.getProtocolVersion() < protocol.getCodec().getProtocolVersion()) {
                    session.disconnect("Outdated client! Please use " + protocol.getCodec()
                        .getMinecraftVersion() + ".");
                }
            }
            default -> session.disconnect("Invalid client intention: " + packet.getIntent());
        }
        return null;
    }
}
