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

package com.zenith.server;

import com.github.steveice10.mc.protocol.MinecraftProtocol;
import com.github.steveice10.mc.protocol.data.SubProtocol;
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
import com.zenith.Bot;

import java.net.SocketAddress;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;

import static com.zenith.util.Constants.*;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
@Getter
public class PorkServerListener implements ServerListener {
    @NonNull
    protected final Bot bot;

    protected final Map<Session, PorkServerConnection> connections = Collections.synchronizedMap(new IdentityHashMap<>());

    //this isn't really needed, but it lets me print the correct address to the log
    //TODO: ip-ban specific clients?
    protected final Map<Session, SocketAddress> addresses = Collections.synchronizedMap(new IdentityHashMap<>());

    @Override
    public void serverBound(ServerBoundEvent event) {
        SERVER_LOG.success("Server started.");
    }

    @Override
    public void serverClosing(ServerClosingEvent event) {
        SERVER_LOG.info("Closing server...");
    }

    @Override
    public void serverClosed(ServerClosedEvent event) {
        SERVER_LOG.success("Server closed.");
    }

    @Override
    public void sessionAdded(SessionAddedEvent event) {
        //SERVER_LOG.info("session added");
        if (((MinecraftProtocol) event.getSession().getPacketProtocol()).getSubProtocol() != SubProtocol.STATUS) {
            PorkServerConnection connection = new PorkServerConnection(this.bot, event.getSession());
            event.getSession().addListener(connection);
            this.addresses.put(event.getSession(), event.getSession().getRemoteAddress());
            this.connections.put(event.getSession(), connection);
        }
    }

    @Override
    public void sessionRemoved(SessionRemovedEvent event) {
        this.addresses.remove(event.getSession());
        PorkServerConnection connection = this.connections.remove(event.getSession());
        if (connection != null) {
            this.bot.getCurrentPlayer().compareAndSet(connection, null);
        }
    }
}
