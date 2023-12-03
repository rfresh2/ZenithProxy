package com.zenith.network.client.handler.incoming;

import com.github.steveice10.mc.protocol.data.ProtocolState;
import com.github.steveice10.mc.protocol.packet.common.clientbound.ClientboundResourcePackPushPacket;
import com.zenith.cache.data.config.ResourcePack;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.PacketHandler;

import static com.zenith.Shared.CACHE;

public class ResourcePackPushHandler implements PacketHandler<ClientboundResourcePackPushPacket, ClientSession> {
    @Override
    public ClientboundResourcePackPushPacket apply(final ClientboundResourcePackPushPacket packet, final ClientSession session) {
        CACHE.getConfigurationCache().getResourcePacks().put(packet.getId(), new ResourcePack(packet.getId(), packet.getUrl(), packet.getHash(), packet.isRequired(), packet.getPrompt()));
        if (session.getPacketProtocol().getState() == ProtocolState.GAME) return packet;
        else return null;
    }
}
