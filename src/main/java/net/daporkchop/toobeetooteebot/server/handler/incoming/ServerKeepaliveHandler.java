package net.daporkchop.toobeetooteebot.server.handler.incoming;

import com.github.steveice10.mc.protocol.packet.ingame.client.ClientKeepAlivePacket;
import lombok.NonNull;
import net.daporkchop.toobeetooteebot.server.PorkServerConnection;
import net.daporkchop.toobeetooteebot.util.handler.HandlerRegistry;

/**
 * @author DaPorkchop_
 */
public class ServerKeepaliveHandler implements HandlerRegistry.IncomingHandler<ClientKeepAlivePacket, PorkServerConnection> {
    @Override
    public boolean apply(@NonNull ClientKeepAlivePacket packet, @NonNull PorkServerConnection session) {
        return false;
    }

    @Override
    public Class<ClientKeepAlivePacket> getPacketClass() {
        return ClientKeepAlivePacket.class;
    }
}
