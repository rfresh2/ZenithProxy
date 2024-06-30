package com.zenith.feature.esp;

import com.zenith.network.registry.PacketHandler;
import com.zenith.network.server.ServerSession;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.EntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.MetadataType;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.ByteEntityMetadata;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundSetEntityDataPacket;

import java.util.ArrayList;

public class GlowingEntityMetadataPacketHandler implements PacketHandler<ClientboundSetEntityDataPacket, ServerSession> {
    @Override
    public ClientboundSetEntityDataPacket apply(final ClientboundSetEntityDataPacket packet, final ServerSession session) {
        ClientboundSetEntityDataPacket p = packet;
        var metadata = packet.getMetadata();
        boolean edited = false;
        for (EntityMetadata<?, ?> entityMetadata : metadata) {
            if (entityMetadata.getId() == 0 && entityMetadata.getType() == MetadataType.BYTE) {
                ByteEntityMetadata byteMetadata = (ByteEntityMetadata) entityMetadata;
                byte b = byteMetadata.getPrimitiveValue();
                // add glowing effect
                byteMetadata.setValue((byte) (b | 0x40));
                edited = true;
                break;
            }
        }
        if (!edited) {
            byte b = 0x40;
            metadata = new ArrayList<>(metadata.size() + 1);
            metadata.addAll(packet.getMetadata());
            metadata.add(new ByteEntityMetadata(0, MetadataType.BYTE, b));
            p = new ClientboundSetEntityDataPacket(packet.getEntityId(), metadata);
        }
        return p;
    }
}
