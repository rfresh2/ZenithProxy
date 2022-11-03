package com.zenith.server;

import com.github.steveice10.mc.protocol.MinecraftProtocol;
import com.github.steveice10.mc.protocol.data.SubProtocol;
import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.event.server.*;
import com.zenith.Proxy;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.net.SocketAddress;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;

import static com.zenith.util.Constants.SERVER_LOG;


@RequiredArgsConstructor
@Getter
public class ProxyServerListener implements ServerListener {
    @NonNull
    protected final Proxy proxy;

    public final Map<Session, ServerConnection> connections = Collections.synchronizedMap(new IdentityHashMap<>());

    //this isn't really needed, but it lets me print the correct address to the log
    //TODO: ip-ban specific clients?
    protected final Map<Session, SocketAddress> addresses = Collections.synchronizedMap(new IdentityHashMap<>());

    @Override
    public void serverBound(ServerBoundEvent event) {
        SERVER_LOG.info("Server started.");
    }

    @Override
    public void serverClosing(ServerClosingEvent event) {
        SERVER_LOG.info("Closing server...");
    }

    @Override
    public void serverClosed(ServerClosedEvent event) {
        SERVER_LOG.info("Server closed.");
    }

    @Override
    public void sessionAdded(SessionAddedEvent event) {
        if (((MinecraftProtocol) event.getSession().getPacketProtocol()).getSubProtocol() != SubProtocol.STATUS) {
            ServerConnection connection = new ServerConnection(this.proxy, event.getSession());
            event.getSession().addListener(connection);
            this.addresses.put(event.getSession(), event.getSession().getRemoteAddress());
            this.connections.put(event.getSession(), connection);
        }
    }

    @Override
    public void sessionRemoved(SessionRemovedEvent event) {
        this.addresses.remove(event.getSession());
        ServerConnection connection = this.connections.remove(event.getSession());
        if (connection != null) {
            this.proxy.getCurrentPlayer().compareAndSet(connection, null);
        }
    }
}
