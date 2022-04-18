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
import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.event.session.*;
import com.github.steveice10.packetlib.packet.Packet;
import com.github.steveice10.packetlib.packet.PacketProtocol;
import com.zenith.Proxy;
import com.zenith.event.proxy.ProxyClientDisconnectedEvent;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.ClosedChannelException;
import java.util.List;
import java.util.Map;

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

    @Override
    public void packetReceived(PacketReceivedEvent event) {
        this.lastPacket = System.currentTimeMillis();
        if (SERVER_HANDLERS.handleInbound(event.getPacket(), this) && ((MinecraftProtocol) this.session.getPacketProtocol()).getSubProtocol() == SubProtocol.GAME && this.isLoggedIn) {
            this.proxy.getClient().getSession().send(event.getPacket()); //TODO: handle multi-client correctly (i.e. only allow one client to send packets at a time)
        }
    }

    @Override
    public void packetSending(PacketSendingEvent event) {
        Packet p1 = event.getPacket();
        Packet p2 = SERVER_HANDLERS.handleOutgoing(p1, this);
        if (p2 == null) {
            event.setCancelled(true);
        } else if (p1 != p2) {
            event.setPacket(p2);
        }
    }

    @Override
    public void packetSent(PacketSentEvent event) {
        SERVER_HANDLERS.handlePostOutgoing(event.getPacket(), this);
    }

    @Override
    public void connected(ConnectedEvent event) {
    }

    @Override
    public void disconnecting(DisconnectingEvent event) {
    }

    @Override
    public void disconnected(DisconnectedEvent event) {
        if (event.getCause() != null && !((event.getCause() instanceof IOException || event.getCause() instanceof ClosedChannelException) && !this.isPlayer))   {
            SERVER_LOG.alert(String.format("Player disconnected: %s", event.getSession().getRemoteAddress()), event.getCause());
            EVENT_BUS.dispatch(new ProxyClientDisconnectedEvent());
        } else if (this.isPlayer) {
            SERVER_LOG.info("Player disconnected: %s", event.getSession().getRemoteAddress());
            EVENT_BUS.dispatch(new ProxyClientDisconnectedEvent());
        }
    }

    public void send(@NonNull Packet packet) {
        this.session.send(packet);
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
    public int getCompressionThreshold() {
        return this.session.getCompressionThreshold();
    }

    @Override
    public void setCompressionThreshold(int threshold) {
        this.session.setCompressionThreshold(threshold);
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
