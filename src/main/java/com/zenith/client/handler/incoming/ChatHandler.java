package com.zenith.client.handler.incoming;

import com.github.steveice10.mc.protocol.packet.ingame.server.ServerChatPacket;
import com.zenith.client.ClientSession;
import com.zenith.event.proxy.DeathMessageEvent;
import com.zenith.event.proxy.ServerChatReceivedEvent;
import com.zenith.event.proxy.ServerRestartingEvent;
import com.zenith.util.handler.HandlerRegistry;
import lombok.NonNull;
import net.daporkchop.lib.minecraft.text.component.MCTextRoot;
import net.daporkchop.lib.minecraft.text.parser.AutoMCFormatParser;

import java.awt.*;

import static com.zenith.util.Constants.*;
import static java.util.Objects.nonNull;

public class ChatHandler implements HandlerRegistry.AsyncIncomingHandler<ServerChatPacket, ClientSession> {
    @Override
    public boolean applyAsync(@NonNull ServerChatPacket packet, @NonNull ClientSession session) {
        try {
            MCTextRoot mcTextRoot = AutoMCFormatParser.DEFAULT.parse(packet.getMessage());
            final String messageString = mcTextRoot.toRawString();
            CHAT_LOG.info(messageString);
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
                if (mcTextRoot.getChildren().stream().anyMatch(child -> nonNull(child.getColor()) && child.getColor().equals(new Color(170, 0, 0)))
                        // we should find ourselves in the death message
                        && mcTextRoot.getChildren().stream().anyMatch(child -> nonNull(child.getText()) && child.getText().equals(CONFIG.authentication.username))) {
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
