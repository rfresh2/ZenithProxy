package net.daporkchop.toobeetooteebot.client.handler.incoming.spawn;

import com.github.steveice10.mc.protocol.packet.ingame.server.entity.spawn.ServerSpawnExpOrbPacket;
import lombok.NonNull;
import net.daporkchop.toobeetooteebot.client.PorkClientSession;
import net.daporkchop.toobeetooteebot.util.cache.data.entity.EntityExperienceOrb;
import net.daporkchop.toobeetooteebot.util.handler.HandlerRegistry;

/**
 * @author DaPorkchop_
 */
public class SpawnExperienceOrbHandler implements HandlerRegistry.IncomingHandler<ServerSpawnExpOrbPacket, PorkClientSession> {
    @Override
    public boolean apply(@NonNull ServerSpawnExpOrbPacket packet, @NonNull PorkClientSession session) {
        CACHE.getEntityCache().add(new EntityExperienceOrb()
                .setExp(packet.getExp())
                .setEntityId(packet.getEntityId())
                .setX(packet.getX())
                .setY(packet.getY())
                .setZ(packet.getZ())
        );
        return true;
    }

    @Override
    public Class<ServerSpawnExpOrbPacket> getPacketClass() {
        return ServerSpawnExpOrbPacket.class;
    }
}
