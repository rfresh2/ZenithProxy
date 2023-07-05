package com.zenith.client.handler.incoming.spawn;

import com.github.steveice10.mc.protocol.packet.ingame.server.entity.spawn.ServerSpawnPaintingPacket;
import com.zenith.cache.data.entity.EntityPainting;
import com.zenith.client.ClientSession;
import com.zenith.feature.handler.HandlerRegistry;
import lombok.NonNull;

import static com.zenith.Shared.CACHE;

public class SpawnPaintingPacket implements HandlerRegistry.AsyncIncomingHandler<ServerSpawnPaintingPacket, ClientSession> {
    @Override
    public boolean applyAsync(@NonNull ServerSpawnPaintingPacket packet, @NonNull ClientSession session) {
        CACHE.getEntityCache().add(new EntityPainting()
                .setDirection(packet.getDirection())
                .setPaintingType(packet.getPaintingType())
                .setEntityId(packet.getEntityId())
                .setUuid(packet.getUUID())
                .setX(packet.getPosition().getX())
                .setY(packet.getPosition().getY())
                .setZ(packet.getPosition().getZ())
        );
        return true;
    }

    @Override
    public Class<ServerSpawnPaintingPacket> getPacketClass() {
        return ServerSpawnPaintingPacket.class;
    }
}
