package com.zenith.network.client.handler.incoming;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundSystemChatPacket;
import com.zenith.Proxy;
import com.zenith.event.proxy.DeathMessageEvent;
import com.zenith.event.proxy.SelfDeathMessageEvent;
import com.zenith.event.proxy.ServerChatReceivedEvent;
import com.zenith.feature.deathmessages.DeathMessageParseResult;
import com.zenith.feature.deathmessages.DeathMessagesParser;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.AsyncPacketHandler;
import com.zenith.util.ComponentSerializer;
import lombok.NonNull;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

import java.util.Objects;
import java.util.Optional;

import static com.zenith.Shared.*;
import static java.util.Objects.nonNull;

public class SystemChatHandler implements AsyncPacketHandler<ClientboundSystemChatPacket, ClientSession> {
    private final DeathMessagesParser deathMessagesHelper = new DeathMessagesParser();

    @Override
    public boolean applyAsync(@NonNull ClientboundSystemChatPacket packet, @NonNull ClientSession session) {
        try {
            String serializedChat = ComponentSerializer.serializeJson(packet.getContent());
            if (Proxy.getInstance().isInQueue()) serializedChat = serializedChat.replace("\\n\\n", "");
            CHAT_LOG.info(serializedChat);
            final Component component = packet.getContent();
            final String messageString = ComponentSerializer.serializePlain(component);
            Optional<DeathMessageParseResult> deathMessage = Optional.empty();
            if (!messageString.startsWith("<")) { // normal chat msg
                // death message color on 2b
                if (component.children().stream().anyMatch(child -> nonNull(child.color())
                    && Objects.equals(child.color(), TextColor.color(170, 0, 0)))) {
                    deathMessage = deathMessagesHelper.parse(messageString, true);
                    if (deathMessage.isPresent()) {
                        EVENT_BUS.postAsync(new DeathMessageEvent(deathMessage.get(), messageString));
                        if (deathMessage.get().getVictim().equals(CACHE.getProfileCache().getProfile().getName())) {
                            EVENT_BUS.postAsync(new SelfDeathMessageEvent(messageString));
                        }
                    } else {
                        CLIENT_LOG.warn("Failed to parse death message: {}", messageString);
                    }
                }
            }

            String senderName = null;
            String whisperTarget = null;
            if (messageString.startsWith("<")) {
                senderName = extractSenderNameNormalChat(messageString);
            } else if (deathMessage.isEmpty()) {
                final String[] split = messageString.split(" ");
                if (split.length > 2) {
                    if (split[1].startsWith("whispers")) {
                        senderName = extractSenderNameReceivedWhisper(split);
                        whisperTarget = CONFIG.authentication.username;
                    } else if (messageString.startsWith("to ")) {
                        senderName = CONFIG.authentication.username;
                        whisperTarget = extractReceiverNameSentWhisper(split);
                    }
                }
            }
            EVENT_BUS.postAsync(new ServerChatReceivedEvent(Optional.ofNullable(senderName).flatMap(t -> CACHE.getTabListCache().getFromName(t)),
                                                            messageString,
                                                            Optional.ofNullable(whisperTarget).flatMap(t -> CACHE.getTabListCache().getFromName(t)),
                                                            deathMessage));
        } catch (final Exception e) {
            CLIENT_LOG.error("Caught exception in ChatHandler. Packet: " + packet, e);
        }
        return true;
    }

    private String extractSenderNameNormalChat(final String message) {
        return message.substring(message.indexOf("<") + 1, message.indexOf(">"));
    }

    private String extractSenderNameReceivedWhisper(final String[] messageSplit) {
        return messageSplit[0].trim();
    }

    private String extractReceiverNameSentWhisper(final String[] messageSplit) {
        return messageSplit[1].replace(":", "");
    }
}
