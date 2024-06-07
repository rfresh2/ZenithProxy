package com.zenith.network.client;

import com.zenith.Proxy;
import com.zenith.event.proxy.ConnectEvent;
import com.zenith.event.proxy.DisconnectEvent;
import com.zenith.network.ClientPacketPingTask;
import com.zenith.network.registry.ZenithHandlerCodec;
import com.zenith.util.ComponentSerializer;
import com.zenith.util.Config;
import lombok.Getter;
import lombok.NonNull;
import net.kyori.adventure.text.Component;
import org.geysermc.mcprotocollib.network.Session;
import org.geysermc.mcprotocollib.network.event.session.SessionListener;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.data.ProtocolState;
import org.geysermc.mcprotocollib.protocol.data.handshake.HandshakeIntent;
import org.geysermc.mcprotocollib.protocol.packet.handshake.serverbound.ClientIntentionPacket;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static com.zenith.Shared.*;
import static java.util.Objects.isNull;

@Getter
public class ClientListener implements SessionListener {
    @NonNull ClientSession session;

    public ClientListener(final @NotNull ClientSession session) {
        this.session = session;
    }

    @Override
    public void packetReceived(Session session, Packet packet) {
        try {
            var state = session.getPacketProtocol().getState();
            final Packet p = ZenithHandlerCodec.CLIENT_REGISTRY.handleInbound(packet, this.session);
            if (p != null && (state == ProtocolState.GAME || state == ProtocolState.CONFIGURATION)) {
                // sends on each connection's own event loop
                var connections = Proxy.getInstance().getActiveConnections().getArray();
                for (int i = 0; i < connections.length; i++) {
                    var connection = connections[i];
                    if (state == ProtocolState.CONFIGURATION && !connection.isConfigured()) continue;
                    connection.sendAsync(p);
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
        session.send(new ClientIntentionPacket(session.getPacketProtocol().getCodec().getProtocolVersion(), session.getHost(), session.getPort(), HandshakeIntent.LOGIN));
        EVENT_BUS.postAsync(new ConnectEvent());
        if (CONFIG.client.ping.mode == Config.Client.Ping.Mode.PACKET) EXECUTOR.execute(new ClientPacketPingTask(this.session));
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
        var connections = Proxy.getInstance().getActiveConnections().getArray();
        for (int i = 0; i < connections.length; i++) {
            var connection = connections[i];
            connection.disconnect(reason);
        }
        Proxy.getInstance().getCurrentPlayer().set(null);
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
        CLIENT_LOG.info("Disconnected: {}", reasonStr);
        var onlineDuration = Duration.ofSeconds(Proxy.getInstance().getOnlineTimeSeconds());
        // stop processing packets before we reset the client cache to avoid race conditions
        this.session.getClientEventLoop().shutdownGracefully(0L, 15L, TimeUnit.SECONDS).awaitUninterruptibly();
        EVENT_BUS.post(new DisconnectEvent(reasonStr, onlineDuration, Proxy.getInstance().isInQueue(), Proxy.getInstance().getQueuePosition()));
    }

}
