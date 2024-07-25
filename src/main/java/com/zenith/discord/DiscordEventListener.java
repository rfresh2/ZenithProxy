package com.zenith.discord;

import com.zenith.Proxy;
import com.zenith.event.module.*;
import com.zenith.event.proxy.*;
import com.zenith.feature.deathmessages.DeathMessageParseResult;
import com.zenith.feature.deathmessages.KillerType;
import com.zenith.feature.queue.Queue;
import com.zenith.util.DisconnectReasonInfo;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.object.component.Button;
import discord4j.core.object.entity.User;
import discord4j.core.object.presence.ClientPresence;
import discord4j.discordjson.json.EmbedData;
import discord4j.discordjson.json.MessageData;
import discord4j.rest.util.Color;
import org.geysermc.mcprotocollib.protocol.data.game.PlayerListEntry;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundChatPacket;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;

import static com.github.rfresh2.EventConsumer.of;
import static com.zenith.Shared.*;
import static com.zenith.command.impl.StatusCommand.getCoordinates;
import static com.zenith.discord.DiscordBot.*;
import static com.zenith.util.math.MathHelper.formatDuration;
import static java.util.Arrays.asList;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

public class DiscordEventListener {
    private final DiscordBot bot;

    public DiscordEventListener(DiscordBot bot) {
        this.bot = bot;
    }

    public void subscribeEvents() {
        if (EVENT_BUS.isSubscribed(this)) throw new RuntimeException("Event handlers already initialized");
        EVENT_BUS.subscribe(
            this,
            of(ConnectEvent.class, this::handleConnectEvent),
            of(PlayerOnlineEvent.class, this::handlePlayerOnlineEvent),
            of(DisconnectEvent.class, this::handleDisconnectEvent),
            of(QueuePositionUpdateEvent.class, this::handleQueuePositionUpdateEvent),
            of(AutoEatOutOfFoodEvent.class, this::handleAutoEatOutOfFoodEvent),
            of(QueueCompleteEvent.class, this::handleQueueCompleteEvent),
            of(StartQueueEvent.class, this::handleStartQueueEvent),
            of(DeathEvent.class, this::handleDeathEvent),
            of(SelfDeathMessageEvent.class, this::handleSelfDeathMessageEvent),
            of(HealthAutoDisconnectEvent.class, this::handleHealthAutoDisconnectEvent),
            of(ProxyClientConnectedEvent.class, this::handleProxyClientConnectedEvent),
            of(ProxySpectatorConnectedEvent.class, this::handleProxySpectatorConnectedEvent),
            of(ProxyClientDisconnectedEvent.class, this::handleProxyClientDisconnectedEvent),
            of(VisualRangeEnterEvent.class, this::handleVisualRangeEnterEvent),
            of(VisualRangeLeaveEvent.class, this::handleVisualRangeLeaveEvent),
            of(VisualRangeLogoutEvent.class, this::handleVisualRangeLogoutEvent),
            of(NonWhitelistedPlayerConnectedEvent.class, this::handleNonWhitelistedPlayerConnectedEvent),
            of(ProxySpectatorDisconnectedEvent.class, this::handleProxySpectatorDisconnectedEvent),
            of(ActiveHoursConnectEvent.class, this::handleActiveHoursConnectEvent),
            of(ServerChatReceivedEvent.class, this::handleServerChatReceivedEvent),
            of(ServerPlayerConnectedEvent.class, this::handleServerPlayerConnectedEvent),
            of(ServerPlayerDisconnectedEvent.class, this::handleServerPlayerDisconnectedEvent),
            of(DiscordMessageSentEvent.class, this::handleDiscordMessageSentEvent),
            of(UpdateStartEvent.class, this::handleUpdateStartEvent),
            of(ServerRestartingEvent.class, this::handleServerRestartingEvent),
            of(ProxyLoginFailedEvent.class, this::handleProxyLoginFailedEvent),
            of(StartConnectEvent.class, this::handleStartConnectEvent),
            of(PrioStatusUpdateEvent.class, this::handlePrioStatusUpdateEvent),
            of(PrioBanStatusUpdateEvent.class, this::handlePrioBanStatusUpdateEvent),
            of(AutoReconnectEvent.class, this::handleAutoReconnectEvent),
            of(MsaDeviceCodeLoginEvent.class, this::handleMsaDeviceCodeLoginEvent),
            of(DeathMessageEvent.class, this::handleDeathMessageEvent),
            of(UpdateAvailableEvent.class, this::handleUpdateAvailableEvent),
            of(ReplayStartedEvent.class, this::handleReplayStartedEvent),
            of(ReplayStoppedEvent.class, this::handleReplayStoppedEvent),
            of(PlayerTotemPopAlertEvent.class, this::handleTotemPopEvent),
            of(NoTotemsEvent.class, this::handleNoTotemsEvent)
        );
    }

    public void handleConnectEvent(ConnectEvent event) {
        sendEmbedMessage(Embed.builder()
                             .title("Proxy Connected")
                             .inQueueColor()
                             .addField("Server", CONFIG.client.server.address, true)
                             .addField("Proxy IP", CONFIG.server.getProxyAddress(), false));
        updatePresence(bot.defaultConnectedPresence.get());
    }

    public void handlePlayerOnlineEvent(PlayerOnlineEvent event) {
        var embedBuilder = Embed.builder()
            .title("Proxy Online")
            .successColor();
        event.queueWait()
            .ifPresent(duration -> embedBuilder.addField("Queue Duration", formatDuration(duration), true));
        sendEmbedMessage(embedBuilder);
    }

    public void handleDisconnectEvent(DisconnectEvent event) {
        var embed = Embed.builder()
            .title("Proxy Disconnected")
            .addField("Reason", "[" + event.reason() + "](" + DisconnectReasonInfo.getDisconnectCategory(event.reason()).getWikiURL() + ")", false)
            .addField("Online Duration", formatDuration(event.onlineDuration()), false)
            .errorColor();
        if (Proxy.getInstance().isOn2b2t()
            && !Proxy.getInstance().isPrio()
            && event.reason().startsWith("You have lost connection")) {
            if (event.onlineDuration().toSeconds() >= 0L
                && event.onlineDuration().toSeconds() <= 1L) {
                embed.description("""
                              You have likely been kicked for reaching the 2b2t non-prio account IP limit.
                              Consider configuring a connection proxy with the `clientConnection` command.
                              Or migrate ZenithProxy instances to multiple hosts/IP's.
                              """);
            } else if (event.wasInQueue() && event.queuePosition() <= 1) {
                embed.description("""
                              You have likely been kicked due to being IP banned by 2b2t.
                              
                              To check, try connecting and waiting through queue with the same account from a different IP.
                              """);
            }
        }

        sendEmbedMessage(embed);
        EXECUTOR.execute(() -> updatePresence(bot.disconnectedPresence));
    }

    public void handleQueuePositionUpdateEvent(QueuePositionUpdateEvent event) {
        if (CONFIG.discord.queueWarning.enabled) {
            if (event.position() == CONFIG.discord.queueWarning.position) {
                bot.sendQueueWarning();
            } else if (event.position() <= 3) {
                bot.sendQueueWarning();
            }
        }
        updatePresence(bot.getQueuePresence());
    }

    public void handleAutoEatOutOfFoodEvent(final AutoEatOutOfFoodEvent event) {
        sendEmbedMessage(Embed.builder()
                             .title("AutoEat Out Of Food")
                             .description("AutoEat threshold met but player has no food")
                             .errorColor());
    }

    public void handleQueueCompleteEvent(QueueCompleteEvent event) {
        updatePresence(bot.defaultConnectedPresence.get());
    }

    public void handleStartQueueEvent(StartQueueEvent event) {
        sendEmbedMessage(Embed.builder()
                             .title("Started Queuing")
                             .inQueueColor()
                             .addField("Regular Queue", Queue.getQueueStatus().regular(), true)
                             .addField("Priority Queue", Queue.getQueueStatus().prio(), true));
        updatePresence(bot.getQueuePresence());
    }

    public void handleDeathEvent(DeathEvent event) {
        sendEmbedMessage(Embed.builder()
                             .title("Player Death")
                             .errorColor()
                             .addField("Coordinates", getCoordinates(CACHE.getPlayerCache()), false));
    }

    public void handleSelfDeathMessageEvent(SelfDeathMessageEvent event) {
        sendEmbedMessage(Embed.builder()
                             .title("Death Message")
                             .errorColor()
                             .addField("Message", event.message(), false));
    }

    public void handleHealthAutoDisconnectEvent(HealthAutoDisconnectEvent event) {
        sendEmbedMessage(Embed.builder()
                             .title("Health AutoDisconnect Triggered")
                             .addField("Health", CACHE.getPlayerCache().getThePlayer().getHealth(), true)
                             .primaryColor());
    }

    public void handleProxyClientConnectedEvent(ProxyClientConnectedEvent event) {
        if (CONFIG.client.extra.clientConnectionMessages) {
            sendEmbedMessage(Embed.builder()
                                 .title("Client Connected")
                                 .addField("Username", event.clientGameProfile().getName(), true)
                                 .primaryColor());
        }
    }

    public void handleProxySpectatorConnectedEvent(ProxySpectatorConnectedEvent event) {
        if (CONFIG.client.extra.clientConnectionMessages) {
            sendEmbedMessage(Embed.builder()
                                 .title("Spectator Connected")
                                 .addField("Username", escape(event.clientGameProfile().getName()), true)
                                 .primaryColor());
        }
    }

    public void handleProxyClientDisconnectedEvent(ProxyClientDisconnectedEvent event) {
        if (!CONFIG.client.extra.clientConnectionMessages) return;
        var builder = Embed.builder()
            .title("Client Disconnected")
            .errorColor();
        if (nonNull(event.clientGameProfile())) {
            builder = builder.addField("Username", escape(event.clientGameProfile().getName()), false);
        }
        if (nonNull(event.reason())) {
            builder = builder.addField("Reason", escape(event.reason()), false);
        }
        sendEmbedMessage(builder);
    }

    public void handleVisualRangeEnterEvent(VisualRangeEnterEvent event) {
        var embedCreateSpec = Embed.builder()
            .title("Player In Visual Range")
            .color(event.isFriend() ? CONFIG.theme.success.discord() : CONFIG.theme.error.discord())
            .addField("Player Name", escape(event.playerEntry().getName()), true)
            .addField("Player UUID", ("[" + event.playerEntry().getProfileId() + "](https://namemc.com/profile/" + event.playerEntry().getProfileId() + ")"), true)
            .thumbnail(Proxy.getInstance().getAvatarURL(event.playerEntry().getProfileId()).toString());

        if (CONFIG.discord.reportCoords) {
            embedCreateSpec.addField("Coordinates", "||["
                + (int) event.playerEntity().getX() + ", "
                + (int) event.playerEntity().getY() + ", "
                + (int) event.playerEntity().getZ()
                + "]||", false);
        }
        final String buttonId = "addFriend" + ThreadLocalRandom.current().nextInt(1000000);
        final List<Button> buttons = asList(Button.primary(buttonId, "Add Friend"));
        final Function<ButtonInteractionEvent, Publisher<Mono<?>>> mapper = e -> {
            if (e.getCustomId().equals(buttonId)) {
                DISCORD_LOG.info(e.getInteraction().getMember()
                                     .map(User::getTag).orElse("Unknown")
                                     + " added friend: " + event.playerEntry().getName() + " [" + event.playerEntry().getProfileId() + "]");
                PLAYER_LISTS.getFriendsList().add(event.playerEntry().getName());
                e.reply().withEmbeds(Embed.builder()
                                         .title("Friend Added")
                                         .successColor()
                                         .addField("Player Name", escape(event.playerEntry().getName()), true)
                                         .addField("Player UUID", ("[" + event.playerEntry().getProfileId() + "](https://namemc.com/profile/" + event.playerEntry().getProfileId() + ")"), true)
                                         .thumbnail(Proxy.getInstance().getAvatarURL(event.playerEntry().getProfileId()).toString())
                                         .toSpec())
                    .block();
                saveConfigAsync();
            }
            return Mono.empty();
        };
        if (CONFIG.client.extra.visualRange.enterAlertMention)
            if (!event.isFriend())
                if (CONFIG.discord.visualRangeMentionRoleId.length() > 3)
                    sendEmbedMessageWithButtons(mentionRole(CONFIG.discord.visualRangeMentionRoleId), embedCreateSpec, buttons, mapper, Duration.ofHours(1));
                else
                    sendEmbedMessageWithButtons(mentionAccountOwner(), embedCreateSpec, buttons, mapper, Duration.ofHours(1));
            else
                sendEmbedMessage(embedCreateSpec);
        else
            if (!event.isFriend())
                sendEmbedMessageWithButtons(embedCreateSpec, buttons, mapper, Duration.ofHours(1));
            else
                sendEmbedMessage(embedCreateSpec);
    }

    public void handleVisualRangeLeaveEvent(final VisualRangeLeaveEvent event) {
        var embedCreateSpec = Embed.builder()
            .title("Player Left Visual Range")
            .color(event.isFriend() ? CONFIG.theme.success.discord() : CONFIG.theme.error.discord())
            .addField("Player Name", escape(event.playerEntry().getName()), true)
            .addField("Player UUID", ("[" + event.playerEntity().getUuid() + "](https://namemc.com/profile/" + event.playerEntry().getProfileId() + ")"), true)
            .thumbnail(Proxy.getInstance().getAvatarURL(event.playerEntity().getUuid()).toString());

        if (CONFIG.discord.reportCoords) {
            embedCreateSpec.addField("Coordinates", "||["
                + (int) event.playerEntity().getX() + ", "
                + (int) event.playerEntity().getY() + ", "
                + (int) event.playerEntity().getZ()
                + "]||", false);
        }
        sendEmbedMessage(embedCreateSpec);
    }

    public void handleVisualRangeLogoutEvent(final VisualRangeLogoutEvent event) {
        var embedCreateSpec = Embed.builder()
            .title("Player Logout In Visual Range")
            .color(event.isFriend() ? CONFIG.theme.success.discord() : CONFIG.theme.error.discord())
            .addField("Player Name", escape(event.playerEntry().getName()), true)
            .addField("Player UUID", ("[" + event.playerEntity().getUuid() + "](https://namemc.com/profile/" + event.playerEntry().getProfileId() + ")"), true)
            .thumbnail(Proxy.getInstance().getAvatarURL(event.playerEntity().getUuid()).toString());

        if (CONFIG.discord.reportCoords) {
            embedCreateSpec.addField("Coordinates", "||["
                + (int) event.playerEntity().getX() + ", "
                + (int) event.playerEntity().getY() + ", "
                + (int) event.playerEntity().getZ()
                + "]||", false);
        }
        sendEmbedMessage(embedCreateSpec);
    }

    public void handleNonWhitelistedPlayerConnectedEvent(NonWhitelistedPlayerConnectedEvent event) {
        var builder = Embed.builder()
            .title("Non-Whitelisted Player Connected")
            .errorColor();
        if (nonNull(event.remoteAddress()) && CONFIG.discord.showNonWhitelistLoginIP) {
            builder = builder.addField("IP", escape(event.remoteAddress().toString()), false);
        }
        if (nonNull(event.gameProfile()) && nonNull(event.gameProfile().getId()) && nonNull(event.gameProfile().getName())) {
            builder
                .addField("Username", escape(event.gameProfile().getName()), false)
                .addField("Player UUID", ("[" + event.gameProfile().getId().toString() + "](https://namemc.com/profile/" + event.gameProfile().getId().toString() + ")"), true)
                .thumbnail(Proxy.getInstance().getAvatarURL(event.gameProfile().getId()).toString());
            final String buttonId = "whitelist" + ThreadLocalRandom.current().nextInt(10000000);
            final List<Button> buttons = asList(Button.primary(buttonId, "Whitelist Player"));
            final Function<ButtonInteractionEvent, Publisher<Mono<?>>> mapper = e -> {
                if (e.getCustomId().equals(buttonId)) {
                    if (validateButtonInteractionEventFromAccountOwner(e)) {
                        DISCORD_LOG.info(e.getInteraction().getMember()
                                             .map(User::getTag).orElse("Unknown")
                                             + " whitelisted " + event.gameProfile().getName() + " [" + event.gameProfile().getId().toString() + "]");
                        PLAYER_LISTS.getWhitelist().add(event.gameProfile().getName());
                        e.reply().withEmbeds(Embed.builder()
                                                 .title("Player Whitelisted")
                                                 .successColor()
                                                 .addField("Player Name", escape(event.gameProfile().getName()), true)
                                                 .addField("Player UUID", ("[" + event.gameProfile().getId().toString() + "](https://namemc.com/profile/" + event.gameProfile().getId().toString() + ")"), true)
                                                 .thumbnail(Proxy.getInstance().getAvatarURL(event.gameProfile().getId()).toString())
                                                 .toSpec()).block();
                        saveConfigAsync();
                    } else {
                        DISCORD_LOG.error(e.getInteraction().getMember()
                                              .map(User::getTag).orElse("Unknown")
                                              + " attempted to whitelist " + event.gameProfile().getName() + " [" + event.gameProfile().getId().toString() + "] but was not authorized to do so!");
                        e.reply().withEmbeds(Embed.builder()
                                                 .title("Not Authorized!")
                                                 .errorColor()
                                                 .addField("Error",
                                                           "User: " + e.getInteraction().getMember().map(User::getTag).orElse("Unknown")
                                                               + " is not authorized to execute this command! Contact the account owner", true)
                                                 .toSpec()).block();
                    }
                }
                return Mono.empty();
            };
            sendEmbedMessageWithButtons(builder, buttons, mapper, Duration.ofHours(1L));
        } else { // shouldn't be possible if verifyUsers is enabled
            if (nonNull(event.gameProfile())) {
                builder
                    .addField("Username", escape(event.gameProfile().getName()), false);
            }
            sendEmbedMessage(builder);
        }
    }

    public void handleProxySpectatorDisconnectedEvent(ProxySpectatorDisconnectedEvent event) {
        if (CONFIG.client.extra.clientConnectionMessages) {
            var builder = Embed.builder()
                .title("Spectator Disconnected")
                .errorColor();
            if (nonNull(event.clientGameProfile())) {
                builder = builder.addField("Username", escape(event.clientGameProfile().getName()), false);
            }
            sendEmbedMessage(builder);
        }
    }

    public void handleActiveHoursConnectEvent(ActiveHoursConnectEvent event) {
        int queueLength;
        if (Proxy.getInstance().isPrio()) {
            queueLength = Queue.getQueueStatus().prio();
        } else {
            queueLength = Queue.getQueueStatus().regular();
        }
        var embed = Embed.builder()
            .title("Active Hours Connect Triggered")
            .addField("ETA", Queue.getQueueEta(queueLength), false)
            .primaryColor();
        if (event.willWait())
            embed.addField("Info", "Waiting 1 minute to avoid 2b2t reconnect queue skip", false);
        sendEmbedMessage(embed);
    }

    public void handleServerChatReceivedEvent(ServerChatReceivedEvent event) {
        if (!CONFIG.discord.chatRelay.enable || CONFIG.discord.chatRelay.channelId.isEmpty()) return;
        if (CONFIG.discord.chatRelay.ignoreQueue && Proxy.getInstance().isInQueue()) return;
        try {
            String message = event.message();
            String ping = "";
            if (CONFIG.discord.chatRelay.mentionWhileConnected || isNull(Proxy.getInstance().getCurrentPlayer().get())) {
                if (CONFIG.discord.chatRelay.mentionRoleOnWhisper || CONFIG.discord.chatRelay.mentionRoleOnNameMention) {
                    if (!message.startsWith("<")) {
                        if (event.isIncomingWhisper()
                            && CONFIG.discord.chatRelay.mentionRoleOnWhisper
                            && !message.toLowerCase(Locale.ROOT).contains("discord.gg/")
                            && event.sender().map(s -> !PLAYER_LISTS.getIgnoreList().contains(s.getName())).orElse(true)) {
                            ping = mentionAccountOwner();
                        }
                    } else {
                        if (CONFIG.discord.chatRelay.mentionRoleOnNameMention) {
                            if (event.sender().filter(sender -> sender.getName().equals(CONFIG.authentication.username)).isEmpty()
                                && event.sender().map(s -> !PLAYER_LISTS.getIgnoreList().contains(s.getName())).orElse(true)
                                && Arrays.asList(message.toLowerCase().split(" ")).contains(CONFIG.authentication.username.toLowerCase())) {
                                ping = mentionAccountOwner();
                            }
                        }
                    }
                }
            }
            final UUID senderUUID;
            final String senderName;
            if (event.isPublicChat()) {
                if (!CONFIG.discord.chatRelay.publicChats) return;
                message = "**" + event.sender().get().getName() + ":** " + message.substring(message.indexOf(" ") + 1);
                senderName = event.sender().get().getName();
                senderUUID = event.sender().get().getProfileId();
            } else if (event.isWhisper()) {
                if (!CONFIG.discord.chatRelay.whispers) return;
                message = message.replace(event.sender().get().getName(), "**" + event.sender().get().getName() + "**");
                message = message.replace(event.whisperTarget().get().getName(), "**" + event.whisperTarget().get().getName() + "**");
                senderName = event.sender().get().getName();
                senderUUID = event.sender().get().getProfileId();
            } else if (event.isDeathMessage()) {
                if (!CONFIG.discord.chatRelay.deathMessages) return;
                DeathMessageParseResult death = event.deathMessage().get();
                message = message.replace(death.victim(), "**" + death.victim() + "**");
                var k = death.killer().filter(killer -> killer.type() == KillerType.PLAYER);
                if (k.isPresent()) message = message.replace(k.get().name(), "**" + k.get().name() + "**");
                senderName = death.victim();
                senderUUID = CACHE.getTabListCache().getFromName(death.victim()).map(PlayerListEntry::getProfileId).orElse(null);
            } else {
                if (!CONFIG.discord.chatRelay.serverMessages) return;
                senderName = "Hausemaster";
                senderUUID = null;
            }
            final String avatarURL = senderUUID != null ? Proxy.getInstance().getAvatarURL(senderUUID).toString() : Proxy.getInstance().getAvatarURL(senderName).toString();
            var embed = Embed.builder()
                .description(escape(message))
                .footer("\u200b", avatarURL)
                .color(event.isPublicChat() ? (event.publicChatContent().startsWith(">") ? Color.MEDIUM_SEA_GREEN : Color.BLACK)
                           : event.isDeathMessage() ? Color.RUBY
                    : event.isWhisper() ? Color.MAGENTA
                    : Color.MOON_YELLOW)
                .timestamp(Instant.now());
            if (ping.isEmpty()) {
                sendRelayEmbedMessage(embed);
            } else {
                sendRelayEmbedMessage(ping, embed);
            }
        } catch (final Throwable e) {
            DISCORD_LOG.error("", e);
        }
    }

    public void handleServerPlayerConnectedEvent(ServerPlayerConnectedEvent event) {
        if (CONFIG.discord.chatRelay.enable && CONFIG.discord.chatRelay.connectionMessages && !CONFIG.discord.chatRelay.channelId.isEmpty()) {
            if (!Proxy.getInstance().isOnlineForAtLeastDuration(Duration.ofSeconds(3))) return;
            if (CONFIG.discord.chatRelay.ignoreQueue && Proxy.getInstance().isInQueue()) return;
            sendRelayEmbedMessage(Embed.builder()
                                      .description(escape("**" + event.playerEntry().getName() + "** connected"))
                                      .successColor()
                                      .footer("\u200b", Proxy.getInstance().getAvatarURL(event.playerEntry().getProfileId()).toString())
                                      .timestamp(Instant.now()));
        }
        if (CONFIG.client.extra.stalk.enabled && PLAYER_LISTS.getStalkList().contains(event.playerEntry().getProfile())) {
            sendEmbedMessage(mentionAccountOwner(), Embed.builder()
                .title("Stalked Player Online!")
                .successColor()
                .addField("Player Name", event.playerEntry().getName(), true)
                .thumbnail(Proxy.getInstance().getAvatarURL(event.playerEntry().getProfileId()).toString()));
        }
    }

    public void handleServerPlayerDisconnectedEvent(ServerPlayerDisconnectedEvent event) {
        if (CONFIG.discord.chatRelay.enable && CONFIG.discord.chatRelay.connectionMessages && !CONFIG.discord.chatRelay.channelId.isEmpty()) {
            if (!Proxy.getInstance().isOnlineForAtLeastDuration(Duration.ofSeconds(3))) return;
            if (CONFIG.discord.chatRelay.ignoreQueue && Proxy.getInstance().isInQueue()) return;
            sendRelayEmbedMessage(Embed.builder()
                                      .description(escape("**" + event.playerEntry().getName() + "** disconnected"))
                                      .errorColor()
                                      .footer("\u200b", Proxy.getInstance().getAvatarURL(event.playerEntry().getProfileId()).toString())
                                      .timestamp(Instant.now()));
        }
        if (CONFIG.client.extra.stalk.enabled && PLAYER_LISTS.getStalkList().contains(event.playerEntry().getProfile())) {
            sendEmbedMessage(mentionAccountOwner(), Embed.builder()
                .title("Stalked Player Offline!")
                .errorColor()
                .addField("Player Name", event.playerEntry().getName(), true)
                .thumbnail(Proxy.getInstance().getAvatarURL(event.playerEntry().getProfileId()).toString()));
        }
    }

    public void handleDiscordMessageSentEvent(DiscordMessageSentEvent event) {
        if (!CONFIG.discord.chatRelay.enable) return;
        if (!CONFIG.discord.chatRelay.sendMessages) return;
        if (!Proxy.getInstance().isConnected() || event.message().isEmpty()) return;
        // determine if this message is a reply
        if (event.event().getMessage().getReferencedMessage().isPresent()) {
            // we could do a bunch of if statements checking everything's in order and in expected format
            // ...or we could just throw an exception wherever it fails and catch it
            try {
                final MessageData messageData = event.event().getMessage().getReferencedMessage().get().getData();
                // abort if reply is not to a message sent by us
                if (bot.client.getSelfId().asLong() != messageData.author().id().asLong()) return;
                final EmbedData embed = messageData.embeds().getFirst();
                final String sender = bot.extractRelayEmbedSenderUsername(embed.color(), embed.description().get());
                Proxy.getInstance().getClient().sendAsync(new ServerboundChatPacket("/w " + sender + " " + event.message()));
            } catch (final Exception e) {
                DISCORD_LOG.error("Error performing chat relay reply", e);
            }
        } else Proxy.getInstance().getClient().sendAsync(new ServerboundChatPacket(event.message()));
        bot.lastRelaymessage = Optional.of(Instant.now());
    }

    public void handleUpdateStartEvent(UpdateStartEvent event) {
        sendEmbedMessage(bot.getUpdateMessage(event.newVersion()));
    }

    public void handleServerRestartingEvent(ServerRestartingEvent event) {
        sendEmbedMessage(Embed.builder()
                             .title("Server Restarting")
                             .errorColor()
                             .addField("Message", event.message(), true));
    }

    public void handleProxyLoginFailedEvent(ProxyLoginFailedEvent event) {
        sendEmbedMessage(Embed.builder()
                             .title("Login Failed")
                             .errorColor()
                             .addField("Help", "Try waiting and connecting again.", false));
    }

    public void handleStartConnectEvent(StartConnectEvent event) {
        sendEmbedMessage(Embed.builder()
                             .title("Connecting...")
                             .inQueueColor());
    }

    public void handlePrioStatusUpdateEvent(PrioStatusUpdateEvent event) {
        if (!CONFIG.client.extra.prioStatusChangeMention) return;
        var embedCreateSpec = Embed.builder();
        if (event.prio()) {
            embedCreateSpec
                .title("Prio Queue Status Detected")
                .successColor();
        } else {
            embedCreateSpec
                .title("Prio Queue Status Lost")
                .errorColor();
        }
        embedCreateSpec.addField("User", escape(CONFIG.authentication.username), false);
        sendEmbedMessage((CONFIG.discord.mentionRoleOnPrioUpdate ? mentionAccountOwner() : ""), embedCreateSpec);
    }

    public void handlePrioBanStatusUpdateEvent(PrioBanStatusUpdateEvent event) {
        var embedCreateSpec = Embed.builder();
        if (event.prioBanned()) {
            embedCreateSpec
                .title("Prio Ban Detected")
                .errorColor();
        } else {
            embedCreateSpec
                .title("Prio Unban Detected")
                .successColor();
        }
        embedCreateSpec.addField("User", escape(CONFIG.authentication.username), false);
        sendEmbedMessage((CONFIG.discord.mentionRoleOnPrioBanUpdate ? mentionAccountOwner() : ""), embedCreateSpec);
    }

    public void handleAutoReconnectEvent(final AutoReconnectEvent event) {
        sendEmbedMessage(Embed.builder()
                             .title("AutoReconnecting in " + event.delaySeconds() + "s")
                             .inQueueColor());
    }

    public void handleMsaDeviceCodeLoginEvent(final MsaDeviceCodeLoginEvent event) {
        final var embed = Embed.builder()
            .title("Microsoft Device Code Login")
            .primaryColor()
            .description("Login Here: " + event.deviceCode().getDirectVerificationUri());
        if (CONFIG.discord.mentionRoleOnDeviceCodeAuth)
            sendEmbedMessage(mentionAccountOwner(), embed);
        else
            sendEmbedMessage(embed);
    }

    public void handleDeathMessageEvent(final DeathMessageEvent event) {
        if (!CONFIG.client.extra.killMessage) return;
        event.deathMessageParseResult().killer().ifPresent(killer -> {
            if (!killer.name().equals(CONFIG.authentication.username)) return;
            sendEmbedMessage(Embed.builder()
                                 .title("Kill Detected")
                                 .primaryColor()
                                 .addField("Victim", escape(event.deathMessageParseResult().victim()), false)
                                 .addField("Message", escape(event.deathMessageRaw()), false));
        });
    }

    public void handleUpdateAvailableEvent(final UpdateAvailableEvent event) {
        var embedBuilder = Embed.builder()
            .title("Update Available!")
            .primaryColor();
        event.getVersion().ifPresent(v -> embedBuilder
            .addField("Current", "`" + escape(LAUNCH_CONFIG.version) + "`", false)
            .addField("New", "`" + escape(v) + "`", false));
        embedBuilder.addField(
            "Info",
            "Update will be applied after the next disconnect.\nOr apply now: `update`",
            false);
        sendEmbedMessage(embedBuilder);
    }

    public void handleReplayStartedEvent(final ReplayStartedEvent event) {
        sendEmbedMessage(Embed.builder()
                             .title("Replay Recording Started")
                             .primaryColor());
    }

    public void handleReplayStoppedEvent(final ReplayStoppedEvent event) {
        var embed = Embed.builder()
            .title("Replay Recording Stopped")
            .primaryColor();
        var replayFile = event.replayFile();
        if (replayFile != null && CONFIG.client.extra.replayMod.sendRecordingsToDiscord) {
            try (InputStream in = new BufferedInputStream(new FileInputStream(replayFile))) {
                // 25MB discord file attachment size limit
                if (replayFile.length() > 24 * 1024 * 1024)
                    embed.description("Replay too large to upload to discord: " + (replayFile.length() / (1024 * 1024)) + "mb");
                else
                    embed.fileAttachment(new Embed.FileAttachment(replayFile.getName(), in.readAllBytes()));
            } catch (final Exception e) {
                DISCORD_LOG.error("Failed to read replay file", e);
                embed.description("Error reading replay file: " + e.getMessage());
            }
        }
        sendEmbedMessageWithFileAttachment(embed);
    }

    public void handleTotemPopEvent(final PlayerTotemPopAlertEvent event) {
        var embed = Embed.builder()
            .title("Player Totem Popped")
            .addField("Totems Left", event.totemsRemaining(), false)
            .errorColor();
        if (CONFIG.client.extra.autoTotem.totemPopAlertMention)
            sendEmbedMessage(mentionAccountOwner(), embed);
        else
            sendEmbedMessage(embed);
    }

    public void handleNoTotemsEvent(final NoTotemsEvent event) {
        var embed = Embed.builder()
            .title("Player Out of Totems")
            .errorColor();
        if (CONFIG.client.extra.autoTotem.noTotemsAlertMention)
            sendEmbedMessage(mentionAccountOwner(), embed);
        else
            sendEmbedMessage(embed);
    }

    /**
     * Convenience proxy methods
     */
    public void sendEmbedMessage(Embed embed) {
        bot.sendEmbedMessage(embed);
    }
    public void sendEmbedMessage(String message, Embed embed) {
        bot.sendEmbedMessage(message, embed);
    }
    public void sendMessage(final String message) {
        bot.sendMessage(message);
    }
    public void sendRelayEmbedMessage(Embed embedCreateSpec) {
        bot.sendRelayEmbedMessage(embedCreateSpec);
    }
    public void sendRelayEmbedMessage(String message, Embed embed) {
        bot.sendRelayEmbedMessage(message, embed);
    }
    public void sendRelayMessage(final String message) {
        bot.sendRelayMessage(message);
    }
    void sendEmbedMessageWithButtons(String message, Embed embed, List<Button> buttons, Function<ButtonInteractionEvent, Publisher<Mono<?>>> mapper, Duration timeout) {
        bot.sendEmbedMessageWithButtons(message, embed, buttons, mapper, timeout);
    }
    void sendEmbedMessageWithButtons(Embed embed, List<Button> buttons, Function<ButtonInteractionEvent, Publisher<Mono<?>>> mapper, Duration timeout) {
        bot.sendEmbedMessageWithButtons(embed, buttons, mapper, timeout);
    }
    public void updatePresence(final ClientPresence presence) {
        bot.updatePresence(presence);
    }
    public void sendEmbedMessageWithFileAttachment(Embed embed) {
        bot.sendEmbedMessageWithFileAttachment(embed);
    }
}
