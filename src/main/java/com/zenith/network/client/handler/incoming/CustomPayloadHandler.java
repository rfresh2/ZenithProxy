package com.zenith.network.client.handler.incoming;

import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.PacketHandler;
import com.zenith.util.BrandSerializer;
import net.kyori.adventure.key.Key;
import org.geysermc.mcprotocollib.protocol.codec.MinecraftCodecHelper;
import org.geysermc.mcprotocollib.protocol.packet.common.clientbound.ClientboundCustomPayloadPacket;

import static com.zenith.Shared.CACHE;

public class CustomPayloadHandler implements PacketHandler<ClientboundCustomPayloadPacket, ClientSession> {
    public static final CustomPayloadHandler INSTANCE = new CustomPayloadHandler();
    @Override
    public ClientboundCustomPayloadPacket apply(ClientboundCustomPayloadPacket packet, ClientSession session) {
        Key channel = packet.getChannel();
        if (channel.namespace().equals("minecraft") && channel.value().equals("brand")) {
            CACHE.getChunkCache().setServerBrand(packet.getData());
            return new ClientboundCustomPayloadPacket(
                packet.getChannel(),
                BrandSerializer.appendBrand((MinecraftCodecHelper) session.getCodecHelper(), packet.getData()));
        }
        return packet;
    }
}
