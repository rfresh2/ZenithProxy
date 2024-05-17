package com.zenith.network.server;

import com.github.steveice10.mc.auth.data.GameProfile;
import com.zenith.Proxy;
import com.zenith.cache.data.PlayerCache;
import com.zenith.cache.data.ServerProfileCache;
import com.zenith.cache.data.entity.Entity;
import com.zenith.cache.data.entity.EntityCache;
import com.zenith.event.proxy.ProxyClientDisconnectedEvent;
import com.zenith.event.proxy.ProxySpectatorDisconnectedEvent;
import com.zenith.feature.spectator.SpectatorEntityRegistry;
import com.zenith.feature.spectator.entity.SpectatorEntity;
import com.zenith.network.registry.ZenithHandlerCodec;
import com.zenith.util.ComponentSerializer;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.EventLoop;
import io.netty.handler.codec.DecoderException;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import org.geysermc.mcprotocollib.network.Flag;
import org.geysermc.mcprotocollib.network.Session;
import org.geysermc.mcprotocollib.network.codec.PacketCodecHelper;
import org.geysermc.mcprotocollib.network.event.session.SessionListener;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.network.tcp.TcpSession;
import org.geysermc.mcprotocollib.protocol.MinecraftProtocol;
import org.geysermc.mcprotocollib.protocol.data.ProtocolState;
import org.geysermc.mcprotocollib.protocol.data.game.scoreboard.CollisionRule;
import org.geysermc.mcprotocollib.protocol.data.game.scoreboard.NameTagVisibility;
import org.geysermc.mcprotocollib.protocol.data.game.scoreboard.TeamAction;
import org.geysermc.mcprotocollib.protocol.data.game.scoreboard.TeamColor;
import org.geysermc.mcprotocollib.protocol.packet.common.clientbound.ClientboundDisconnectPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundSystemChatPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundRemoveEntitiesPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundSetEntityDataPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.scoreboard.ClientboundSetPlayerTeamPacket;
import org.geysermc.mcprotocollib.protocol.packet.login.clientbound.ClientboundLoginDisconnectPacket;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.net.SocketAddress;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.zenith.Shared.*;


@Getter
@Setter
public class ServerConnection implements Session, SessionListener {
    protected final TcpSession session;

    public static final int DEFAULT_COMPRESSION_THRESHOLD = 256;

    // Always empty post-1.7
    private static final String SERVER_ID = "";
    private static final KeyPair KEY_PAIR;

    static {
        try {
            KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
            gen.initialize(1024);
            KEY_PAIR = gen.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Failed to generate server key pair.", e);
        }
    }

    private final byte[] challenge = new byte[4];
    private String username = "";
    // as requested by the player during login. may not be the same as what mojang api returns
    private @Nullable UUID loginProfileUUID;
    private int protocolVersion; // as reported by the client when they connected

    public ServerConnection(final Session session) {
        ThreadLocalRandom.current().nextBytes(this.challenge);
        this.session = (TcpSession) session;
        initSpectatorEntity();
    }

    protected long lastPingId = 0L;
    protected long lastPingTime = 0L;
    protected long ping = 0L;
    protected long lastKeepAliveId = 0L;
    protected long lastKeepAliveTime = 0L;

    protected boolean whitelistChecked = false;
    protected boolean isPlayer = false;
    protected boolean isLoggedIn = false;
    protected boolean isSpectator = false;
    // we have performed the configuration phase at zenith
    // any subsequent configurations should pass through to client
    protected boolean isConfigured = false;
    protected boolean onlySpectator = false;
    protected boolean allowSpectatorServerPlayerPosRotate = true;
    // allow spectator to set their camera to client
    // need to persist state to allow them in and out of this
    protected Entity cameraTarget = null;
    protected boolean showSelfEntity = true;
    protected int spectatorEntityId = 2147483647 - ThreadLocalRandom.current().nextInt(1000000);
    protected int spectatorSelfEntityId = spectatorEntityId - 1;
    protected UUID spectatorEntityUUID = UUID.randomUUID();
    protected ServerProfileCache profileCache = new ServerProfileCache();
    protected ServerProfileCache spectatorFakeProfileCache = new ServerProfileCache();
    protected PlayerCache spectatorPlayerCache = new PlayerCache(new EntityCache());
    protected SpectatorEntity spectatorEntity;

    /**
     * Team data
     */
    protected List<String> currentTeamMembers = new ArrayList<>();
    private static final String teamName = "ZenithProxy";
    private static final Component displayName = Component.text("ZenithProxy");
    private static final Component prefix = Component.text("");
    private static final Component suffix = Component.text("");
    private static final boolean friendlyFire = false;
    private static final boolean seeFriendlyInvisibles = false;

    public KeyPair getKeyPair() {
        return KEY_PAIR;
    }

    public String getServerId() {
        return SERVER_ID;
    }

    public EventLoop getEventLoop() {
        return this.session.getChannel().eventLoop();
    }

    @Override
    public void packetReceived(Session session, Packet packet) {
        try {
            Packet p = packet;
            var state = session.getPacketProtocol().getState(); // storing this before handlers might mutate it on the session
            p = ZenithHandlerCodec.SERVER_REGISTRY.handleInbound(p, this);
            if (p != null && !isSpectator() && (state == ProtocolState.GAME || state == ProtocolState.CONFIGURATION)) {
                if (state == ProtocolState.CONFIGURATION && !isConfigured()) return;
                Proxy.getInstance().getClient().sendAsync(p);
            }
        } catch (final Exception e) {
            SERVER_LOG.error("Failed handling Received packet: {}", packet.getClass().getSimpleName(), e);
        }
    }

    @Override
    public Packet packetSending(final Session session, final Packet packet) {
        try {
            return ZenithHandlerCodec.SERVER_REGISTRY.handleOutgoing(packet, this);
        } catch (final Exception e) {
            SERVER_LOG.error("Failed handling packet sending: {}", packet.getClass().getSimpleName(), e);
        }
        return packet;
    }


    @Override
    public void packetSent(Session session, Packet packet) {
        try {
            ZenithHandlerCodec.SERVER_REGISTRY.handlePostOutgoing(packet, this);
        } catch (final Exception e) {
            SERVER_LOG.error("Failed handling PostOutgoing packet: {}", packet.getClass().getSimpleName(), e);
        }
    }

    @Override
    public boolean packetError(final Session session, final Throwable throwable) {
        SERVER_LOG.debug("", throwable);
        return isLoggedIn;
    }

    @Override
    public void connected(final Session session) {
    }

    @Override
    public void disconnecting(final Session session, final Component reason, final Throwable cause) {

    }

    @Override
    public void disconnected(final Session session, final Component reason, final Throwable cause) {
        Proxy.getInstance().getActiveConnections().remove(this);
        if (!this.isPlayer && cause != null && !(cause instanceof DecoderException || cause instanceof IOException)) {
            // any scanners or TCP connections established result in a lot of these coming in even when they are not actually speaking mc protocol
            SERVER_LOG.warn(String.format("Connection disconnected: %s", session.getRemoteAddress()), cause);
            return;
        }
        if (this.isPlayer) {
            final String reasonStr = ComponentSerializer.serializePlain(reason);

            if (!isSpectator()) {
                SERVER_LOG.info("Player disconnected: UUID: {}, Username: {}, Address: {}, Reason {}",
                                Optional.ofNullable(this.profileCache.getProfile()).map(GameProfile::getId).orElse(null),
                                Optional.ofNullable(this.profileCache.getProfile()).map(GameProfile::getName).orElse(null),
                                session.getRemoteAddress(),
                                reasonStr,
                                cause);
                try {
                    EVENT_BUS.post(new ProxyClientDisconnectedEvent(reasonStr, profileCache.getProfile()));
                } catch (final Throwable e) {
                    SERVER_LOG.info("Could not get game profile of disconnecting player");
                    EVENT_BUS.post(new ProxyClientDisconnectedEvent(reasonStr));
                }
            } else {
                var connections = Proxy.getInstance().getActiveConnections().getArray();
                for (int i = 0; i < connections.length; i++) {
                    var connection = connections[i];
                    connection.send(new ClientboundRemoveEntitiesPacket(new int[]{this.spectatorEntityId}));
                    connection.send(new ClientboundSystemChatPacket(ComponentSerializer.minedown("&9" + profileCache.getProfile().getName() + " disconnected&r"), false));
                }
                EVENT_BUS.postAsync(new ProxySpectatorDisconnectedEvent(profileCache.getProfile()));
            }
        }
        ServerConnection serverConnection = Proxy.getInstance().getCurrentPlayer().get();
        if (serverConnection != null) {
            serverConnection.syncTeamMembers();
        }
    }

    public void setLoggedIn() {
        this.isLoggedIn = true;
        if (!Proxy.getInstance().getActiveConnections().contains(this))
            Proxy.getInstance().getActiveConnections().add(this);
    }

    @Override
    public Future<Void> send(@NonNull Packet packet) {
        return this.session.send(packet);
    }

    public void sendAsyncAlert(final String minedown) {
        // todo: gradient colors?
        this.sendAsync(new ClientboundSystemChatPacket(ComponentSerializer.minedown("&7[&bZenithProxy&7]&r " + minedown), false));
    }

    @Override
    public void send(@NonNull final Packet packet, @NonNull final ChannelFutureListener channelFutureListener) {
        this.session.send(packet, channelFutureListener);
    }

    public Future<Void> sendDirect(Packet packet) {
        return this.session.sendDirect(packet);
    }

    @Override
    public void sendDelayedDirect(@NonNull final Packet packet) {
        this.session.sendDelayedDirect(packet);
    }

    @Override
    public void flush() {
        this.session.flush();
    }

    @Override
    public void sendBundleDirect(@NotNull final @NonNull Packet... packets) {
        this.session.sendBundleDirect(packets);
    }

    @Override
    public void sendBundleDirect(@NonNull final List<Packet> packets) {
        this.session.sendBundleDirect(packets);
    }

    @Override
    public void sendBundle(@NonNull final List<Packet> packets) {
        this.session.sendBundle(packets);
    }

    @Override
    public void sendBundle(@NotNull final @NonNull Packet... packets) {
        this.session.sendBundle(packets);
    }

    @Override
    public void sendAsync(@NonNull final Packet packet) {
        this.session.sendAsync(packet);
    }

    @Override
    public void sendScheduledAsync(@NonNull final Packet packet, final long delay, final TimeUnit unit) {
        this.session.sendScheduledAsync(packet, delay, unit);
    }

    public boolean isActivePlayer() {
        // note: this could be false for the player connection during some points of disconnect
        return Objects.equals(Proxy.getInstance().getCurrentPlayer().get(), this);
    }

    // Spectator helper methods

    public Packet getEntitySpawnPacket() {
        return spectatorEntity.getSpawnPacket(spectatorEntityId, spectatorEntityUUID, spectatorPlayerCache, spectatorFakeProfileCache.getProfile());
    }

    public ClientboundSetEntityDataPacket getSelfEntityMetadataPacket() {
        return new ClientboundSetEntityDataPacket(spectatorEntityId, spectatorEntity.getSelfEntityMetadata(
            profileCache.getProfile(),
            spectatorFakeProfileCache.getProfile(),
            spectatorEntityId));
    }

    public ClientboundSetEntityDataPacket getEntityMetadataPacket() {
        return new ClientboundSetEntityDataPacket(spectatorEntityId, spectatorEntity.getEntityMetadata(
            profileCache.getProfile(),
            spectatorFakeProfileCache.getProfile(),
            spectatorEntityId));
    }

    public Optional<Packet> getSoundPacket() {
        return spectatorEntity.getSoundPacket(spectatorPlayerCache);
    }

    public boolean hasCameraTarget() {
        return cameraTarget != null;
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

    public void initializeTeam() {
        session.send(new ClientboundSetPlayerTeamPacket(
            teamName,
            displayName,
            prefix,
            suffix,
            friendlyFire,
            seeFriendlyInvisibles,
            NameTagVisibility.HIDE_FOR_OTHER_TEAMS,
            CollisionRule.PUSH_OWN_TEAM, // ??? doesn't allow pushing own team members but PUSH_OTHER_TEAMS does ???
            TeamColor.AQUA,
            currentTeamMembers.toArray(new String[0])
        ));
    }

    public synchronized void syncTeamMembers() {
        final List<String> teamMembers = Proxy.getInstance().getSpectatorConnections().stream()
            .map(ServerConnection::getSpectatorEntityUUID)
            .map(UUID::toString)
            .collect(Collectors.toCollection(ArrayList::new));
        if (!teamMembers.isEmpty()) teamMembers.add(profileCache.getProfile().getName());
        final List<String> toRemove = currentTeamMembers.stream()
            .filter(member -> !teamMembers.contains(member))
            .toList();
        final List<String> toAdd = teamMembers.stream()
            .filter(member -> !currentTeamMembers.contains(member))
            .toList();
        if (!toRemove.isEmpty()) {
            sendAsync(new ClientboundSetPlayerTeamPacket(
                teamName,
                TeamAction.REMOVE_PLAYER,
                toRemove.toArray(new String[0])
            ));
        }
        if (!toAdd.isEmpty()) {
            sendAsync(new ClientboundSetPlayerTeamPacket(
                teamName,
                TeamAction.ADD_PLAYER,
                toAdd.toArray(new String[0])
            ));
        }
        this.currentTeamMembers = teamMembers;
        SERVER_LOG.debug("Synced Team members: {} for {}", currentTeamMembers, this.profileCache.getProfile().getName());
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
    public MinecraftProtocol getPacketProtocol() {
        return this.session.getPacketProtocol();
    }

    @Override
    public PacketCodecHelper getCodecHelper() {
        return session.getCodecHelper();
    }

    @Override
    public Map<String, Object> getFlags() {
        return this.session.getFlags();
    }

    @Override
    public boolean hasFlag(Flag<?> flag) {
        return this.session.hasFlag(flag);
    }

    @Override
    public <T> T getFlag(Flag<T> key) {
        return this.session.getFlag(key);
    }

    @Override
    public <T> T getFlag(Flag<T> key, T def) {
        return this.session.getFlag(key, def);
    }

    @Override
    public <T> void setFlag(Flag<T> flag, T value) {
        this.session.setFlag(flag, value);
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
    public void callPacketReceived(Packet packet) {
        this.session.callPacketReceived(packet);
    }

    @Override
    public Packet callPacketSending(final Packet packet) {
        return this.session.callPacketSending(packet);
    }

    @Override
    public void callConnected() {
        this.session.callConnected();
    }

    @Override
    public void callDisconnecting(final Component reason, final Throwable cause) {
        this.session.callDisconnecting(reason, cause);
    }

    @Override
    public void callDisconnected(final Component reason, final Throwable cause) {
        this.session.callDisconnected(reason, cause);
    }

    @Override
    public void callPacketSent(Packet packet) {
        this.session.callPacketSent(packet);
    }

    @Override
    public boolean callPacketError(final Throwable throwable) {
        return this.session.callPacketError(throwable);
    }

    @Override
    public int getCompressionThreshold() {
        return this.session.getCompressionThreshold();
    }

    @Override
    public void setCompressionThreshold(int threshold, int level, boolean validateCompression) {
        this.session.setCompressionThreshold(threshold, level, validateCompression);
    }

    @Override
    public void enableEncryption(SecretKey key) {
        this.session.enableEncryption(key);
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
        disconnect(Component.text(reason));
    }

    @Override
    public void disconnect(String reason, Throwable cause) {
        disconnect(Component.text(reason), cause);
    }

    @Override
    public void disconnect(@Nullable final Component reason) {
        disconnect(reason, null);
    }

    @Override
    public void disconnect(@Nullable final Component reason, final Throwable cause) {
        var disconnectPacket = getDisconnectPacket(reason);
        if (disconnectPacket != null) {
            send(disconnectPacket, future -> this.session.disconnect(reason, cause));
        } else {
            this.session.disconnect(reason, cause);
        }
    }

    private @Nullable Packet getDisconnectPacket(@Nullable final Component reason) {
        if (reason == null) return null;
        MinecraftProtocol protocol = (MinecraftProtocol) session.getPacketProtocol();
        if (protocol.getState() == ProtocolState.LOGIN) {
            return new ClientboundLoginDisconnectPacket(reason);
        } else if (protocol.getState() == ProtocolState.GAME) {
            return new ClientboundDisconnectPacket(reason);
        }
        return null;
    }
}
