/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2016-2020 DaPorkchop_
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software
 * is furnished to do so, subject to the following conditions:
 *
 * Any persons and/or organizations using this software must include the above copyright notice and this permission notice,
 * provide sufficient credit to the original authors of the project (IE: DaPorkchop_), as well as provide a link to the original project.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS
 * BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

package com.zenith.server.handler.player.incoming;

import com.github.steveice10.mc.protocol.packet.ingame.client.ClientChatPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerChatPacket;
import com.zenith.server.ServerConnection;
import com.zenith.util.Queue;
import com.zenith.util.handler.HandlerRegistry;
import lombok.NonNull;
import net.daporkchop.lib.unsafe.PUnsafe;

import static com.zenith.util.Constants.MANUAL_DISCONNECT;

/**
 * @author DaPorkchop_
 */
public class ServerChatHandler implements HandlerRegistry.IncomingHandler<ClientChatPacket, ServerConnection> {
    protected static final long CLIENTCHATPACKET_MESSAGE_OFFSET = PUnsafe.pork_getOffset(ClientChatPacket.class, "message");

    @Override
    public boolean apply(@NonNull ClientChatPacket packet, @NonNull ServerConnection session) {
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
            } else if (packet.getMessage().startsWith("!m")) {
                session.getProxy().getServerConnections().forEach(connection -> {
                            connection.send(new ServerChatPacket("§c" + session.getProfileCache().getProfile().getName() + " > " + packet.getMessage().substring(2).trim() + "§r", true));
                });
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
