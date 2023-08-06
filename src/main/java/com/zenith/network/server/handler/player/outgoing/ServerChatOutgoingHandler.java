package com.zenith.network.server.handler.player.outgoing;

import com.github.steveice10.mc.protocol.packet.ingame.server.ServerChatPacket;
import com.zenith.network.registry.OutgoingHandler;
import com.zenith.network.server.ServerConnection;
import net.daporkchop.lib.minecraft.text.component.MCTextRoot;
import net.daporkchop.lib.minecraft.text.parser.AutoMCFormatParser;

import java.awt.*;

import static com.zenith.Shared.*;
import static java.util.Objects.nonNull;

public class ServerChatOutgoingHandler implements OutgoingHandler<ServerChatPacket, ServerConnection> {
    @Override
    public ServerChatPacket apply(ServerChatPacket packet, ServerConnection session) {
        try {
            final MCTextRoot mcTextRoot = AutoMCFormatParser.DEFAULT.parse(packet.getMessage());
            final String message = mcTextRoot.toRawString();
            if (message.startsWith("<")) {
                if (CONFIG.client.extra.chat.hideChat) {
                    return null;
                } else if (WHITELIST_MANAGER.isPlayerIgnored(message.substring(message.indexOf("<") + 1, message.indexOf(">")))) {
                    return null;
                }
            }
            if (CONFIG.client.extra.chat.hideChat && message.startsWith("<")) {
                return null;
            } else if (isWhisper(message)) {
                if (CONFIG.client.extra.chat.hideWhispers) {
                    return null;
                } else if (WHITELIST_MANAGER.isPlayerIgnored(message.substring(0, message.indexOf(" ")))) {
                    return null;
                }
            } else if (CONFIG.client.extra.chat.hideDeathMessages && isDeathMessage(mcTextRoot, message)) {
                return null;
            }
        } catch (final Exception e) {
            SERVER_LOG.error("Failed to parse chat message in ServerChatOutgoingHandler: {}", packet.getMessage(), e);
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

    private boolean isDeathMessage(final MCTextRoot mcTextRoot, final String messageRaw) {
        if (!messageRaw.startsWith("<")) {
            return mcTextRoot.getChildren().stream().anyMatch(child -> nonNull(child.getColor()) && child.getColor().equals(new Color(170, 0, 0)));
        }
        return false;
    }
}
