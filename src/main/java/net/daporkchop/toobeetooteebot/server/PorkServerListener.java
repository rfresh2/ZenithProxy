/*
 * Adapted from the Wizardry License
 *
 * Copyright (c) 2016-2018 DaPorkchop_
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

package net.daporkchop.toobeetooteebot.server;

import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.event.server.ServerBoundEvent;
import com.github.steveice10.packetlib.event.server.ServerClosedEvent;
import com.github.steveice10.packetlib.event.server.ServerClosingEvent;
import com.github.steveice10.packetlib.event.server.ServerListener;
import com.github.steveice10.packetlib.event.server.SessionAddedEvent;
import com.github.steveice10.packetlib.event.server.SessionRemovedEvent;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.toobeetooteebot.Bot;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
@Getter
public class PorkServerListener implements ServerListener {
    @NonNull
    private final Bot bot;

    private final Map<Session, PorkServerConnection> connections = Collections.synchronizedMap(new IdentityHashMap<>());

    //this isn't really needed, but it lets me print the correct address to the log
    //TODO: ip-ban specific clients?
    private final Map<Session, SocketAddress> addresses = Collections.synchronizedMap(new IdentityHashMap<>());

    @Override
    public void serverBound(ServerBoundEvent event) {
        System.out.println("Server started.");
    }

    @Override
    public void serverClosing(ServerClosingEvent event) {
    }

    @Override
    public void serverClosed(ServerClosedEvent event) {
    }

    @Override
    public void sessionAdded(SessionAddedEvent event) {
        System.out.printf("Client connected: %s\n", event.getSession().getRemoteAddress());
        PorkServerConnection connection = new PorkServerConnection(this.bot, event.getSession());
        event.getSession().addListener(connection);
        this.bot.getServerConnections().add(connection);
        this.connections.put(event.getSession(), connection);
        this.addresses.put(event.getSession(), event.getSession().getRemoteAddress());
    }

    @Override
    public void sessionRemoved(SessionRemovedEvent event) {
        System.out.printf("Client disconnected: %s\n", this.addresses.remove(event.getSession()));
        this.bot.getServerConnections().remove(this.connections.remove(event.getSession()));
    }
}
