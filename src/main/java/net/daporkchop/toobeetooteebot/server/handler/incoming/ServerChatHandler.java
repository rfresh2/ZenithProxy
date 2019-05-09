/*
 * Adapted from the Wizardry License
 *
 * Copyright (c) 2016-2019 DaPorkchop_
 *
 * Permission is hereby granted to any persons and/or organizations using this software to copy, modify, merge, publish, and distribute it.
 * Said persons and/or organizations are not allowed to use the software or any derivatives of the work for commercial use or any other means to generate income, nor are they allowed to claim this software as their own.
 *
 * The persons and/or organizations are also disallowed from sub-licensing and/or trademarking this software without explicit permission from DaPorkchop_.
 *
 * Any persons and/or organizations using this software must disclose their source code and have it publicly available, include this license, provide sufficient credit to the original authors of the project (IE: DaPorkchop_), as well as provide a link to the original project.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON INFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

package net.daporkchop.toobeetooteebot.server.handler.incoming;

import com.github.steveice10.mc.protocol.packet.ingame.client.ClientChatPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerChatPacket;
import net.daporkchop.toobeetooteebot.server.PorkServerConnection;
import net.daporkchop.toobeetooteebot.util.handler.HandlerRegistry;

import javax.annotation.Nonnull;
import java.lang.reflect.Field;

/**
 * @author DaPorkchop_
 */
public class ServerChatHandler implements HandlerRegistry.IncomingHandler<ClientChatPacket, PorkServerConnection> {
    @Override
    public boolean apply(@Nonnull ClientChatPacket packet, @Nonnull PorkServerConnection session) {
        if (packet.getMessage().startsWith("!"))   {
            if (packet.getMessage().startsWith("!!"))   {
                //allow sending ingame commands to bots or whatever
                try  {
                    Field f = ClientChatPacket.class.getDeclaredField("message");
                    f.setAccessible(true);
                    f.set(packet, packet.getMessage().substring(1));
                } catch (NoSuchFieldException
                        | IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
                return true;
            } else if ("!dc".equalsIgnoreCase(packet.getMessage())) {
                session.getBot().getClient().getSession().disconnect("User forced disconnect", false);
                return false;
            } else if ("!reboot".equalsIgnoreCase(packet.getMessage())) {
                SHOULD_RECONNECT.set(false);
                session.getBot().getClient().getSession().disconnect("User forced disconnect", false);
                return false;
            } else {
                session.send(new ServerChatPacket(String.format("§cUnknown command: §o%s", packet.getMessage())));
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
