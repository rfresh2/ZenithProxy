package com.zenith.network.client.handler.incoming;

import com.zenith.event.chat.PublicChatEvent;
import com.zenith.event.chat.WhisperChatEvent;
import com.zenith.mc.chat_type.ChatType;
import com.zenith.mc.chat_type.ChatTypeRegistry;
import com.zenith.network.client.ClientSession;
import com.zenith.network.codec.PacketHandler;
import com.zenith.util.ComponentSerializer;
import net.kyori.adventure.text.Component;
import org.geysermc.mcprotocollib.protocol.data.game.PlayerListEntry;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundDisguisedChatPacket;

import java.util.Optional;

import static com.zenith.Globals.*;

public class DisguisedChatHandler implements PacketHandler<ClientboundDisguisedChatPacket, ClientSession> {

    @Override
    public ClientboundDisguisedChatPacket apply(final ClientboundDisguisedChatPacket packet, final ClientSession session) {
        var username = ComponentSerializer.serializePlain(packet.getName());
        var senderPlayerEntry = CACHE.getTabListCache().getFromName(username)
            .or(() -> CACHE.getTabListCache().getRecentlyRemovedPlayer(username));
        ChatType chatType = ChatTypeRegistry.REGISTRY.get(packet.getChatType().id());
        if (chatType != null) {
            Component chatComponent = chatType.render(
                packet.getName(),
                packet.getMessage(),
                null,
                packet.getTargetName());
            if (CONFIG.client.extra.logChatMessages) {
                CHAT_LOG.info(chatComponent);
            }
            String messageContent = ComponentSerializer.serializePlain(packet.getMessage());
            boolean isWhisper = false;
            Optional<PlayerListEntry> whisperTarget = Optional.empty();
            if ("commands.message.display.incoming".equals(chatType.translationKey())) {
                isWhisper = true;
                whisperTarget = CACHE.getTabListCache().get(CACHE.getProfileCache().getProfile().getId());
            } else if ("commands.message.display.outgoing".equals(chatType.translationKey())) {
                isWhisper = true;
                var targetName = ComponentSerializer.serializePlain(packet.getTargetName());
                whisperTarget = CACHE.getTabListCache().getFromName(targetName)
                    .or(() -> CACHE.getTabListCache().getRecentlyRemovedPlayer(targetName));
            }
            if (isWhisper) {
                if (senderPlayerEntry.isEmpty()) {
                    CLIENT_LOG.warn("No sender found for PlayerChatPacket whisper. chatType: {}, content: {}", chatType.translationKey(), ComponentSerializer.serializePlain(chatComponent));
                } else if (whisperTarget.isEmpty()) {
                    CLIENT_LOG.warn("No whisper target found for PlayerChatPacket whisper. chatType: {}, content: {}", chatType.translationKey(), ComponentSerializer.serializePlain(chatComponent));
                } else {
                    boolean outgoing = "commands.message.display.outgoing".equals(chatType.translationKey());
                    EVENT_BUS.postAsync(new WhisperChatEvent(
                        outgoing,
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
