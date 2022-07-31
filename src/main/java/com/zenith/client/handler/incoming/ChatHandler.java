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

package com.zenith.client.handler.incoming;

import com.github.steveice10.mc.protocol.packet.ingame.server.ServerChatPacket;
import com.zenith.event.proxy.DeathMessageEvent;
import com.zenith.event.proxy.ServerChatReceivedEvent;
import com.zenith.event.proxy.ServerRestartingEvent;
import lombok.NonNull;
import net.daporkchop.lib.minecraft.text.component.MCTextRoot;
import net.daporkchop.lib.minecraft.text.parser.AutoMCFormatParser;
import com.zenith.client.PorkClientSession;
import com.zenith.util.handler.HandlerRegistry;

import java.awt.*;

import static com.zenith.util.Constants.*;

/**
 * @author DaPorkchop_
 */
public class ChatHandler implements HandlerRegistry.AsyncIncomingHandler<ServerChatPacket, PorkClientSession> {
    @Override
    public boolean applyAsync(@NonNull ServerChatPacket packet, @NonNull PorkClientSession session) {
        try {
            CHAT_LOG.info(packet.getMessage());
            MCTextRoot mcTextRoot = AutoMCFormatParser.DEFAULT.parse(packet.getMessage());
            final String messageString = mcTextRoot.toRawString();
            /*
             * example death message:
             * {"extra":[{"text":""},{"color":"dark_aqua","text":""},
             * {"color":"dark_aqua","clickEvent":{"action":"suggest_command","value":"/w DCI5135 "},
             * "hoverEvent":{"action":"show_text","value":[{"text":""},
             * {"color":"gold","text":"Message "},{"color":"dark_aqua","text":""},
             * {"color":"dark_aqua","text":"DCI5135"},{"color":"dark_aqua","text":""}]},"text":"DCI5135"},
             * {"color":"dark_aqua","text":" "},
             * {"color":"dark_red","text":"died inside lava somehow."}],"text":""}
             */
            if (!messageString.startsWith("<")) { // normal chat msg
                // death message color on 2b
                if (mcTextRoot.getChildren().stream().anyMatch(child -> child.getColor().equals(new Color(170, 0, 0))) //throws a nullpointer exception for queue messages, will fix later
                        // we should find ourselves in the death message
                        && mcTextRoot.getChildren().stream().anyMatch(child -> child.getText().equals(CONFIG.authentication.username))) {
                    // todo: known oversight: also detects when we kill someone else
                    // probable death message
                    EVENT_BUS.dispatch(new DeathMessageEvent(messageString));
                } else if (messageString.startsWith(("[SERVER]"))) { // server message
                    System.out.println(messageString);
                    if (messageString.startsWith("[SERVER] Server restarting in 15 minutes...")) { // todo: include time till restart in event
                        EVENT_BUS.dispatch(new ServerRestartingEvent());
                    }
                }
            }


            if ("2b2t.org".equals(CONFIG.client.server.address)
                    && mcTextRoot.toRawString().toLowerCase().startsWith("Exception Connecting:".toLowerCase()))    {
                CLIENT_LOG.error("2b2t's queue is broken as per usual, disconnecting to avoid being stuck forever");
                session.disconnect("heck");
            }
            WEBSOCKET_SERVER.fireChat(packet.getMessage());
            EVENT_BUS.dispatch(new ServerChatReceivedEvent(messageString));
        } catch (final Exception e) {
            CLIENT_LOG.error("Caught exception in ChatHandler. Packet: " + packet, e);
        }
        return true;
    }

    @Override
    public Class<ServerChatPacket> getPacketClass() {
        return ServerChatPacket.class;
    }
}
