package com.zenith.network.server;

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
import io.netty.channel.EventLoop;
import io.netty.handler.codec.DecoderException;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import org.geysermc.mcprotocollib.auth.GameProfile;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.network.tcp.TcpServer;
import org.geysermc.mcprotocollib.network.tcp.TcpServerSession;
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
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.zenith.Shared.*;

@Getter
@Setter
public class ServerSession extends TcpServerSession {

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
    // cancel outbound packets until we have received the protocol switch ack
    protected boolean awaitingProtocolSwitch = false;
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

    public ServerSession(final String host, final int port, final MinecraftProtocol protocol, final TcpServer server) {
        super(host, port, protocol, server);
        ThreadLocalRandom.current().nextBytes(this.challenge);
        initSpectatorEntity();
    }

    public EventLoop getEventLoop() {
        return getChannel().eventLoop();
    }

    @Override
    public void callPacketReceived(Packet packet) {
        try {
            Packet p = packet;
            var state = getPacketProtocol().getState(); // storing this before handlers might mutate it on the session
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
    public Packet callPacketSending(Packet packet) {
        if (CONFIG.debug.blockProtocolSwitchRaceCondition && blockProtocolSwitchPacketSendingRaceCondition(packet)) return null;
        try {
            return ZenithHandlerCodec.SERVER_REGISTRY.handleOutgoing(packet, this);
        } catch (final Exception e) {
            SERVER_LOG.error("Failed handling packet sending: {}", packet.getClass().getSimpleName(), e);
        }
        return packet;
    }

    private boolean blockProtocolSwitchPacketSendingRaceCondition(Packet packet) {
        if (awaitingProtocolSwitch) {
            /**
             * Problem: race conditions during GAME -> CONFIGURATION -> GAME from velocity server switches
             *
             * MCPL has a single variable for the protocol state
             * The state is not switched until we receive configuration ACK back from the client
             * in-between the time the server sends a start configuration until we receive the ack we can still
             * send GAME packets which will cause the client to disconnect
             *
             * Its possible there's race conditions in other protocol switches as well
             */

            // re-queue packet onto event loop
            // if the packet does not match the dest protocol state it will be cancelled by the packet error handler eventually
            sendAsync(packet);
            // other options:
            //  blackhole the packet
            //  introduce a packet queue to preserve order
            return true;
        }
        return false;
    }

    @Override
    public void callPacketSent(Packet packet) {
        try {
            ZenithHandlerCodec.SERVER_REGISTRY.handlePostOutgoing(packet, this);
        } catch (final Exception e) {
            SERVER_LOG.error("Failed handling PostOutgoing packet: {}", packet.getClass().getSimpleName(), e);
        }
    }

    @Override
    public boolean callPacketError(Throwable throwable) {
        SERVER_LOG.debug("", throwable);
        return isLoggedIn;
    }

    @Override
    public void callConnected() {

    }

    @Override
    public void callDisconnecting(Component reason, Throwable cause) {

    }

    @Override
    public void callDisconnected(Component reason, Throwable cause) {
        Proxy.getInstance().getActiveConnections().remove(this);
        if (!this.isPlayer && cause != null && !(cause instanceof DecoderException || cause instanceof IOException)) {
            // any scanners or TCP connections established result in a lot of these coming in even when they are not actually speaking mc protocol
            SERVER_LOG.warn(String.format("Connection disconnected: %s", getRemoteAddress()), cause);
            return;
        }
        if (this.isPlayer) {
            final String reasonStr = ComponentSerializer.serializePlain(reason);

            if (!isSpectator()) {
                SERVER_LOG.info("Player disconnected: UUID: {}, Username: {}, Address: {}, Reason {}",
                                Optional.ofNullable(this.profileCache.getProfile()).map(GameProfile::getId).orElse(null),
                                Optional.ofNullable(this.profileCache.getProfile()).map(GameProfile::getName).orElse(null),
                                getRemoteAddress(),
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
        ServerSession serverConnection = Proxy.getInstance().getCurrentPlayer().get();
        if (serverConnection != null) {
            serverConnection.syncTeamMembers();
        }
    }

    @Override
    public void disconnect(@Nullable final Component reason, final Throwable cause) {
        if (this.getChannel() != null && this.getChannel().isActive()) {
            var disconnectPacket = getDisconnectPacket(reason);
            if (disconnectPacket != null) {
                try {
                    send(disconnectPacket).get(1L, TimeUnit.SECONDS);
                } catch (final Exception e) {
                    // fall through
                }
            }
        }
        super.disconnect(reason, cause);
    }

    private @Nullable Packet getDisconnectPacket(@Nullable final Component reason) {
        if (reason == null) return null;
        MinecraftProtocol protocol = getPacketProtocol();
        if (protocol.getState() == ProtocolState.LOGIN) {
            return new ClientboundLoginDisconnectPacket(reason);
        } else if (protocol.getState() == ProtocolState.GAME) {
            return new ClientboundDisconnectPacket(reason);
        }
        return null;
    }

    public void setLoggedIn() {
        this.isLoggedIn = true;
        if (!Proxy.getInstance().getActiveConnections().contains(this))
            Proxy.getInstance().getActiveConnections().add(this);
    }

    public void sendAsyncAlert(final String minedown) {
        // todo: gradient colors?
        this.sendAsync(new ClientboundSystemChatPacket(ComponentSerializer.minedown("&7[&bZenithProxy&7]&r " + minedown), false));
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
        send(new ClientboundSetPlayerTeamPacket(
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
            .map(ServerSession::getSpectatorEntityUUID)
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
}
