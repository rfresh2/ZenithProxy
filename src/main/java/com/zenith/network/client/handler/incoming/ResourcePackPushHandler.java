package com.zenith.network.client.handler.incoming;

import com.zenith.cache.data.config.ResourcePack;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.PacketHandler;
import org.geysermc.mcprotocollib.protocol.data.ProtocolState;
import org.geysermc.mcprotocollib.protocol.packet.common.clientbound.ClientboundResourcePackPushPacket;

import static com.zenith.Shared.CACHE;

public class ResourcePackPushHandler implements PacketHandler<ClientboundResourcePackPushPacket, ClientSession> {
    @Override
    public ClientboundResourcePackPushPacket apply(final ClientboundResourcePackPushPacket packet, final ClientSession session) {
        CACHE.getConfigurationCache().getResourcePacks().put(packet.getId(), new ResourcePack(packet.getId(), packet.getUrl(), packet.getHash(), packet.isRequired(), packet.getPrompt()));
        if (session.getPacketProtocol().getState() == ProtocolState.GAME) return packet;
        else return null;
    }
}
