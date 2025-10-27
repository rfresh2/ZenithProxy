package com.zenith.discord;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.zenith.Proxy;
import com.zenith.event.chat.DeathMessageChatEvent;
import com.zenith.event.chat.PublicChatEvent;
import com.zenith.event.chat.SystemChatEvent;
import com.zenith.event.chat.WhisperChatEvent;
import com.zenith.event.message.DiscordRelayChannelMessageReceivedEvent;
import com.zenith.event.message.PrivateMessageSendEvent;
import com.zenith.event.server.ServerPlayerConnectedEvent;
import com.zenith.event.server.ServerPlayerDisconnectedEvent;
import com.zenith.feature.deathmessages.DeathMessageParseResult;
import com.zenith.feature.deathmessages.KillerType;
import com.zenith.util.ChatUtil;
import com.zenith.util.ComponentSerializer;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.utils.Color;
import org.geysermc.mcprotocollib.protocol.data.game.PlayerListEntry;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundChatPacket;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

import static com.github.rfresh2.EventConsumer.of;
import static com.zenith.Globals.*;
import static com.zenith.discord.DiscordBot.escape;
import static com.zenith.discord.NotificationEventListener.notificationMention;
import static java.util.Objects.isNull;

public class ChatRelayEventListener {
    public static final ChatRelayEventListener INSTANCE = new ChatRelayEventListener();

    private ChatRelayEventListener() {}

    public void subscribeEvents() {
        EVENT_BUS.subscribe(
            this,
            of(DiscordRelayChannelMessageReceivedEvent.class, this::handleRelayInputMessage),
            of(WhisperChatEvent.class, this::handleWhisperChatEvent),
            of(SystemChatEvent.class, this::handleSystemChatEvent),
            of(PublicChatEvent.class, this::handlePublicChatEvent),
            of(PrivateMessageSendEvent.class, this::handlePrivateMessageSendEvent),
            of(DeathMessageChatEvent.class, this::handleDeathMessageChatEvent),
            of(ServerPlayerConnectedEvent.class, this::handleServerPlayerConnectedEvent),
            of(ServerPlayerDisconnectedEvent.class, this::handleServerPlayerDisconnectedEvent)
        );
    }

    private void handleRelayInputMessage(DiscordRelayChannelMessageReceivedEvent event) {
        if (!CONFIG.discord.chatRelay.enable) return;
        if (!CONFIG.discord.chatRelay.sendMessages) return;
        if (!Proxy.getInstance().isConnected() || event.message().isEmpty()) return;
        // determine if this message is a reply
        if (event.event().getMessage().getReferencedMessage() != null) {
            // we could do a bunch of if statements checking everything's in order and in expected format
            // ...or we could just throw an exception wherever it fails and catch it
            try {
                var messageData = event.event().getMessage().getReferencedMessage();
                // abort if reply is not to a message sent by us
                if (DISCORD.jda().getSelfUser().getIdLong() != messageData.getAuthor().getIdLong()) return;
                final MessageEmbed embed = messageData.getEmbeds().getFirst();
                if (embed.getColor() != null && embed.getColor().getRGB() == PRIVATE_MESSAGE_EMBED_COLOR.getRGB()) {
                    // replying to private message
                    sendPrivateMessage(event.message(), event.event());
                } else {
                    final String sender = extractRelayEmbedSenderUsername(embed.getColor(), embed.getDescription());
                    boolean pm = false;
                    var connections = Proxy.getInstance().getActiveConnections().getArray();
                    for (int i = 0; i < connections.length; i++) {
                        var connection = connections[i];
                        var name = connection.getName();
                        if (sender.equals(name)) {
                            sendPrivateMessage(event.message(), event.event());
                            pm = true;
                            break;
                        }
                    }
                    if (!pm) {
                        Proxy.getInstance().getClient().sendAsync(ChatUtil.getWhisperChatPacket(sender, event.message()));
                    }
                }
            } catch (final Exception e) {
                DISCORD_LOG.error("Error performing chat relay reply", e);
            }
        } else {
            if (event.message().startsWith(CONFIG.discord.prefix)) { // send as private message
                sendPrivateMessage(event.message().substring(CONFIG.discord.prefix.length()), event.event());
            } else {
                Proxy.getInstance().getClient().sendAsync(new ServerboundChatPacket(event.message()));
            }
        }
        DISCORD.lastRelayMessage = Optional.of(Instant.now());
    }

    private String extractRelayEmbedSenderUsername(@Nullable final Color color, final String msgContent) {
        final String sender;
        if (color != null && color.equals(Color.MAGENTA)) {
            // extract whisper sender
            sender = msgContent.split("\\*\\*")[1];
        } else if (color != null && color.equals(Color.BLACK)) {
            // extract public chat sender
            sender = msgContent.split("\\*\\*")[1].replace(":", "");
            // todo: we could support death messages here if we remove any bolded discord formatting and feed the message content into the parser
        } else {
            throw new RuntimeException("Unhandled message being replied to, aborting relay");
        }
        return sender;
    }

    private void handleWhisperChatEvent(WhisperChatEvent event) {
        if (!CONFIG.discord.chatRelay.whispers) return;
        if (!CONFIG.discord.chatRelay.enable || CONFIG.discord.chatRelay.channelId.isEmpty()) return;
        if (CONFIG.discord.chatRelay.ignoreQueue && Proxy.getInstance().isInQueue()) return;
        try {
            String message = ComponentSerializer.serializePlain(event.component());
            if (ignoreRegexFilter(message)) return;
            String ping = "";
            if (CONFIG.discord.chatRelay.mentionWhileConnected || isNull(Proxy.getInstance().getCurrentPlayer().get())) {
                if (CONFIG.discord.chatRelay.mentionRoleOnWhisper && !event.outgoing()) {
                    if (!message.toLowerCase(Locale.ROOT).contains("discord.gg/")
                        && !message.toLowerCase(Locale.ROOT).contains("discord.com/invite/")
                        && !PLAYER_LISTS.getIgnoreList().contains(event.sender().getName())) {
                        ping = notificationMention();
                    }
                }
            }
            message = message.replace(event.sender().getName(), "**" + event.sender().getName() + "**");
            if (!event.sender().getName().equals(event.receiver().getName())) {
                message = message.replace(event.receiver().getName(), "**" + event.receiver().getName() + "**");
            }
            UUID senderUUID = event.sender().getProfileId();
            final String avatarURL = Proxy.getInstance().getPlayerHeadURL(senderUUID).toString();
            var embed = Embed.builder()
                .description(escape(message))
                .footer("\u200b", avatarURL)
                .color(Color.MAGENTA);
            if (ping.isEmpty()) {
                sendRelayEmbedMessage(embed);
            } else {
                sendRelayEmbedMessage(ping, embed);
            }
        } catch (final Throwable e) {
            DISCORD_LOG.error("Error processing WhisperChatEvent", e);
        }
    }

    private void handleSystemChatEvent(SystemChatEvent event) {
        if (!CONFIG.discord.chatRelay.serverMessages) return;
        if (!CONFIG.discord.chatRelay.enable || CONFIG.discord.chatRelay.channelId.isEmpty()) return;
        if (CONFIG.discord.chatRelay.ignoreQueue && Proxy.getInstance().isInQueue()) return;
        try {
            String message = event.message();
            if (ignoreRegexFilter(message)) return;
            final String avatarURL = Proxy.getInstance().isOn2b2t() ? Proxy.getInstance().getPlayerHeadURL("Hausemaster").toString() : null;
            var embed = Embed.builder()
                .description(escape(message))
                .footer("\u200b", avatarURL)
                .color(Color.MOON_YELLOW);
            sendRelayEmbedMessage(embed);
        } catch (final Throwable e) {
            DISCORD_LOG.error("Error processing SystemChatEvent", e);
        }
    }

    private void handlePublicChatEvent(PublicChatEvent event) {
        if (!CONFIG.discord.chatRelay.publicChats) return;
        if (!CONFIG.discord.chatRelay.enable || CONFIG.discord.chatRelay.channelId.isEmpty()) return;
        if (CONFIG.discord.chatRelay.ignoreQueue && Proxy.getInstance().isInQueue()) return;
        try {
            String message = event.message();
            if (ignoreRegexFilter(message)) return;
            Color color = message.startsWith(">") ? Color.MEDIUM_SEA_GREEN : Color.BLACK;
            String ping = "";
            if (CONFIG.discord.chatRelay.mentionWhileConnected || isNull(Proxy.getInstance().getCurrentPlayer().get())) {
                if (CONFIG.discord.chatRelay.mentionRoleOnNameMention
                    && event.sender().getName().equals(CONFIG.authentication.username)
                    && !PLAYER_LISTS.getIgnoreList().contains(event.sender().getName())
                    && Arrays.asList(message.toLowerCase().split(" ")).contains(CONFIG.authentication.username.toLowerCase())) {
                    ping = notificationMention();
                }
            }
            message = "**" + event.sender().getName() + ":** " + message;
            UUID senderUUID = event.sender().getProfileId();
            final String avatarURL = Proxy.getInstance().getPlayerHeadURL(senderUUID).toString();
            var embed = Embed.builder()
                .description(escape(message))
                .footer("\u200b", avatarURL)
                .color(color);
            if (ping.isEmpty()) {
                sendRelayEmbedMessage(embed);
            } else {
                sendRelayEmbedMessage(ping, embed);
            }
        } catch (final Throwable e) {
            DISCORD_LOG.error("Error processing PublicChatEvent", e);
        }
    }

    private static final Color PRIVATE_MESSAGE_EMBED_COLOR = Color.RED;

    private void handlePrivateMessageSendEvent(final PrivateMessageSendEvent event) {
        var embed = Embed.builder()
            .description(escape("**" + event.getSenderName() + "**: " + event.getStringContents()))
            .color(PRIVATE_MESSAGE_EMBED_COLOR);
        if (event.getSenderUUID() != null) {
            embed.footer("Private Message", Proxy.getInstance().getPlayerHeadURL(event.getSenderUUID()).toString());
        } else {
            embed.footer("Private Message", null);
        }
        sendRelayEmbedMessage(embed);
    }

    private void handleDeathMessageChatEvent(DeathMessageChatEvent event) {
        if (!CONFIG.discord.chatRelay.deathMessages) return;
        if (!CONFIG.discord.chatRelay.enable || CONFIG.discord.chatRelay.channelId.isEmpty()) return;
        if (CONFIG.discord.chatRelay.ignoreQueue && Proxy.getInstance().isInQueue()) return;
        try {
            String message = event.message();
            if (ignoreRegexFilter(message)) return;
            DeathMessageParseResult death = event.deathMessage();
            message = message.replace(death.victim(), "**" + death.victim() + "**");
            var k = death.killer().filter(killer -> killer.type() == KillerType.PLAYER);
            if (k.isPresent()) message = message.replace(k.get().name(), "**" + k.get().name() + "**");
            String senderName = death.victim();
            UUID senderUUID = CACHE.getTabListCache().getFromName(death.victim()).map(PlayerListEntry::getProfileId).orElse(null);
            final String avatarURL = senderUUID != null
                ? Proxy.getInstance().getPlayerHeadURL(senderUUID).toString()
                : Proxy.getInstance().getPlayerHeadURL(senderName).toString();
            var embed = Embed.builder()
                .description(escape(message))
                .footer("\u200b", avatarURL)
                .color(Color.RUBY);
            sendRelayEmbedMessage(embed);
        } catch (final Throwable e) {
            DISCORD_LOG.error("Error processing DeathMessageChatEvent", e);
        }
    }

    private void handleServerPlayerConnectedEvent(ServerPlayerConnectedEvent event) {
        if (!CONFIG.discord.chatRelay.enable || !CONFIG.discord.chatRelay.connectionMessages || CONFIG.discord.chatRelay.channelId.isEmpty()) return;
        if (!Proxy.getInstance().isOnlineForAtLeastDuration(Duration.ofSeconds(3))) return;
        if (CONFIG.discord.chatRelay.ignoreQueue && Proxy.getInstance().isInQueue()) return;
        sendRelayEmbedMessage(Embed.builder()
            .description(escape("**" + event.playerEntry().getName() + "** connected"))
            .successColor()
            .footer("\u200b", Proxy.getInstance().getPlayerHeadURL(event.playerEntry().getProfileId()).toString()));
    }

    private void handleServerPlayerDisconnectedEvent(ServerPlayerDisconnectedEvent event) {
        if (!CONFIG.discord.chatRelay.enable || !CONFIG.discord.chatRelay.connectionMessages || CONFIG.discord.chatRelay.channelId.isEmpty()) return;
        if (!Proxy.getInstance().isOnlineForAtLeastDuration(Duration.ofSeconds(3))) return;
        if (CONFIG.discord.chatRelay.ignoreQueue && Proxy.getInstance().isInQueue()) return;
        sendRelayEmbedMessage(Embed.builder()
            .description(escape("**" + event.playerEntry().getName() + "** disconnected"))
            .errorColor()
            .footer("\u200b", Proxy.getInstance().getPlayerHeadURL(event.playerEntry().getProfileId()).toString()));
    }

    final Cache<String, Pattern> ignoreRegexCache = CacheBuilder.newBuilder()
        .expireAfterAccess(Duration.ofMinutes(1))
        .build();

    private boolean ignoreRegexFilter(String contents) {
        for (int i = 0; i < CONFIG.discord.chatRelay.ignoreRegex.size(); i++) {
            var regex = CONFIG.discord.chatRelay.ignoreRegex.get(i);
            Pattern pattern;
            try {
                pattern = ignoreRegexCache.get(regex, () -> Pattern.compile(regex));
            } catch (Exception e) {
                CONFIG.discord.chatRelay.ignoreRegex.remove(i);
                i--;
                continue;
            }
            if (pattern.matcher(contents).find()) {
                DISCORD_LOG.debug("Filtering relay message: '{}' matched regex: '{}'", contents, regex);
                return true;
            }
        }
        return false;
    }

    private void sendPrivateMessage(String message, MessageReceivedEvent event) {
        EVENT_BUS.postAsync(new PrivateMessageSendEvent(
            event.getMessage().getAuthor().getName(),
            message));
    }

    public void sendRelayEmbedMessage(Embed embedCreateSpec) {
        DISCORD.sendRelayEmbedMessage(embedCreateSpec);
    }
    public void sendRelayEmbedMessage(String message, Embed embed) {
        DISCORD.sendRelayEmbedMessage(message, embed);
    }
    public void sendRelayMessage(final String message) {
        DISCORD.sendRelayMessage(message);
    }
}
