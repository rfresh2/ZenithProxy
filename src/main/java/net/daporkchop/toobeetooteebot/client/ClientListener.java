/*
 * Adapted from the Wizardry License
 *
 * Copyright (c) 2016-2019 DaPorkchop_
 *
 * Permission is hereby granted to any persons and/or organizations using this software to copy, modify, merge, publish, and distribute it.
 * Said persons and/or organizations are not allowed to use the software or any derivatives of the work for commercial use or any other means to generate income, nor are they allowed to claim this software as their own.
 *
 * The persons and/or organizations are also disallowed from sub-licensing and/or trademarking this software without explicit permission from DaPorkchop_.
 *
 * Any persons and/or organizations using this software must disclose their source code and have it publicly available, include this license, provide sufficient credit to the original authors of the project (IE: DaPorkchop_), as well as provide a link to the original project.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON INFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

package net.daporkchop.toobeetooteebot.client;

import com.github.steveice10.mc.protocol.MinecraftProtocol;
import com.github.steveice10.mc.protocol.data.SubProtocol;
import com.github.steveice10.packetlib.event.session.ConnectedEvent;
import com.github.steveice10.packetlib.event.session.DisconnectedEvent;
import com.github.steveice10.packetlib.event.session.DisconnectingEvent;
import com.github.steveice10.packetlib.event.session.PacketReceivedEvent;
import com.github.steveice10.packetlib.event.session.PacketSendingEvent;
import com.github.steveice10.packetlib.event.session.PacketSentEvent;
import com.github.steveice10.packetlib.event.session.SessionListener;
import com.github.steveice10.packetlib.packet.Packet;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.toobeetooteebot.Bot;
import net.daporkchop.toobeetooteebot.util.Constants;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
@Getter
public class ClientListener implements SessionListener, Constants {
    @NonNull
    protected final Bot bot;

    @NonNull
    protected final PorkClientSession session;

    @Override
    public void packetReceived(PacketReceivedEvent event) {
        try {
            if (CLIENT_HANDLERS.handleInbound(event.getPacket(), this.session)) {
                this.bot.getServerConnections()
                        .stream()
                        .filter(session -> ((MinecraftProtocol) session.getPacketProtocol()).getSubProtocol() == SubProtocol.GAME)
                        .forEach(c -> c.send(event.getPacket()));
            }
        } catch (RuntimeException e) {
            CLIENT_LOG.alert(e);
            throw e;
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
    public void packetSent(PacketSentEvent event) {
        try {
            CLIENT_HANDLERS.handlePostOutgoing(event.getPacket(), this.session);
        } catch (Exception e) {
            CLIENT_LOG.alert(e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void connected(ConnectedEvent event) {
        CLIENT_LOG.success("Connected to %s!", event.getSession().getRemoteAddress());
    }

    @Override
    public void disconnecting(DisconnectingEvent event) {
        CLIENT_LOG.info("Disconnecting from server...")
                  .trace("Disconnect reason: %s", event.getReason());

        Bot.getInstance().getServerConnections().forEach(c -> c.disconnect(event.getReason()));
    }

    @Override
    public void disconnected(DisconnectedEvent event) {
        CLIENT_LOG.info("Disconnected.");
    }
}
