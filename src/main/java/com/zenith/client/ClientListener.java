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

import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.mc.protocol.MinecraftProtocol;
import com.github.steveice10.mc.protocol.data.SubProtocol;
import com.github.steveice10.mc.protocol.data.game.TitleAction;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerPlayerListDataPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerPlayerListEntryPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerTitlePacket;
import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.event.session.*;
import com.github.steveice10.packetlib.packet.Packet;
import com.zenith.Proxy;
import com.zenith.event.proxy.*;
import com.zenith.server.PorkServerConnection;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.lib.minecraft.text.parser.AutoMCFormatParser;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

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
    protected final PorkClientSession session;

    private boolean inQueue = false;
    private int lastQueuePosition = Integer.MAX_VALUE;
    // in game
    private boolean online = false;
    private boolean disconnected = true;

    @Override
    public void packetReceived(Session session, Packet packet) {
        if (packet instanceof ServerTitlePacket) {
            parse2bQueuePos(((ServerTitlePacket) packet));
        }
        if (packet instanceof ServerPlayerListDataPacket) {
            parse2bQueueState((ServerPlayerListDataPacket) packet);
        }
        try {
            if (CLIENT_HANDLERS.handleInbound(packet, this.session)) {
                PorkServerConnection connection = this.proxy.getCurrentPlayer().get();
                if (connection != null && ((MinecraftProtocol) connection.getPacketProtocol()).getSubProtocol() == SubProtocol.GAME)    {
                    connection.send(packet);
                }
            }
        } catch (RuntimeException e) {
            CLIENT_LOG.alert(e);
            throw e;
        } catch (Exception e) {
            CLIENT_LOG.alert(e);
            throw new RuntimeException(e);
        }
    }

    private void parse2bQueueState(ServerPlayerListDataPacket packet) {
        Optional<String> queueHeader = Arrays.stream(packet.getHeader().split("\\\\n"))
                .map(m -> m.trim())
                .filter(m -> m.contains("2b2t is full") || m.toLowerCase(Locale.ROOT).contains("pending"))
                .findAny();
        if (queueHeader.isPresent()) {
            if (!inQueue) {
                EVENT_BUS.dispatch(new StartQueueEvent());
            }
            inQueue = true;
        } else if (inQueue) {
            inQueue = false;
            lastQueuePosition = Integer.MAX_VALUE;
            EVENT_BUS.dispatch(new QueueCompleteEvent());
        } else if (!online) {
            online = true;
            EVENT_BUS.dispatch(new PlayerOnlineEvent());
        }
    }

    private void parse2bQueuePos(ServerTitlePacket serverTitlePacket) {
        try {
            Optional<Integer> position = Optional.of(serverTitlePacket)
                    .filter(packet -> packet.getAction().equals(TitleAction.SUBTITLE))
                    .map(ServerTitlePacket::getSubtitle)
                    .map(title -> AutoMCFormatParser.DEFAULT.parse(title).toRawString())
                    .map(text -> text.split(":")[1].trim())
                    .map(Integer::parseInt);
            if (position.isPresent()) {
                if (position.get() != lastQueuePosition) {
                    EVENT_BUS.dispatch(new QueuePositionUpdateEvent(position.get()));
                }
                lastQueuePosition = position.get();
            }
        } catch (final Exception e) {
            CLIENT_LOG.warn("Error parsing queue position from title packet", e);
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
        WEBSOCKET_SERVER.fireReset();
        CLIENT_LOG.success("Connected to %s!", event.getSession().getRemoteAddress());
        disconnected = false;
        EVENT_BUS.dispatch(new ConnectEvent());
    }

    @Override
    public void disconnecting(DisconnectingEvent event) {
        WEBSOCKET_SERVER.fireReset();
        CLIENT_LOG.info("Disconnecting from server...")
                  .trace("Disconnect reason: %s", event.getReason());

        PorkServerConnection connection = this.proxy.getCurrentPlayer().get();
        if (connection != null)    {
            connection.disconnect(event.getReason());
        }
    }

    @Override
    public void disconnected(DisconnectedEvent event) {
        WEBSOCKET_SERVER.fireReset();
        CLIENT_LOG.info("Disconnected: " + event.getReason());
        if (!disconnected) {
            disconnected = true;
            EVENT_BUS.dispatch(new DisconnectEvent(AutoMCFormatParser.DEFAULT.parse(event.getReason()).toRawString()));
        }
    }
}
