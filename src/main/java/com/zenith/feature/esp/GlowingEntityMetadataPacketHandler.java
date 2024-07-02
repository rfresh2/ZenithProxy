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
        for (int i = 0; i < metadata.size(); i++) {
            final EntityMetadata<?, ?> entityMetadata = metadata.get(i);
            if (entityMetadata.getId() == 0 && entityMetadata.getType() == MetadataType.BYTE) {
                ByteEntityMetadata byteMetadata = (ByteEntityMetadata) entityMetadata;
                var newMetadata = new ByteEntityMetadata(0, MetadataType.BYTE, (byte) (byteMetadata.getPrimitiveValue() | 0x40));
                var newMetadataList = new ArrayList<>(metadata);
                newMetadataList.set(i, newMetadata);
                p = packet.withMetadata(newMetadataList);
                edited = true;
                break;
            }
        }
        if (!edited) {
            var newMetadata = new ArrayList<EntityMetadata<?, ?>>(metadata.size() + 1);
            newMetadata.addAll(packet.getMetadata());
            newMetadata.add(new ByteEntityMetadata(0, MetadataType.BYTE, (byte) 0x40));
            p = packet.withMetadata(newMetadata);
        }
        return p;
    }
}
