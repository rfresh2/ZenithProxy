package net.daporkchop.toobeetooteebot.client.handler.incoming;

import com.github.steveice10.mc.protocol.packet.ingame.server.ServerChatPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerDisconnectPacket;
import net.daporkchop.toobeetooteebot.client.PorkClientSession;
import net.daporkchop.toobeetooteebot.util.handler.HandlerRegistry;

/**
 * @author DaPorkchop_
 */
public class DisconnectHandler implements HandlerRegistry.IncomingHandler<ServerDisconnectPacket, PorkClientSession> {
    @Override
    public boolean apply(ServerDisconnectPacket packet, PorkClientSession session) {
        //TODO: check if this packet actually makes it this far down the pipeline
        session.getBot().getServerConnections().forEach(con -> {
            con.send(new ServerChatPacket("\u00A7l\u00A7cDisconnected from server. Reason:"));
            con.send(new ServerChatPacket(packet.getReason()));
        });
        //don't forward on to clients
        return false;
    }

    @Override
    public Class<ServerDisconnectPacket> getPacketClass() {
        return ServerDisconnectPacket.class;
    }
}
