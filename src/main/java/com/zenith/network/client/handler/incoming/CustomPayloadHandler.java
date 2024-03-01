package com.zenith.network.client.handler.incoming;

import com.github.steveice10.mc.protocol.codec.MinecraftCodecHelper;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundCustomPayloadPacket;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.PacketHandler;
import com.zenith.util.BrandSerializer;

import static com.zenith.Shared.CACHE;

public class CustomPayloadHandler implements PacketHandler<ClientboundCustomPayloadPacket, ClientSession> {
    @Override
    public ClientboundCustomPayloadPacket apply(ClientboundCustomPayloadPacket packet, ClientSession session) {
        if (packet.getChannel().equalsIgnoreCase("minecraft:brand")) {
            CACHE.getChunkCache().setServerBrand(packet.getData());
            return new ClientboundCustomPayloadPacket(
                packet.getChannel(),
                BrandSerializer.appendBrand((MinecraftCodecHelper) session.getCodecHelper(), packet.getData()));
        }
        return packet;
    }
}
