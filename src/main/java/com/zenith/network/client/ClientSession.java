package com.zenith.network.client;

import com.zenith.Proxy;
import com.zenith.event.proxy.ConnectEvent;
import com.zenith.event.proxy.DisconnectEvent;
import com.zenith.network.ClientPacketPingTask;
import com.zenith.network.registry.ZenithHandlerCodec;
import com.zenith.util.ComponentSerializer;
import com.zenith.util.Config;
import io.netty.channel.DefaultEventLoop;
import io.netty.channel.EventLoop;
import io.netty.util.concurrent.DefaultThreadFactory;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import org.geysermc.mcprotocollib.network.ProxyInfo;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.network.tcp.TcpClientSession;
import org.geysermc.mcprotocollib.network.tcp.TcpConnectionManager;
import org.geysermc.mcprotocollib.protocol.MinecraftProtocol;
import org.geysermc.mcprotocollib.protocol.data.ProtocolState;
import org.geysermc.mcprotocollib.protocol.data.handshake.HandshakeIntent;
import org.geysermc.mcprotocollib.protocol.packet.handshake.serverbound.ClientIntentionPacket;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static com.zenith.Shared.*;
import static java.util.Objects.isNull;

@Getter
@Setter
public class ClientSession extends TcpClientSession {
    private final EventLoop eventLoop = new DefaultEventLoop(new DefaultThreadFactory("Client Event Loop", true));
    protected boolean serverProbablyOff;
    protected long ping = 0L;
    protected long lastPingId = 0L;
    protected long lastPingSentTime = 0L;

    private boolean inQueue = false;
    private int lastQueuePosition = Integer.MAX_VALUE;
    // in game
    private boolean online = false;
    private boolean disconnected = true;
    private static final ClientTickManager clientTickManager = new ClientTickManager();

    public ClientSession(String host, int port, String bindAddress, MinecraftProtocol protocol, ProxyInfo proxyInfo, TcpConnectionManager tcpManager) {
        super(host, port, bindAddress, 0, protocol, proxyInfo, tcpManager);
    }

    public ClientSession(String host, int port, String bindAddress, MinecraftProtocol protocol, TcpConnectionManager tcpManager) {
        this(host, port, bindAddress, protocol, null, tcpManager);
    }

    public void setOnline(final boolean online) {
        this.online = online;
        if (online) clientTickManager.startClientTicks();
        else clientTickManager.stopClientTicks();
    }

    public void setDisconnected(final boolean disconnected) {
        this.disconnected = disconnected;
        setOnline(false);
    }

    @Override
    public void disconnect(Component reason, Throwable cause) {
        super.disconnect(reason, cause);
        serverProbablyOff = false;
        if (cause == null) {
            serverProbablyOff = true;
        } else if (cause instanceof IOException)    {
            CLIENT_LOG.error("Error during client disconnect", cause);
        } else {
            CLIENT_LOG.error("", cause);
        }
        this.online = false;
    }

    @Override
    public void connect(boolean wait) {
        super.connect(wait);
    }

    @Override
    public void callPacketReceived(Packet packet) {
        try {
            var state = this.getPacketProtocol().getState();
            final Packet p = ZenithHandlerCodec.CLIENT_REGISTRY.handleInbound(packet, this);
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
    public Packet callPacketSending(Packet packet) {
        try {
            return ZenithHandlerCodec.CLIENT_REGISTRY.handleOutgoing(packet, this);
        } catch (Exception e) {
            CLIENT_LOG.error("", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void callPacketSent(Packet packet) {
        try {
            ZenithHandlerCodec.CLIENT_REGISTRY.handlePostOutgoing(packet, this);
        } catch (Exception e) {
            CLIENT_LOG.error("", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean callPacketError(Throwable throwable) {
        CLIENT_LOG.debug("", throwable);
        return true;
    }

    @Override
    public void callConnected() {
        CLIENT_LOG.info("Connected to {}!", getRemoteAddress());
        this.setDisconnected(false);
        send(new ClientIntentionPacket(getPacketProtocol().getCodec().getProtocolVersion(), getHost(), getPort(), HandshakeIntent.LOGIN));
        EVENT_BUS.postAsync(new ConnectEvent());
        if (CONFIG.client.ping.mode == Config.Client.Ping.Mode.PACKET) EXECUTOR.execute(new ClientPacketPingTask(this));
    }

    @Override
    public void callDisconnecting(Component reason, Throwable cause) {
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
    public void callDisconnected(Component reason, Throwable cause) {
        setDisconnected(true);
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
        getClientEventLoop().shutdownGracefully(0L, 15L, TimeUnit.SECONDS).awaitUninterruptibly();
        EVENT_BUS.post(new DisconnectEvent(reasonStr, onlineDuration, Proxy.getInstance().isInQueue(), Proxy.getInstance().getQueuePosition()));
    }

    public EventLoop getClientEventLoop() {
        return eventLoop;
    }
}
