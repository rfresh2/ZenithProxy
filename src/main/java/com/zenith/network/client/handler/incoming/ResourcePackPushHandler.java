package com.zenith.network.client.handler.incoming;

import com.zenith.Proxy;
import com.zenith.cache.data.config.ResourcePack;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.PacketHandler;
import org.geysermc.mcprotocollib.protocol.data.game.ResourcePackStatus;
import org.geysermc.mcprotocollib.protocol.packet.common.clientbound.ClientboundResourcePackPushPacket;
import org.geysermc.mcprotocollib.protocol.packet.common.serverbound.ServerboundResourcePackPacket;

import static com.zenith.Shared.CACHE;

public class ResourcePackPushHandler implements PacketHandler<ClientboundResourcePackPushPacket, ClientSession> {
    public static final ResourcePackPushHandler INSTANCE = new ResourcePackPushHandler();
    @Override
    public ClientboundResourcePackPushPacket apply(final ClientboundResourcePackPushPacket packet, final ClientSession session) {
        CACHE.getConfigurationCache().getResourcePacks().put(packet.getId(), new ResourcePack(packet.getId(), packet.getUrl(), packet.getHash(), packet.isRequired(), packet.getPrompt()));
        if (Proxy.getInstance().hasActivePlayer()) return packet;
        else {
            session.sendAsync(new ServerboundResourcePackPacket(packet.getId(), ResourcePackStatus.SUCCESSFULLY_LOADED));
            return null;
        }
    }
}
