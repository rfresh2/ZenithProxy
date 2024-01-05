package com.zenith.feature.forwarding.handlers.outbound;

import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.mc.protocol.MinecraftConstants;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.spawn.ClientboundAddPlayerPacket;
import com.zenith.module.impl.ProxyForwarding;
import com.zenith.network.registry.PacketHandler;
import com.zenith.network.server.ServerConnection;

public class ForwardingAddPlayerHandler implements PacketHandler<ClientboundAddPlayerPacket, ServerConnection> {
    @Override
    public ClientboundAddPlayerPacket apply(ClientboundAddPlayerPacket packet, ServerConnection session) {
        final GameProfile clientProfile = session.getFlag(MinecraftConstants.PROFILE_KEY);

        if (packet.getUuid().equals(clientProfile.getId())) {
            return packet.withUuid(ProxyForwarding.getFakeUuid(packet.getUuid()));
        }

        return packet;
    }
}
