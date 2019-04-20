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
    private final Bot bot;

    @NonNull
    private final PorkClientSession session;

    @Override
    public void packetReceived(PacketReceivedEvent event) {
        if (CLIENT_HANDLERS.handleInbound(event.getPacket(), this.session)) {
            this.bot.getServerConnections().stream()
                    .filter(session -> ((MinecraftProtocol) session.getPacketProtocol()).getSubProtocol() == SubProtocol.GAME)
                    .forEach(c -> c.send(event.getPacket()));
        }
    }

    @Override
    public void packetSending(PacketSendingEvent event) {
        Packet p1 = event.getPacket();
        Packet p2 = CLIENT_HANDLERS.handleOutgoing(p1, this.session);
        if (p2 == null) {
            event.setCancelled(true);
        } else if (p1 != p2)    {
            event.setPacket(p2);
        }
    }

    @Override
    public void packetSent(PacketSentEvent event) {
        CLIENT_HANDLERS.handlePostOutgoing(event.getPacket(), this.session);
    }

    @Override
    public void connected(ConnectedEvent event) {
        logger.debug("Connection complete!");
    }

    @Override
    public void disconnecting(DisconnectingEvent event) {
    }

    @Override
    public void disconnected(DisconnectedEvent event) {
        logger.info("Disconnecting from server. Reason: ${0}", event.getReason());
    }
}
