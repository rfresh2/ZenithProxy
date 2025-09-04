package com.zenith.feature.extrachat;

import com.zenith.mc.chat_type.ChatTypeRegistry;
import com.zenith.module.impl.ExtraChat;
import com.zenith.network.codec.PacketHandler;
import com.zenith.network.server.ServerSession;
import net.kyori.adventure.text.Component;
import org.geysermc.mcprotocollib.protocol.data.game.chat.ChatFilterType;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundPlayerChatPacket;

import static com.zenith.Globals.*;

public class ECPlayerChatOutgoingHandler implements PacketHandler<ClientboundPlayerChatPacket, ServerSession> {

    @Override
    public ClientboundPlayerChatPacket apply(final ClientboundPlayerChatPacket packet, final ServerSession session) {
        var chatType = ChatTypeRegistry.REGISTRY.get(packet.getChatType().id());
        if (chatType != null) {
            boolean isWhisper = "commands.message.display.incoming".equals(chatType.translationKey()) || "commands.message.display.outgoing".equals(chatType.translationKey());
            if (CONFIG.client.extra.chat.hideWhispers && isWhisper) {
                packet.setFilterMask(ChatFilterType.FULLY_FILTERED);
            } else if (CONFIG.client.extra.chat.hideChat && !isWhisper) {
                packet.setFilterMask(ChatFilterType.FULLY_FILTERED);
            }
        } else {
            SERVER_LOG.warn("Unknown chat type: {}", packet.getChatType().id());
        }
        if (CONFIG.client.extra.chat.insertClickableLinks && packet.getUnsignedContent() == null) {
            var content = Component.text(packet.getContent());
            var linkify = MODULE.get(ExtraChat.class).insertClickableLinks(content);
            return packet.withUnsignedContent(linkify);
        }
        return packet;
    }
}
