package com.zenith.server;

import com.github.steveice10.mc.protocol.MinecraftProtocol;
import com.github.steveice10.mc.protocol.data.SubProtocol;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerChatPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntityDestroyPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntityMetadataPacket;
import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.crypt.PacketEncryption;
import com.github.steveice10.packetlib.event.session.*;
import com.github.steveice10.packetlib.packet.Packet;
import com.github.steveice10.packetlib.packet.PacketProtocol;
import com.zenith.Proxy;
import com.zenith.cache.data.PlayerCache;
import com.zenith.cache.data.ServerProfileCache;
import com.zenith.cache.data.entity.EntityCache;
import com.zenith.event.proxy.ProxyClientDisconnectedEvent;
import com.zenith.event.proxy.ProxySpectatorDisconnectedEvent;
import com.zenith.util.spectator.SpectatorEntityRegistry;
import com.zenith.util.spectator.entity.SpectatorEntity;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import net.daporkchop.lib.minecraft.text.parser.AutoMCFormatParser;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.ClosedChannelException;
import java.util.*;

import static com.zenith.util.Constants.*;


@Getter
@Setter
public class ServerConnection implements Session, SessionListener {

    protected final Proxy proxy;

    protected final Session session;

    public ServerConnection(final Proxy proxy, final Session session) {
        this.proxy = proxy;
        this.session = session;
        initSpectatorEntity();
    }

    protected long lastPacket = System.currentTimeMillis();

    protected boolean isPlayer = false;
    protected boolean isLoggedIn = false;
    protected boolean isSpectator = false;
    protected boolean onlySpectator = false;
    protected boolean allowSpectatorServerPlayerPosRotate = true;
    // allow spectator to set their camera to client
    // need to persist state to allow them in and out of this
    protected boolean playerCam = false;
    protected boolean showSelfEntity = true;
    protected int spectatorEntityId = 2147483647 - this.hashCode();
    protected int spectatorSelfEntityId = spectatorEntityId - 1;
    protected UUID spectatorEntityUUID = UUID.randomUUID();
    protected ServerProfileCache profileCache = new ServerProfileCache();
    protected PlayerCache spectatorPlayerCache = new PlayerCache(new EntityCache());
    protected SpectatorEntity spectatorEntity;

    @Override
    public void packetReceived(Session session, Packet packet) {
        try {
            if (!isSpectator()) {
                this.lastPacket = System.currentTimeMillis();
                if (SERVER_PLAYER_HANDLERS.handleInbound(packet, this)
                        && ((MinecraftProtocol) this.session.getPacketProtocol()).getSubProtocol() == SubProtocol.GAME
                        && this.isLoggedIn) {
                    this.proxy.getClient().send(packet);
                }
            } else {
                if (SERVER_SPECTATOR_HANDLERS.handleInbound(packet, this)
                        && ((MinecraftProtocol) this.session.getPacketProtocol()).getSubProtocol() == SubProtocol.GAME
                        && this.isLoggedIn) {
                    this.proxy.getClient().send(packet);
                }
            }
        } catch (final Exception e) {
            SERVER_LOG.error("Failed handling Received packet: " + packet.getClass().getSimpleName(), e);
        }
    }

    @Override
    public void packetSending(PacketSendingEvent event) {
        try {
            Packet p1 = event.getPacket();
            Packet p2;
            if (!isSpectator()) {
                p2 = SERVER_PLAYER_HANDLERS.handleOutgoing(p1, this);
            } else {
                p2 = SERVER_SPECTATOR_HANDLERS.handleOutgoing(p1, this);
            }
            if (p2 == null) {
                event.setCancelled(true);
            } else if (p1 != p2) {
                event.setPacket(p2);
            }
        } catch (final Exception e) {
            SERVER_LOG.error("Failed handling Sending packet: " + event.getPacket().getClass().getSimpleName(), e);
        }
    }

    @Override
    public void packetSent(Session session, Packet packet) {
        try {
            if (!isSpectator()) {
                SERVER_PLAYER_HANDLERS.handlePostOutgoing(packet, this);
            } else {
                SERVER_SPECTATOR_HANDLERS.handlePostOutgoing(packet, this);
            }
        } catch (final Exception e) {
            SERVER_LOG.error("Failed handling PostOutgoing packet: " + packet.getClass().getSimpleName(), e);
        }
    }

    @Override
    public void packetError(PacketErrorEvent event) {
        SERVER_LOG.error("", event.getCause());
    }

    @Override
    public void connected(ConnectedEvent event) {
    }

    @Override
    public void disconnecting(DisconnectingEvent event) {
    }

    @Override
    public void disconnected(DisconnectedEvent event) {
        if (event.getCause() != null && !((event.getCause() instanceof IOException || event.getCause() instanceof ClosedChannelException) && !this.isPlayer)) {
            // any scanners or TCP connections established result in a lot of these coming in even when they are not actually speaking mc protocol
            SERVER_LOG.warn(String.format("Connection disconnected: %s", event.getSession().getRemoteAddress()), event.getCause());
            return;
        }
        if (this.isPlayer) {
            final String reason = AutoMCFormatParser.DEFAULT.parse(event.getReason()).toRawString();
            if (!isSpectator()) {
                SERVER_LOG.info("Player disconnected: {}, {}", event.getSession().getRemoteAddress(), reason, event.getCause());
                try {
                    EVENT_BUS.dispatch(new ProxyClientDisconnectedEvent(event.getReason(), profileCache.getProfile()));
                } catch (final Throwable e) {
                    SERVER_LOG.info("Could not get game profile of disconnecting player");
                    EVENT_BUS.dispatch(new ProxyClientDisconnectedEvent(reason));
                }
            } else {
                proxy.getServerConnections().forEach(connection -> {
                    connection.send(new ServerEntityDestroyPacket(this.spectatorEntityId));
                    connection.send(new ServerChatPacket("§9" + profileCache.getProfile().getName() + " disconnected§r", true));
                });
                EVENT_BUS.dispatch(new ProxySpectatorDisconnectedEvent(profileCache.getProfile()));
            }
        }
    }

    public void send(@NonNull Packet packet) {
        this.session.send(packet);
    }

    public boolean isActivePlayer() {
        // note: this could be false for the player connection during some points of disconnect
        return Objects.equals(this.proxy.getCurrentPlayer().get(), this);
    }

    // Spectator helper methods

    public Packet getEntitySpawnPacket() {
        return spectatorEntity.getSpawnPacket(spectatorEntityId, spectatorEntityUUID, spectatorPlayerCache, profileCache.getProfile());
    }

    public ServerEntityMetadataPacket getSelfEntityMetadataPacket() {
        return new ServerEntityMetadataPacket(spectatorEntityId, spectatorEntity.getSelfEntityMetadata(profileCache.getProfile(), spectatorEntityId));
    }

    public ServerEntityMetadataPacket getEntityMetadataPacket() {
        return new ServerEntityMetadataPacket(spectatorEntityId, spectatorEntity.getEntityMetadata(profileCache.getProfile(), spectatorEntityId));
    }

    public Optional<Packet> getSoundPacket() {
        return spectatorEntity.getSoundPacket(spectatorPlayerCache);
    }

    public void initSpectatorEntity() {
        this.spectatorEntity = SpectatorEntityRegistry.getSpectatorEntityWithDefault(CONFIG.server.spectator.spectatorEntity);
    }

    // todo: might rework this to handle respawns in some central place
    public boolean setSpectatorEntity(final String identifier) {
        Optional<SpectatorEntity> entity = SpectatorEntityRegistry.getSpectatorEntity(identifier);
        if (entity.isPresent()) {
            this.spectatorEntity = entity.get();
            return true;
        } else {
            return false;
        }
    }

    //
    //
    //
    // SESSION METHOD IMPLEMENTATIONS
    //
    //
    //

    @Override
    public void connect() {
        this.session.connect();
    }

    @Override
    public void connect(boolean wait) {
        this.session.connect(wait);
    }

    @Override
    public String getHost() {
        return this.session.getHost();
    }

    @Override
    public int getPort() {
        return this.session.getPort();
    }

    @Override
    public SocketAddress getLocalAddress() {
        return this.session.getLocalAddress();
    }

    @Override
    public SocketAddress getRemoteAddress() {
        return this.session.getRemoteAddress();
    }

    @Override
    public PacketProtocol getPacketProtocol() {
        return this.session.getPacketProtocol();
    }

    @Override
    public Map<String, Object> getFlags() {
        return this.session.getFlags();
    }

    @Override
    public boolean hasFlag(String key) {
        return this.session.hasFlag(key);
    }

    @Override
    public <T> T getFlag(String key) {
        return this.session.getFlag(key);
    }

    @Override
    public <T> T getFlag(String key, T def) {
        return this.session.getFlag(key, def);
    }

    @Override
    public void setFlag(String key, Object value) {
        this.session.setFlag(key, value);
    }

    @Override
    public List<SessionListener> getListeners() {
        return this.session.getListeners();
    }

    @Override
    public void addListener(SessionListener listener) {
        this.session.addListener(listener);
    }

    @Override
    public void removeListener(SessionListener listener) {
        this.session.removeListener(listener);
    }

    @Override
    public void callEvent(SessionEvent event) {
        this.session.callEvent(event);
    }

    @Override
    public void callPacketReceived(Packet packet) {
        this.session.callPacketReceived(packet);
    }

    @Override
    public void callPacketSent(Packet packet) {
        this.session.callPacketSent(packet);
    }

    @Override
    public int getCompressionThreshold() {
        return this.session.getCompressionThreshold();
    }

    @Override
    public void setCompressionThreshold(int threshold, boolean validateCompression) {
        this.session.setCompressionThreshold(threshold, validateCompression);
    }

    @Override
    public void enableEncryption(PacketEncryption encryption) {
        this.session.enableEncryption(encryption);
    }

    @Override
    public int getConnectTimeout() {
        return this.session.getConnectTimeout();
    }

    @Override
    public void setConnectTimeout(int timeout) {
        this.session.setConnectTimeout(timeout);
    }

    @Override
    public int getReadTimeout() {
        return this.session.getReadTimeout();
    }

    @Override
    public void setReadTimeout(int timeout) {
        this.session.setReadTimeout(timeout);
    }

    @Override
    public int getWriteTimeout() {
        return this.session.getWriteTimeout();
    }

    @Override
    public void setWriteTimeout(int timeout) {
        this.session.setWriteTimeout(timeout);
    }

    @Override
    public boolean isConnected() {
        return this.session.isConnected();
    }

    @Override
    public void disconnect(String reason) {
        this.session.disconnect(reason);
    }

    @Override
    public void disconnect(String reason, Throwable cause) {
        this.session.disconnect(reason, cause);
    }
}
