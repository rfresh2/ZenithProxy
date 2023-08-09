package com.zenith.network.client;

import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.event.session.*;
import com.github.steveice10.packetlib.packet.Packet;
import com.zenith.Proxy;
import com.zenith.event.proxy.ConnectEvent;
import com.zenith.event.proxy.DisconnectEvent;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.lib.minecraft.text.parser.AutoMCFormatParser;

import static com.zenith.Shared.*;
import static java.util.Objects.isNull;


@RequiredArgsConstructor
@Getter
public class ClientListener implements SessionListener {
    @NonNull
    protected final Proxy proxy;

    @NonNull
    protected final ClientSession session;

    @Override
    public void packetReceived(Session session, Packet packet) {
        try {
            if (CLIENT_HANDLERS.handleInbound(packet, this.session)) {
                this.proxy.getActiveConnections().forEach(connection -> connection.send(packet));
            }
        } catch (Exception e) {
            CLIENT_LOG.error("", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void packetSending(PacketSendingEvent event) {
        try {
            Packet p1 = event.getPacket();
            Packet p2 = CLIENT_HANDLERS.handleOutgoing(p1, this.session);
            if (p2 == null) {
                event.setCancelled(true);
            } else if (p1 != p2) {
                event.setPacket(p2);
            }
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
    public void packetError(PacketErrorEvent event) {
        CLIENT_LOG.error("", event.getCause());
    }

    @Override
    public void connected(ConnectedEvent event) {
        CLIENT_LOG.info("Connected to {}!", event.getSession().getRemoteAddress());
        session.setDisconnected(false);
        EVENT_BUS.postAsync(new ConnectEvent());
    }

    @Override
    public void disconnecting(DisconnectingEvent event) {
        try {
            CLIENT_LOG.info("Disconnecting from server...");
            CLIENT_LOG.trace("Disconnect reason: {}", event.getReason());
            // reason can be malformed for MC parser the logger uses
        } catch (final Exception e) {
            // fall through
        }
        this.proxy.getActiveConnections().forEach(connection -> connection.disconnect(event.getReason()));
    }

    @Override
    public void disconnected(DisconnectedEvent event) {
        CLIENT_LOG.info("Disconnected: " + event.getReason());
        session.setDisconnected(true);
        String reason;
        try {
            reason = AutoMCFormatParser.DEFAULT.parse(event.getReason()).toRawString();
        } catch (final Exception e) {
            CLIENT_LOG.warn("Unable to parse disconnect reason: {}", event.getReason(), e);
            reason = isNull(event.getReason()) ? "Disconnected" : event.getReason();
        }
        EVENT_BUS.post(new DisconnectEvent(reason));
    }
}
