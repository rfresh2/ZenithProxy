package com.zenith.network.client.handler.incoming;

import com.zenith.Proxy;
import com.zenith.event.chat.DeathMessageChatEvent;
import com.zenith.event.chat.PublicChatEvent;
import com.zenith.event.chat.SystemChatEvent;
import com.zenith.event.chat.WhisperChatEvent;
import com.zenith.event.queue.QueueSkipEvent;
import com.zenith.event.server.ClientDeathMessageEvent;
import com.zenith.feature.deathmessages.DeathMessageParseResult;
import com.zenith.feature.deathmessages.DeathMessagesParser;
import com.zenith.network.client.ClientSession;
import com.zenith.network.codec.ClientEventLoopPacketHandler;
import com.zenith.util.ComponentSerializer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundSystemChatPacket;
import org.jspecify.annotations.NonNull;

import java.util.Objects;
import java.util.Optional;

import static com.zenith.Globals.*;
import static java.util.Objects.nonNull;

public class SystemChatHandler implements ClientEventLoopPacketHandler<ClientboundSystemChatPacket, ClientSession> {
    private static final TextColor DEATH_MSG_COLOR_2b2t = TextColor.color(170, 0, 0);
    private final DeathMessagesParser deathMessagesHelper = new DeathMessagesParser();

    @Override
    public boolean applyAsync(@NonNull ClientboundSystemChatPacket packet, @NonNull ClientSession session) {
        try {

            final boolean essentialsChat = CONFIG.client.extra.chat.essentialsFormatting;

            if (CONFIG.client.extra.logChatMessages) {
                var component = packet.getContent();
                if (Proxy.getInstance().isInQueue()) {
                    component = component.replaceText(b -> b
                        .matchLiteral("\n\n")
                        .replacement("")
                    );
                }
                CHAT_LOG.info(component);
            }
            final Component component = packet.getContent();
            final String messageString = ComponentSerializer.serializePlain(component);
            Optional<DeathMessageParseResult> deathMessage = Optional.empty();
            String senderName = null;
            String whisperTarget = null;
            if (!messageString.startsWith("<") && Proxy.getInstance().isOn2b2t())
                deathMessage = parseDeathMessage2b2t(component, deathMessage, messageString);
            if (messageString.startsWith("<")) {
                senderName = extractSenderNameNormalChat(messageString);
            } 
            else if (deathMessage.isEmpty()) {
                if (essentialsChat && messageString.startsWith("["))
                {
                    // [$senderName -> me] $messageText
                    // [me -> $whisperTarget] $messageText
                    final String inner = extractBracketContents(messageString, 0);
                    if (inner.endsWith(" -> me")) 
                    {
                        senderName = inner.substring(0, inner.length() - 6);
                        whisperTarget = CONFIG.authentication.username;
                    }
                    else if (inner.startsWith("me -> "))
                    {
                        senderName = CONFIG.authentication.username;
                        whisperTarget = inner.substring(6);
                    }
                }
                else
                { 
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
            }

            final String decoratedSenderName = senderName;
            final String decoratedWhisperTarget = whisperTarget;

            // Try to strip any ranks or other decoration from the names
            if (essentialsChat && senderName != null)
            {
                final String[] split = senderName.split(" ");
                senderName = split[split.length - 1];
            }
            if (essentialsChat && whisperTarget != null)
            {
                final String[] split = whisperTarget.split(" ");
                whisperTarget = split[split.length - 1];
            }

            var sender = Optional.ofNullable(senderName).flatMap(t -> CACHE.getTabListCache().getFromName(t));
            var playerWhisperTarget = Optional.ofNullable(whisperTarget).flatMap(t -> CACHE.getTabListCache().getFromName(t));

            // The above attempt at getting the player name failed. Try to match the full display name against a display name in tab instead.
            if (sender.isEmpty() && decoratedSenderName != null && essentialsChat)
            {
                sender = Optional.ofNullable(decoratedSenderName).flatMap(t -> CACHE.getTabListCache().getFromDisplayName(t));
            }
            if (playerWhisperTarget.isEmpty() && decoratedWhisperTarget != null && essentialsChat)
            {
                playerWhisperTarget = Optional.ofNullable(decoratedWhisperTarget).flatMap(t -> CACHE.getTabListCache().getFromDisplayName(t));
            }

            // Try to match the clipped display name against a display name in tab too.
            if (sender.isEmpty() && senderName != null && essentialsChat)
            {
                sender = Optional.ofNullable(senderName).flatMap(t -> CACHE.getTabListCache().getFromDisplayName(t));
            }
            if (playerWhisperTarget.isEmpty() && whisperTarget != null && essentialsChat)
            {
                playerWhisperTarget = Optional.ofNullable(whisperTarget).flatMap(t -> CACHE.getTabListCache().getFromDisplayName(t));
            }

            // Mark false-positive system messages caused by unresolved names, so chatRelay still handles them correctly.
            final boolean isUnresolvedIncomingWhisper = sender.isEmpty() && playerWhisperTarget.isPresent() 
                && playerWhisperTarget.get().getName().equalsIgnoreCase(CONFIG.authentication.username);
            final boolean isUnresolvedWhisper = whisperTarget != null && (playerWhisperTarget.isEmpty() || sender.isEmpty());
            final boolean isUnresolvedPublicChat = whisperTarget == null && senderName != null && sender.isEmpty();

            if (isUnresolvedWhisper) 
            {
                sender = Optional.empty();
            }

            if (Proxy.getInstance().isOn2b2t()
                && "Reconnecting to server 2b2t.".equals(messageString)
                && NamedTextColor.GOLD.equals(component.style().color())) {
                CLIENT_LOG.info("Queue Skip Detected");
                EVENT_BUS.postAsync(QueueSkipEvent.INSTANCE);
            }

            if (sender.isPresent() && deathMessage.isEmpty() && playerWhisperTarget.isEmpty()) {
                EVENT_BUS.postAsync(new PublicChatEvent(sender.get(), component, messageString));
            } else if (sender.isPresent() && deathMessage.isEmpty() && playerWhisperTarget.isPresent()) {
                var outgoing = sender.get().getName().equalsIgnoreCase(CONFIG.authentication.username);
                EVENT_BUS.postAsync(new WhisperChatEvent(outgoing, sender.get(), playerWhisperTarget.get(), component, messageString));
            } else if (sender.isEmpty() && deathMessage.isPresent() && playerWhisperTarget.isEmpty()) {
                EVENT_BUS.postAsync(new DeathMessageChatEvent(deathMessage.get(), component, messageString));
            } else {
                EVENT_BUS.postAsync(new SystemChatEvent(component, messageString, isUnresolvedPublicChat, isUnresolvedWhisper, isUnresolvedIncomingWhisper));
            }
        } catch (final Exception e) {
            CLIENT_LOG.error("Caught exception in ChatHandler. Packet: {}", packet, e);
        }
        return true;
    }

    private Optional<DeathMessageParseResult> parseDeathMessage2b2t(final Component component, Optional<DeathMessageParseResult> deathMessage, final String messageString) {
        if (component.children().stream().anyMatch(child -> nonNull(child.color())
            && Objects.equals(child.color(), DEATH_MSG_COLOR_2b2t))) { // death message color on 2b
            deathMessage = deathMessagesHelper.parse(component, messageString);
            if (deathMessage.isPresent()) {
                if (deathMessage.get().victim().equals(CACHE.getProfileCache().getProfile().getName())) {
                    EVENT_BUS.postAsync(new ClientDeathMessageEvent(messageString));
                }
            } else {
                CLIENT_LOG.warn("Failed to parse death message: {}", messageString);
            }
        }
        return deathMessage;
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

    private String extractBracketContents(final String _str, Integer index) {
        final char[] str = _str.toCharArray();
        Integer level = 0;
        for (Integer i = index; i < _str.length(); i++)
        {
            if (str[i] == '[') level++;
            if (str[i] == ']' && --level == 0)
            {
                return _str.substring(index + 1, i);
            }
        }
        return "";
    }
}
