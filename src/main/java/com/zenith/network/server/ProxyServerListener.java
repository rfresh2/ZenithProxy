package com.zenith.network.server;

import com.github.steveice10.mc.protocol.MinecraftProtocol;
import com.github.steveice10.mc.protocol.data.ProtocolState;
import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.event.server.*;
import com.zenith.Proxy;
import com.zenith.event.proxy.ServerConnectionAddedEvent;
import com.zenith.event.proxy.ServerConnectionRemovedEvent;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import static com.zenith.Shared.*;


@RequiredArgsConstructor
@Getter
public class ProxyServerListener implements ServerListener {
    @NonNull
    protected final Proxy proxy;

    public final Reference2ObjectMap<Session, ServerConnection> connections = new Reference2ObjectOpenHashMap<>();

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
        if (((MinecraftProtocol) event.getSession().getPacketProtocol()).getState() != ProtocolState.STATUS) {
            ServerConnection connection = new ServerConnection(event.getSession());
            event.getSession().addListener(connection);
            this.connections.put(event.getSession(), connection);
            if (CONFIG.server.extra.timeout.enable)
                connection.setReadTimeout(CONFIG.server.extra.timeout.seconds);
            else
                connection.setReadTimeout(0);
            EVENT_BUS.post(new ServerConnectionAddedEvent(connection));
        }
    }

    @Override
    public void sessionRemoved(SessionRemovedEvent event) {
        ServerConnection connection = this.connections.remove(event.getSession());
        if (connection != null) {
            this.proxy.getCurrentPlayer().compareAndSet(connection, null);
        }
        EVENT_BUS.post(new ServerConnectionRemovedEvent(connection));
    }
}
