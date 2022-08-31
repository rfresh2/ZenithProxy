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

package com.zenith.client;

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

import static com.zenith.util.Constants.*;

/**
 * @author DaPorkchop_
 */
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
                this.proxy.getServerConnections().forEach(connection -> connection.send(packet));
            }
        } catch (Exception e) {
            CLIENT_LOG.alert(e);
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
            CLIENT_LOG.alert(e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void packetSent(Session session, Packet packet) {
        try {
            CLIENT_HANDLERS.handlePostOutgoing(packet, this.session);
        } catch (Exception e) {
            CLIENT_LOG.alert(e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void packetError(PacketErrorEvent event) {
        CLIENT_LOG.error(event.getCause());
    }

    @Override
    public void connected(ConnectedEvent event) {
        CLIENT_LOG.success("Connected to %s!", event.getSession().getRemoteAddress());
        session.setDisconnected(false);
        EVENT_BUS.dispatch(new ConnectEvent());
    }

    @Override
    public void disconnecting(DisconnectingEvent event) {
        try {
            CLIENT_LOG.info("Disconnecting from server...")
                    .trace("Disconnect reason: %s", event.getReason());
            // reason can be malformed for MC parser the logger uses
        } catch (final Exception e) {
            // fall through
        }
        this.proxy.getServerConnections().forEach(connection -> connection.disconnect(event.getReason()));
    }

    @Override
    public void disconnected(DisconnectedEvent event) {
        CLIENT_LOG.info("Disconnected: " + event.getReason());
        if (!session.isDisconnected()) {
            session.setDisconnected(true);
            EVENT_BUS.dispatch(new DisconnectEvent(AutoMCFormatParser.DEFAULT.parse(event.getReason()).toRawString()));
        }
    }
}
