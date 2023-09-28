package com.zenith.network.client;

import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.event.session.SessionListener;
import com.github.steveice10.packetlib.packet.Packet;
import com.zenith.Proxy;
import com.zenith.event.proxy.ConnectEvent;
import com.zenith.event.proxy.DisconnectEvent;
import com.zenith.util.ComponentSerializer;
import lombok.NonNull;
import net.kyori.adventure.text.Component;

import static com.zenith.Shared.*;
import static java.util.Objects.isNull;

public record ClientListener(@NonNull ClientSession session) implements SessionListener {
    @Override
    public void packetReceived(Session session, Packet packet) {
        try {
            if (CLIENT_HANDLERS.handleInbound(packet, this.session)) {
                Proxy.getInstance().getActiveConnections().forEach(connection -> connection.send(packet));
            }
        } catch (Exception e) {
            CLIENT_LOG.error("", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public Packet packetSending(final Session session, final Packet packet) {
        try {
            return CLIENT_HANDLERS.handleOutgoing(packet, this.session);
        } catch (Exception e) {
            CLIENT_LOG.error("", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void packetSent(Session session, Packet packet) {
        try {
            CLIENT_HANDLERS.handlePostOutgoing(packet, this.session);
        } catch (Exception e) {
            CLIENT_LOG.error("", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean packetError(final Session session, final Throwable throwable) {
        CLIENT_LOG.debug("", throwable);
        return true;
    }

    @Override
    public void connected(final Session session) {
        CLIENT_LOG.info("Connected to {}!", session.getRemoteAddress());
        this.session.setDisconnected(false);
        EVENT_BUS.postAsync(new ConnectEvent());
    }

    @Override
    public void disconnecting(final Session session, final Component reason, final Throwable cause) {
        try {
            CLIENT_LOG.info("Disconnecting from server...");
            CLIENT_LOG.trace("Disconnect reason: {}", reason);
            // reason can be malformed for MC parser the logger uses
        } catch (final Exception e) {
            // fall through
        }
        Proxy.getInstance().getActiveConnections().forEach(connection -> connection.disconnect(reason));
    }

    @Override
    public void disconnected(final Session session, final Component reason, final Throwable cause) {
        this.session.setDisconnected(true);
        String reasonStr;
        try {
            reasonStr = ComponentSerializer.toRawString(reason);
        } catch (final Exception e) {
            CLIENT_LOG.warn("Unable to parse disconnect reason: {}", reason, e);
            reasonStr = isNull(reason) ? "Disconnected" : ComponentSerializer.serialize(reason);
        }
        CLIENT_LOG.info("Disconnected: " + reasonStr);
        EVENT_BUS.post(new DisconnectEvent(reasonStr));
    }

}
