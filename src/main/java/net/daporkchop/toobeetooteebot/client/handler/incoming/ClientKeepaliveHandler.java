package net.daporkchop.toobeetooteebot.client.handler.incoming;

import com.github.steveice10.mc.protocol.packet.ingame.server.ServerKeepAlivePacket;
import lombok.NonNull;
import net.daporkchop.toobeetooteebot.client.PorkClientSession;
import net.daporkchop.toobeetooteebot.util.handler.HandlerRegistry;

/**
 * @author DaPorkchop_
 */
public class ClientKeepaliveHandler implements HandlerRegistry.IncomingHandler<ServerKeepAlivePacket, PorkClientSession> {
    @Override
    public boolean apply(@NonNull ServerKeepAlivePacket packet, @NonNull PorkClientSession session) {
        return false;
    }

    @Override
    public Class<ServerKeepAlivePacket> getPacketClass() {
        return ServerKeepAlivePacket.class;
    }
}
