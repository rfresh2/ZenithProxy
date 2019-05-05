package net.daporkchop.toobeetooteebot.client.handler.incoming;

import com.github.steveice10.mc.protocol.packet.ingame.server.ServerKeepAlivePacket;
import lombok.NonNull;
import net.daporkchop.toobeetooteebot.client.PorkClientSession;
import net.daporkchop.toobeetooteebot.util.handler.HandlerRegistry;

import static net.daporkchop.lib.math.primitive.PMath.floorI;

/**
 * @author DaPorkchop_
 */
public class ClientKeepaliveHandler implements HandlerRegistry.IncomingHandler<ServerKeepAlivePacket, PorkClientSession> {
    @Override
    public boolean apply(@NonNull ServerKeepAlivePacket packet, @NonNull PorkClientSession session) {
        CACHE.getChunkCache().tick(
                floorI(CACHE.getPlayerCache().getX()) >> 4,
                floorI(CACHE.getPlayerCache().getZ()) >> 4
        );
        return false;
    }

    @Override
    public Class<ServerKeepAlivePacket> getPacketClass() {
        return ServerKeepAlivePacket.class;
    }
}
