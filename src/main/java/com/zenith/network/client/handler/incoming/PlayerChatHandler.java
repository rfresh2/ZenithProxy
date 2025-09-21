package com.zenith.network.client.handler.incoming;

import com.zenith.event.chat.PublicChatEvent;
import com.zenith.event.chat.WhisperChatEvent;
import com.zenith.feature.chatschema.ChatSchemaParser;
import com.zenith.mc.chat_type.ChatTypeRegistry;
import com.zenith.network.client.ClientSession;
import com.zenith.network.codec.PacketHandler;
import com.zenith.util.ComponentSerializer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.geysermc.mcprotocollib.protocol.data.game.PlayerListEntry;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundPlayerChatPacket;

import java.util.Optional;

import static com.zenith.Globals.*;
import static java.util.Objects.requireNonNullElse;

public class PlayerChatHandler implements PacketHandler<ClientboundPlayerChatPacket, ClientSession> {

    @Override
    public ClientboundPlayerChatPacket apply(ClientboundPlayerChatPacket packet, ClientSession session) {
        var senderPlayerEntry = CACHE.getTabListCache().get(packet.getSender());
        var chatType = ChatTypeRegistry.REGISTRY.get(packet.getChatType().id());
        if (chatType != null) {
            TextComponent packetContentComponent = Component.text(packet.getContent());
            Component chatComponent = chatType.render(
                packet.getName(),
                packetContentComponent,
                packet.getUnsignedContent(),
                packet.getTargetName());
            if (CONFIG.client.extra.logChatMessages) {
                CHAT_LOG.info(chatComponent);
            }
            String messageContent = ComponentSerializer.serializePlain(requireNonNullElse(packet.getUnsignedContent(), packetContentComponent));
            boolean isWhisper = false;
            boolean outboundWhisper = false;
            Optional<PlayerListEntry> whisperTarget = Optional.empty();
            if ("commands.message.display.incoming".equals(chatType.translationKey())) {
                isWhisper = true;
                whisperTarget = CACHE.getTabListCache().get(CACHE.getProfileCache().getProfile().getId());
            } else if ("commands.message.display.outgoing".equals(chatType.translationKey())) {
                isWhisper = true;
                outboundWhisper = true;
                whisperTarget = CACHE.getTabListCache().getFromName( // ???
                     ComponentSerializer.serializePlain(packet.getTargetName())
                );
            } else if ("%s".equals(chatType.translationKey())) {
                var schemaParseResult = ChatSchemaParser.parse(messageContent);
                if (schemaParseResult != null) {
                    switch (schemaParseResult.type()) {
                        case PUBLIC_CHAT -> {
                            isWhisper = false;
                            messageContent = schemaParseResult.messageContent();
                        }
                        case WHISPER_INBOUND -> {
                            isWhisper = true;
                            whisperTarget = Optional.ofNullable(schemaParseResult.sender());
                        }
                        case WHISPER_OUTBOUND -> {
                            isWhisper = true;
                            outboundWhisper = true;
                            whisperTarget = Optional.ofNullable(schemaParseResult.receiver());
                        }
                    }
                }
            }
            if (isWhisper) {
                if (senderPlayerEntry.isEmpty()) {
                    CLIENT_LOG.warn("No sender found for PlayerChatPacket whisper. chatType: {}, content: {}", chatType.translationKey(), ComponentSerializer.serializePlain(chatComponent));
                } else if (whisperTarget.isEmpty()) {
                    CLIENT_LOG.warn("No whisper target found for PlayerChatPacket whisper. chatType: {}, content: {}", chatType.translationKey(), ComponentSerializer.serializePlain(chatComponent));
                } else {
                    EVENT_BUS.postAsync(new WhisperChatEvent(
                        outboundWhisper,
                        senderPlayerEntry.get(),
                        whisperTarget.get(),
                        chatComponent,
                        messageContent));
                }
            } else {
                if (senderPlayerEntry.isEmpty()) {
                    CLIENT_LOG.warn("No sender found for PlayerChatPacket public chat. chatType: {}, content: {}", chatType.translationKey(), ComponentSerializer.serializePlain(chatComponent));
                } else {
                    EVENT_BUS.postAsync(new PublicChatEvent(
                        senderPlayerEntry.get(),
                        chatComponent,
                        messageContent
                    ));
                }
            }
        } else {
            CLIENT_LOG.warn("Unknown chat type: {}", packet.getChatType().id());
        }
        return packet;
    }
}
