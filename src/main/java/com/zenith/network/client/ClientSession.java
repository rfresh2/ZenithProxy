package com.zenith.network.client;

import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.zenith.Proxy;
import com.zenith.event.client.ClientConnectEvent;
import com.zenith.event.client.ClientDisconnectEvent;
import com.zenith.mc.biome.BiomeRegistry;
import com.zenith.mc.block.BlockRegistry;
import com.zenith.network.ClientKeepAliveTask;
import com.zenith.network.ClientPacketPingTask;
import com.zenith.network.ClientPongTask;
import com.zenith.network.codec.PacketCodecRegistries;
import com.zenith.util.ComponentSerializer;
import io.netty.channel.DefaultEventLoop;
import io.netty.channel.EventLoop;
import io.netty.util.concurrent.DefaultThreadFactory;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import org.geysermc.mcprotocollib.auth.GameProfile;
import org.geysermc.mcprotocollib.network.ProxyInfo;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.network.tcp.TcpClientSession;
import org.geysermc.mcprotocollib.network.tcp.TcpConnectionManager;
import org.geysermc.mcprotocollib.protocol.MinecraftProtocol;
import org.geysermc.mcprotocollib.protocol.codec.MinecraftCodec;
import org.geysermc.mcprotocollib.protocol.data.ProtocolState;
import org.geysermc.mcprotocollib.protocol.data.game.chunk.PalettedWorldState;
import org.geysermc.mcprotocollib.protocol.data.handshake.HandshakeIntent;
import org.geysermc.mcprotocollib.protocol.packet.handshake.serverbound.ClientIntentionPacket;
import org.geysermc.mcprotocollib.protocol.packet.login.serverbound.ServerboundHelloPacket;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static com.zenith.Globals.*;
import static com.zenith.via.ZenithViaInitializer.VIA_USER;
import static java.util.Objects.isNull;

@Getter
@Setter
public class ClientSession extends TcpClientSession {
    // client tick eventloop, separate from the netty event loop
    private final EventLoop clientEventLoop = new DefaultEventLoop(new DefaultThreadFactory("Client Event Loop", true));
    protected long ping = 0L;
    protected long lastPingId = 0L;
    protected long lastPingSentTime = 0L;

    private boolean inQueue = false;
    private int lastQueuePosition = Integer.MAX_VALUE;
    // in game
    private boolean online = false;
    private boolean wasOnline = false;
    // if we are still attempting to connect or this session has disconnected
    private boolean disconnected = true;
    // if we have been disconnected and this session cannot be reused
    private boolean terminalState = false;
    // profile we logged in with
    // MC servers can send a different profile back, which will be stored in `CACHE.getProfileCache()`
    private final GameProfile profile;
    private final String accessToken;
    private int protocolVersionId;
    private static final ClientTickManager clientTickManager = new ClientTickManager();
    private PalettedWorldState palettedWorldState = createPalettedWorldState(1);

    public ClientSession(String host, int port, String bindAddress, MinecraftProtocol protocol, ProxyInfo proxyInfo, TcpConnectionManager tcpManager) {
        super(host, port, bindAddress, 0, protocol, proxyInfo, tcpManager);
        profile = protocol.getProfile();
        accessToken = protocol.getAccessToken();
    }

    public ClientSession(String host, int port, String bindAddress, MinecraftProtocol protocol, TcpConnectionManager tcpManager) {
        this(host, port, bindAddress, protocol, null, tcpManager);
    }

    public void setOnline(final boolean online) {
        this.online = online;
        if (online) {
            clientTickManager.startClientTicks();
            wasOnline = true;
        }
        else clientTickManager.stopClientTicks();
    }

    public void setDisconnected(final boolean disconnected) {
        this.disconnected = disconnected;
        if (disconnected) setTerminalState(true);
        setOnline(false);
    }

    @Override
    public PalettedWorldState getPalettedWorldState() {
        return palettedWorldState;
    }

    public PalettedWorldState createPalettedWorldState(int sectionsCount) {
        int defaultBiomeId = 0;
        var plains = BiomeRegistry.PLAINS.get();
        if (plains != null) defaultBiomeId = plains.id();
        return new PalettedWorldState(
            sectionsCount,
            BLOCK_DATA.blockStateRegistrySize(),
            BlockRegistry.AIR.minStateId(),
            BiomeRegistry.REGISTRY.size(),
            defaultBiomeId
        );
    }

    @Override
    public void disconnect(Component reason, Throwable cause) {
        super.disconnect(reason, cause);
        if (cause != null) CLIENT_LOG.error("Exception during client disconnect", cause);
        this.online = false;
    }

    @Override
    public void connect(boolean wait) {
        super.connect(wait);
    }

    @Override
    public void callPacketReceived(Packet packet) {
        try {
            var state = this.getPacketProtocol().getInboundState();
            final Packet p = PacketCodecRegistries.CLIENT_REGISTRY.handleInbound(packet, this);
            if (p != null && (state == ProtocolState.GAME || state == ProtocolState.CONFIGURATION)) {
                // sends on each connection's own event loop
                var connections = Proxy.getInstance().getActiveConnections().getArray();
                for (int i = 0; i < connections.length; i++) {
                    var connection = connections[i];
                    if (state == ProtocolState.CONFIGURATION && !connection.isConfigured()) continue;
                    if (connection.isSpectator() && PacketCodecRegistries.SPECTATOR_PACKET_FILTER.contains(p.getClass())) continue;
                    connection.sendAsync(p);
                }
            }
        } catch (Exception e) {
            CLIENT_LOG.error("Client Packet Received Error", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public Packet callPacketSending(Packet packet) {
        try {
            return PacketCodecRegistries.CLIENT_REGISTRY.handleOutgoing(packet, this);
        } catch (Exception e) {
            CLIENT_LOG.error("Client Packet Sending Error", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void callPacketSent(Packet packet) {
        try {
            PacketCodecRegistries.CLIENT_REGISTRY.handlePostOutgoing(packet, this);
        } catch (Exception e) {
            CLIENT_LOG.error("Client Packet Sent Error", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean callPacketError(Throwable throwable) {
        CLIENT_LOG.debug("Packet Error", throwable);
        return true;
    }

    @Override
    public void callConnected() {
        CLIENT_LOG.info("Connected to {}!", getRemoteAddress());
        this.setDisconnected(false);
        switchInboundState(ProtocolState.LOGIN);
        send(new ClientIntentionPacket(getPacketProtocol().getCodec().getProtocolVersion(), getHost(), getPort(), HandshakeIntent.LOGIN));
        switchOutboundState(ProtocolState.LOGIN);
        updateClientProtocolVersion();
        EVENT_BUS.postAsync(new ClientConnectEvent());
        send(new ServerboundHelloPacket(profile.getName(), profile.getId()));
        clientEventLoop.scheduleAtFixedRate(new ClientPacketPingTask(this), 0, CONFIG.client.ping.pingIntervalSeconds, TimeUnit.SECONDS);
        clientEventLoop.scheduleAtFixedRate(new ClientKeepAliveTask(this), 0, 50, TimeUnit.MILLISECONDS);
        clientEventLoop.scheduleAtFixedRate(new ClientPongTask(this), 0, 50, TimeUnit.MILLISECONDS);
    }

    private void updateClientProtocolVersion() {
        var nativeZenithProtocol = ProtocolVersion.getProtocol(MinecraftCodec.CODEC.getProtocolVersion());;
        var clientProtocolVersion = nativeZenithProtocol;
        var clientChannel = Proxy.getInstance().getClient().getChannel();
        if (clientChannel.hasAttr(VIA_USER)) {
            var viaUserConnection = clientChannel.attr(VIA_USER).get();
            if (viaUserConnection == null) return;
            clientProtocolVersion = viaUserConnection.getProtocolInfo().serverProtocolVersion();
        }
        protocolVersionId = clientProtocolVersion.getVersion();
    }

    @Override
    public void callDisconnecting(Component reason, Throwable cause) {
        try {
            CLIENT_LOG.info("Disconnecting from server...");
            CLIENT_LOG.trace("Disconnect reason: {}", reason);
            // reason can be malformed for MC parser the logger uses
            var connections = Proxy.getInstance().getActiveConnections().getArray();
            for (int i = 0; i < connections.length; i++) {
                var connection = connections[i];
                connection.disconnect(reason);
            }
        } catch (final Exception e) {
            // fall through
        } finally {
            Proxy.getInstance().getCurrentPlayer().set(null);
        }
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
        CLIENT_LOG.info("Disconnected: {}", reason != null ? reason : reasonStr);
        var onlineDuration = Duration.ofSeconds(Proxy.getInstance().getOnlineTimeSeconds());
        var onlineDurationWithQueueSkip = Duration.ofSeconds(Proxy.getInstance().getOnlineTimeSecondsWithQueueSkip());
        try {
            CLIENT_LOG.trace("Shutting down client event loop...");
            // stop processing packets before we reset the client cache to avoid race conditions
            getClientEventLoop().shutdownGracefully(0L, 15L, TimeUnit.SECONDS).awaitUninterruptibly(20L, TimeUnit.SECONDS);
        } catch (Exception e) {
            CLIENT_LOG.error("Error awaiting client event loop shutdown", e);
        }
        EVENT_BUS.post(new ClientDisconnectEvent(reasonStr, onlineDuration, onlineDurationWithQueueSkip, Proxy.getInstance().isInQueue(), Proxy.getInstance().getQueuePosition()));
    }

    public ProtocolVersion getProtocolVersion() {
        return ProtocolVersion.getProtocol(protocolVersionId);
    }

    public void executeInEventLoop(Runnable runnable) {
        if (clientEventLoop.inEventLoop() || clientEventLoop.isShuttingDown()) {
            runnable.run();
        } else {
            clientEventLoop.execute(runnable);
        }
    }
}
