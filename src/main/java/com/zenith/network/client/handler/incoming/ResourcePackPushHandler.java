package com.zenith.network.client.handler.incoming;

import com.zenith.cache.data.config.ResourcePack;
import com.zenith.network.client.ClientSession;
import com.zenith.network.codec.PacketHandler;
import org.geysermc.mcprotocollib.protocol.data.game.ResourcePackStatus;
import org.geysermc.mcprotocollib.protocol.packet.common.clientbound.ClientboundResourcePackPushPacket;
import org.geysermc.mcprotocollib.protocol.packet.common.serverbound.ServerboundResourcePackPacket;
import org.jspecify.annotations.NonNull;

import static com.zenith.Globals.CACHE;
import static com.zenith.Globals.CLIENT_LOG; 

public class ResourcePackPushHandler implements PacketHandler<ClientboundResourcePackPushPacket, ClientSession> {
    public static final ResourcePackPushHandler INSTANCE = new ResourcePackPushHandler();

    @Override
    public ClientboundResourcePackPushPacket apply(@NonNull final ClientboundResourcePackPushPacket packet, @NonNull final ClientSession session) {
        CACHE.getConfigurationCache().getResourcePacks().put(packet.getId(), new ResourcePack(packet.getId(), packet.getUrl(), packet.getHash(), packet.isRequired(), packet.getPrompt()));
        session.sendAsync(new ServerboundResourcePackPacket(packet.getId(), ResourcePackStatus.ACCEPTED));
        CLIENT_LOG.debug("Spoofed resource pack status to ACCEPTED for: {}", packet.getId());
        session.sendAsync(new ServerboundResourcePackPacket(packet.getId(), ResourcePackStatus.SUCCESSFULLY_LOADED));
        CLIENT_LOG.debug("Spoofed resource pack status to SUCCESSFULLY_LOADED for: {}", packet.getId());
        return null; 
    }
}
