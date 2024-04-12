package com.zenith.network.client.handler.incoming.entity;

import com.zenith.cache.data.entity.Entity;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.ClientEventLoopPacketHandler;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundSetEntityLinkPacket;

import static com.zenith.Shared.CACHE;
import static com.zenith.Shared.CLIENT_LOG;

public class SetEntityLinkHandler implements ClientEventLoopPacketHandler<ClientboundSetEntityLinkPacket, ClientSession> {

    @Override
    public boolean applyAsync(ClientboundSetEntityLinkPacket packet, ClientSession session) {
        Entity entity = CACHE.getEntityCache().get(packet.getEntityId());
        if (entity != null) {
            if (packet.getAttachedToId() == -1) {
                entity.setLeashed(false).setLeashedId(-1);
            } else {
                entity.setLeashed(true).setLeashedId(packet.getAttachedToId());
            }
            return true;
        } else {
            CLIENT_LOG.debug("Received ServerEntityAttachPacket for invalid entity (id={})", packet.getEntityId());
            return false;
        }
    }
}
