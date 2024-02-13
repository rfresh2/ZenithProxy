package com.zenith.network.client;

import com.github.steveice10.mc.protocol.data.ProtocolState;
import com.github.steveice10.mc.protocol.data.handshake.HandshakeIntent;
import com.github.steveice10.mc.protocol.packet.handshake.serverbound.ClientIntentionPacket;
import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.event.session.SessionListener;
import com.github.steveice10.packetlib.packet.Packet;
import com.zenith.Proxy;
import com.zenith.event.proxy.ConnectEvent;
import com.zenith.event.proxy.DisconnectEvent;
import com.zenith.network.registry.ZenithHandlerCodec;
import com.zenith.network.server.ServerConnection;
import com.zenith.util.ComponentSerializer;
import lombok.NonNull;
import net.kyori.adventure.text.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static com.zenith.Shared.CLIENT_LOG;
import static com.zenith.Shared.EVENT_BUS;
import static java.util.Objects.isNull;

public record ClientListener(@NonNull ClientSession session) implements SessionListener {
    @Override
    public void packetReceived(Session session, Packet packet) {
        try {
            var state = session.getPacketProtocol().getState();
            final Packet p = ZenithHandlerCodec.CLIENT_REGISTRY.handleInbound(packet, this.session);
            if (p != null && state == ProtocolState.GAME) {
                for (ServerConnection connection : Proxy.getInstance().getActiveConnections()) {
                    connection.sendAsync(packet); // sends on each connection's own event loop
                }
            }
        } catch (Exception e) {
            CLIENT_LOG.error("", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public Packet packetSending(final Session session, final Packet packet) {
        try {
            return ZenithHandlerCodec.CLIENT_REGISTRY.handleOutgoing(packet, this.session);
        } catch (Exception e) {
            CLIENT_LOG.error("", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void packetSent(Session session, Packet packet) {
        try {
            ZenithHandlerCodec.CLIENT_REGISTRY.handlePostOutgoing(packet, this.session);
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
        session.send(new ClientIntentionPacket(session.getPacketProtocol().getCodec().getProtocolVersion(), session.getHost(), session.getPort(), HandshakeIntent.LOGIN));
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
            reasonStr = ComponentSerializer.serializePlain(reason);
        } catch (final Exception e) {
            CLIENT_LOG.warn("Unable to parse disconnect reason: {}", reason, e);
            reasonStr = isNull(reason) ? "Disconnected" : ComponentSerializer.serializeJson(reason);
        }
        CLIENT_LOG.info("Disconnected: " + reasonStr);
        var connectTime = Optional.ofNullable(Proxy.getInstance().getConnectTime()).orElse(Instant.now());
        var disconnectTime = Instant.now();
        var onlineDuration = Duration.between(connectTime, disconnectTime);
        EVENT_BUS.post(new DisconnectEvent(reasonStr, onlineDuration));
    }

}
