package com.zenith.server.handler.player.outgoing;

import com.github.steveice10.mc.protocol.packet.ingame.server.ServerChatPacket;
import com.zenith.server.ServerConnection;
import com.zenith.util.handler.HandlerRegistry;
import net.daporkchop.lib.minecraft.text.component.MCTextRoot;
import net.daporkchop.lib.minecraft.text.parser.AutoMCFormatParser;

import java.awt.*;

import static com.zenith.util.Constants.CONFIG;
import static java.util.Objects.nonNull;

public class ServerOutgoingChatHandler implements HandlerRegistry.OutgoingHandler<ServerChatPacket, ServerConnection> {
    @Override
    public ServerChatPacket apply(ServerChatPacket packet, ServerConnection session) {
        if (CONFIG.client.extra.chat.hideChat && packet.getMessage().startsWith("<")) {
            return null;
        } else if (CONFIG.client.extra.chat.hideWhispers && isWhisper(packet.getMessage())) {
            return null;
        } else if (CONFIG.client.extra.chat.hideDeathMessages && isDeathMessage(packet.getMessage())) {
            return null;
        }
        return packet;
    }

    @Override
    public Class<ServerChatPacket> getPacketClass() {
        return ServerChatPacket.class;
    }

    private boolean isWhisper(String message) {
        if (!message.startsWith("<")) {
            String[] split = message.split(" ");
            return split.length > 2 && split[1].startsWith("whispers");
        }
        return false;
    }

    private boolean isDeathMessage(String message) {
        if (!message.startsWith("<")) {
            final MCTextRoot mcTextRoot = AutoMCFormatParser.DEFAULT.parse(message);
            return mcTextRoot.getChildren().stream().anyMatch(child -> nonNull(child.getColor()) && child.getColor().equals(new Color(170, 0, 0)));
        }
        return false;
    }
}
