/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2016-2020 DaPorkchop_
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software
 * is furnished to do so, subject to the following conditions:
 *
 * Any persons and/or organizations using this software must include the above copyright notice and this permission notice,
 * provide sufficient credit to the original authors of the project (IE: DaPorkchop_), as well as provide a link to the original project.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS
 * BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

package com.zenith.server;

import com.github.steveice10.mc.protocol.MinecraftProtocol;
import com.github.steveice10.mc.protocol.data.SubProtocol;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.EntityMetadata;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.MetadataType;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntityDestroyPacket;
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
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.ClosedChannelException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static com.zenith.util.Constants.*;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
@Getter
@Setter
public class PorkServerConnection implements Session, SessionListener {
    @NonNull
    protected final Proxy proxy;

    @NonNull
    protected final Session session;

    protected long lastPacket = System.currentTimeMillis();

    protected boolean isPlayer = false;
    protected boolean isLoggedIn = false;
    protected boolean allowSpectatorServerPlayerPosRotate = true;
    protected int spectatorEntityId = 2147483647 - this.hashCode();
    protected int spectatorSelfCatEntityId = spectatorEntityId - 1;
    protected UUID spectatorCatUUID = UUID.randomUUID();
    protected ServerProfileCache profileCache = new ServerProfileCache();
    protected PlayerCache spectatorPlayerCache = new PlayerCache(new EntityCache());

    public final EntityMetadata[] getSpectatorCatEntityMetadata(boolean self) {
        // https://c4k3.github.io/wiki.vg/Entities.html#Entity
        return new EntityMetadata[]{
                new EntityMetadata(0, MetadataType.BYTE, (byte)0),
                new EntityMetadata(1, MetadataType.INT, 0),
                new EntityMetadata(2, MetadataType.STRING, this.getProfileCache().getProfile().getName()),
                new EntityMetadata(3, MetadataType.BOOLEAN, !self), // hide nametag on self
                new EntityMetadata(4, MetadataType.BOOLEAN, false),
                new EntityMetadata(5, MetadataType.BOOLEAN, false),
                new EntityMetadata(6, MetadataType.BYTE, (byte)0),
                new EntityMetadata(7, MetadataType.FLOAT, 10.0f),
                new EntityMetadata(8, MetadataType.INT, 0),
                new EntityMetadata(9, MetadataType.BOOLEAN, false),
                new EntityMetadata(10, MetadataType.INT, 0),
                new EntityMetadata(11, MetadataType.BYTE, (byte)0),
                new EntityMetadata(12, MetadataType.BOOLEAN, false),
                new EntityMetadata(13, MetadataType.BYTE, (byte)4),
//                new EntityMetadata(14, MetadataType.OPTIONAL_UUID, this.getProfileCache().getProfile().getId()), // mob owner
                new EntityMetadata(15, MetadataType.INT, (spectatorEntityId % 3) + 1) // cat texture variant
        };
    }

    @Override
    public void packetReceived(Session session, Packet packet) {
        if (isActivePlayer()) {
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
    }

    @Override
    public void packetSending(PacketSendingEvent event) {
        Packet p1 = event.getPacket();
        Packet p2;
        if (isActivePlayer()) {
            p2 = SERVER_PLAYER_HANDLERS.handleOutgoing(p1, this);
        } else {
            p2 = SERVER_SPECTATOR_HANDLERS.handleOutgoing(p1, this);
        }
        if (p2 == null) {
            event.setCancelled(true);
        } else if (p1 != p2) {
            event.setPacket(p2);
        }
    }

    @Override
    public void packetSent(Session session, Packet packet) {
        if (isActivePlayer()) {
            SERVER_PLAYER_HANDLERS.handlePostOutgoing(packet, this);
        } else {
            SERVER_SPECTATOR_HANDLERS.handlePostOutgoing(packet, this);
        }
    }

    @Override
    public void packetError(PacketErrorEvent event) {
        SERVER_LOG.error(event.getCause());
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
            if (isActivePlayer()) {
                SERVER_LOG.info("Player disconnected: %s", event.getSession().getRemoteAddress());
                try {
                    EVENT_BUS.dispatch(new ProxyClientDisconnectedEvent(event.getReason(), profileCache.getProfile()));
                } catch (final Throwable e) {
                    SERVER_LOG.info("Could not get game profile of disconnecting player");
                    EVENT_BUS.dispatch(new ProxyClientDisconnectedEvent(event.getReason()));
                }
            } else {
                proxy.getServerConnections().forEach(connection -> {
                    connection.send(new ServerEntityDestroyPacket(this.spectatorEntityId));
                });
                EVENT_BUS.dispatch(new ProxySpectatorDisconnectedEvent(profileCache.getProfile()));
            }
        }
    }

    public void send(@NonNull Packet packet) {
        this.session.send(packet);
    }

    public boolean isActivePlayer() {
        return Objects.equals(this.proxy.getCurrentPlayer().get(), this);
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
