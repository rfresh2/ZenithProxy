package com.zenith.network.server.handler.player.outgoing;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundSystemChatPacket;
import com.zenith.network.registry.OutgoingHandler;
import com.zenith.network.server.ServerConnection;
import com.zenith.util.ComponentSerializer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

import java.util.Objects;

import static com.zenith.Shared.*;
import static java.util.Objects.nonNull;

// todo: also handle regular player chat packets although they should be disabled on 2b2t
public class SystemChatOutgoingHandler implements OutgoingHandler<ClientboundSystemChatPacket, ServerConnection> {
    @Override
    public ClientboundSystemChatPacket apply(ClientboundSystemChatPacket packet, ServerConnection session) {
        try {
            final Component component = packet.getContent();
            final String message = ComponentSerializer.toRawString(component);
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
            } else if (CONFIG.client.extra.chat.hideDeathMessages && isDeathMessage(component, message)) {
                return null;
            }
        } catch (final Exception e) {
            SERVER_LOG.error("Failed to parse chat message in ServerChatOutgoingHandler: {}", ComponentSerializer.toRawString(packet.getContent()), e);
        }
        return packet;
    }

    private boolean isWhisper(String message) {
        if (!message.startsWith("<")) {
            String[] split = message.split(" ");
            return split.length > 2 && split[1].startsWith("whispers");
        }
        return false;
    }

    private boolean isDeathMessage(final Component component, final String messageRaw) {
        if (!messageRaw.startsWith("<")) {
            return component.children().stream().anyMatch(child -> nonNull(child.color())
                && Objects.equals(child.color(), TextColor.color(170, 0, 0)));
        }
        return false;
    }
}
