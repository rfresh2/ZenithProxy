package com.zenith.server.handler.spectator.incoming;

import com.github.steveice10.mc.protocol.packet.ingame.client.ClientChatPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerChatPacket;
import com.zenith.server.PorkServerConnection;
import com.zenith.util.Queue;
import com.zenith.util.handler.HandlerRegistry;
import lombok.NonNull;
import net.daporkchop.lib.unsafe.PUnsafe;

import static com.zenith.util.Constants.MANUAL_DISCONNECT;

public class ServerSpectatorChatHandler implements HandlerRegistry.IncomingHandler<ClientChatPacket, PorkServerConnection> {
    protected static final long CLIENTCHATPACKET_MESSAGE_OFFSET = PUnsafe.pork_getOffset(ClientChatPacket.class, "message");

    @Override
    public boolean apply(@NonNull ClientChatPacket packet, @NonNull PorkServerConnection session) {
        if (packet.getMessage().startsWith("!"))   {
            if (packet.getMessage().startsWith("!!"))   {
                //allow sending ingame commands to bots or whatever
                PUnsafe.putObject(packet, CLIENTCHATPACKET_MESSAGE_OFFSET, packet.getMessage().substring(1));
                return true;
            } else if ("!dc".equalsIgnoreCase(packet.getMessage())) {
                session.getProxy().getClient().disconnect(MANUAL_DISCONNECT);
                return false;
            } else if ("!q".equalsIgnoreCase(packet.getMessage())) {
                session.send(new ServerChatPacket(String.format("§7[§9Proxy§7]§r §7Queue: §c" + Queue.getQueueStatus().regular + " §r- §7Prio: §a" + Queue.getQueueStatus().prio, packet.getMessage()), true));
                return false;
            } else {
                session.send(new ServerChatPacket(String.format("§7[§9Proxy§7]§r §cUnknown command: §o%s", packet.getMessage()), true));
                return false;
            }
        }
        return true;
    }

    @Override
    public Class<ClientChatPacket> getPacketClass() {
        return ClientChatPacket.class;
    }
}
