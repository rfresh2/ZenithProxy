package com.zenith.discord;

import com.zenith.Proxy;
import com.zenith.event.chat.DeathMessageChatEvent;
import com.zenith.event.chat.PublicChatEvent;
import com.zenith.event.chat.SystemChatEvent;
import com.zenith.event.chat.WhisperChatEvent;
import com.zenith.event.client.*;
import com.zenith.event.message.DiscordMessageSentEvent;
import com.zenith.event.message.PrivateMessageSendEvent;
import com.zenith.event.module.*;
import com.zenith.event.player.*;
import com.zenith.event.plugin.PluginLoadFailureEvent;
import com.zenith.event.plugin.PluginLoadedEvent;
import com.zenith.event.queue.QueueCompleteEvent;
import com.zenith.event.queue.QueuePositionUpdateEvent;
import com.zenith.event.queue.QueueStartEvent;
import com.zenith.event.server.ClientDeathMessageEvent;
import com.zenith.event.server.ServerPlayerConnectedEvent;
import com.zenith.event.server.ServerPlayerDisconnectedEvent;
import com.zenith.event.server.ServerRestartingEvent;
import com.zenith.event.update.UpdateAvailableEvent;
import com.zenith.event.update.UpdateStartEvent;
import com.zenith.feature.api.fileio.FileIOApi;
import com.zenith.feature.deathmessages.DeathMessageParseResult;
import com.zenith.feature.deathmessages.KillerType;
import com.zenith.feature.player.World;
import com.zenith.feature.queue.Queue;
import com.zenith.module.impl.AntiAFK;
import com.zenith.module.impl.SessionTimeLimit;
import com.zenith.util.DisconnectReasonInfo;
import com.zenith.util.math.MathHelper;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.Color;
import org.geysermc.mcprotocollib.protocol.data.game.PlayerListEntry;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundChatPacket;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static com.github.rfresh2.EventConsumer.of;
import static com.zenith.Globals.*;
import static com.zenith.command.impl.StatusCommand.getCoordinates;
import static com.zenith.discord.DiscordBot.*;
import static com.zenith.util.math.MathHelper.formatDuration;
import static java.util.Arrays.asList;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

public class NotificationEventListener {
    public static final NotificationEventListener INSTANCE = new NotificationEventListener();
    public void subscribeEvents() {
        if (EVENT_BUS.isSubscribed(this)) throw new RuntimeException("Event handlers already initialized");
        EVENT_BUS.subscribe(
            this,
            of(ClientConnectEvent.class, this::handleConnectEvent),
            of(ClientOnlineEvent.class, this::handlePlayerOnlineEvent),
            of(ClientDisconnectEvent.class, this::handleDisconnectEvent),
            of(QueuePositionUpdateEvent.class, this::handleQueuePositionUpdateEvent),
            of(QueueWarningEvent.class, this::handleQueueWarning),
            of(AutoEatOutOfFoodEvent.class, this::handleAutoEatOutOfFoodEvent),
            of(QueueCompleteEvent.class, this::handleQueueCompleteEvent),
            of(QueueStartEvent.class, this::handleStartQueueEvent),
            of(ClientDeathEvent.class, this::handleDeathEvent),
            of(ClientDeathMessageEvent.class, this::handleSelfDeathMessageEvent),
            of(HealthAutoDisconnectEvent.class, this::handleHealthAutoDisconnectEvent),
            of(PlayerConnectedEvent.class, this::handleProxyClientConnectedEvent),
            of(PlayerConnectedEvent.class, this::handleProxyClientConnectedEventCheck2b2tMCVersionMatch),
            of(SpectatorConnectedEvent.class, this::handleProxySpectatorConnectedEvent),
            of(PlayerDisconnectedEvent.class, this::handleProxyClientDisconnectedEvent),
            of(VisualRangeEnterEvent.class, this::handleVisualRangeEnterEvent),
            of(VisualRangeLeaveEvent.class, this::handleVisualRangeLeaveEvent),
            of(VisualRangeLogoutEvent.class, this::handleVisualRangeLogoutEvent),
            of(NonWhitelistedPlayerConnectedEvent.class, this::handleNonWhitelistedPlayerConnectedEvent),
            of(BlacklistedPlayerConnectedEvent.class, this::handleBlacklistedPlayerConnectedEvent),
            of(SpectatorDisconnectedEvent.class, this::handleProxySpectatorDisconnectedEvent),
            of(ActiveHoursConnectEvent.class, this::handleActiveHoursConnectEvent),
            of(DeathMessageChatEvent.class, this::handleDeathMessageChatEventKillMessage),
            of(DeathMessageChatEvent.class, this::handleDeathMessageChatEventChatRelay),
            of(PublicChatEvent.class, this::handlePublicChatEvent),
            of(SystemChatEvent.class, this::handleSystemChatEvent),
            of(WhisperChatEvent.class, this::handleWhisperChatEvent),
            of(ServerPlayerConnectedEvent.class, this::handleServerPlayerConnectedEventChatRelay),
            of(ServerPlayerConnectedEvent.class, this::handleServerPlayerConnectedEventStalk),
            of(ServerPlayerDisconnectedEvent.class, this::handleServerPlayerDisconnectedEventChatRelay),
            of(ServerPlayerDisconnectedEvent.class, this::handleServerPlayerDisconnectedEventStalk),
            of(DiscordMessageSentEvent.class, this::handleDiscordMessageSentEvent),
            of(UpdateStartEvent.class, this::handleUpdateStartEvent),
            of(ServerRestartingEvent.class, this::handleServerRestartingEvent),
            of(ClientLoginFailedEvent.class, this::handleProxyLoginFailedEvent),
            of(ClientStartConnectEvent.class, this::handleStartConnectEvent),
            of(PrioStatusUpdateEvent.class, this::handlePrioStatusUpdateEvent),
            of(PrioBanStatusUpdateEvent.class, this::handlePrioBanStatusUpdateEvent),
            of(AutoReconnectEvent.class, this::handleAutoReconnectEvent),
            of(MsaDeviceCodeLoginEvent.class, this::handleMsaDeviceCodeLoginEvent),
            of(UpdateAvailableEvent.class, this::handleUpdateAvailableEvent),
            of(ReplayStartedEvent.class, this::handleReplayStartedEvent),
            of(ReplayStoppedEvent.class, this::handleReplayStoppedEvent),
            of(PlayerTotemPopAlertEvent.class, this::handleTotemPopEvent),
            of(NoTotemsEvent.class, this::handleNoTotemsEvent),
            of(PluginLoadFailureEvent.class, this::handlePluginLoadFailure),
            of(PluginLoadedEvent.class, this::handlePluginLoadedEvent),
            of(PrivateMessageSendEvent.class, this::handlePrivateMessageSendEvent),
            of(SpawnPatrolTargetAcquiredEvent.class, this::handleSpawnPatrolTargetAcquiredEvent),
            of(SpawnPatrolTargetKilledEvent.class, this::handleSpawnPatrolTargetKilledEvent)
        );
    }

    private void handleSpawnPatrolTargetKilledEvent(SpawnPatrolTargetKilledEvent event) {
        var embed = Embed.builder()
            .title("Target Killed")
            .addField("Target", "[" + event.profile().getName() + "](https://namemc.com/profile/" + event.profile().getId() + ")", false)
            .addField("Death Message", escape(event.message())  , false)
            .thumbnail(Proxy.getInstance().getPlayerBodyURL(event.profile().getId()).toString())
            .successColor();
        sendEmbedMessage(embed);
    }

    private void handleSpawnPatrolTargetAcquiredEvent(SpawnPatrolTargetAcquiredEvent event) {
        var profile = event.targetProfile();
        var embed = Embed.builder()
            .title("Target Acquired")
            .addField("Target", "[" + profile.getName() + "](https://namemc.com/profile/" + profile.getProfileId() + ")", false)
            .addField("Position",getCoordinates(event.target()), false)
            .addField("Our Position", getCoordinates(CACHE.getPlayerCache().getThePlayer()), false)
            .addField("Distance", String.format("%.2f", Math.sqrt(CACHE.getPlayerCache().distanceSqToSelf(event.target()))), false)
            .thumbnail(Proxy.getInstance().getPlayerBodyURL(profile.getProfileId()).toString())
            .primaryColor();
        sendEmbedMessage(embed);
    }

    public void handleConnectEvent(ClientConnectEvent event) {
        var embed = Embed.builder()
            .title("Connected")
            .inQueueColor()
            .addField("Server", CONFIG.client.server.address, true)
            .addField("Proxy IP", CONFIG.server.getProxyAddress(), false);
        if (CONFIG.discord.mentionRoleOnConnect) {
            sendEmbedMessage(notificationMention(), embed);
        } else {
            sendEmbedMessage(embed);
        }
        updatePresence();
    }

    public void handlePlayerOnlineEvent(ClientOnlineEvent event) {
        var embedBuilder = Embed.builder()
            .title("Online")
            .successColor();
        event.queueWait()
            .ifPresent(duration -> embedBuilder.addField("Queue Duration", formatDuration(duration), true));
        if (CONFIG.discord.mentionRoleOnPlayerOnline) {
            sendEmbedMessage(notificationMention(), embedBuilder);
        } else {
            sendEmbedMessage(embedBuilder);
        }
    }

    public void handleDisconnectEvent(ClientDisconnectEvent event) {
        var category = DisconnectReasonInfo.getDisconnectCategory(event.reason());
        var embed = Embed.builder()
            .title("Disconnected")
            .addField("Reason", event.reason(), false)
            .addField("Why?", category.getWikiURL(), false)
            .addField("Category", category.toString(), false)
            .addField("Online Duration", formatDuration(event.onlineDurationWithQueueSkip()), false)
            .errorColor();
        if (Proxy.getInstance().isOn2b2t()
            && !Proxy.getInstance().isPrio()
            && category == DisconnectReasonInfo.DisconnectCategory.KICK) {
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
            } else if (!event.wasInQueue()
                && MathHelper.isInRange( // whether we were kicked at session time limit +- 30s
                    event.onlineDuration().toSeconds(),
                    MODULE.get(SessionTimeLimit.class).getSessionTimeLimit().toSeconds(),
                    30L)) {
                embed.description("""
                        You have likely been kicked for reaching the non-prio session time limit.
                        
                        2b2t kicks non-prio players after %s hours online.
                        """.formatted(MODULE.get(SessionTimeLimit.class).getSessionTimeLimit().toHours()));
            } else if (!event.wasInQueue()
                && MathHelper.isInRange( // whether we were kicked at 20 minutes +- 30s
                     event.onlineDuration().toSeconds(),
                     TimeUnit.MINUTES.toSeconds(20),
                     30L)) {
                String msg = "You have possibly been kicked by 2b2t's AntiAFK plugin";
                if (!MODULE.get(AntiAFK.class).isEnabled()) {
                    msg += "\n\nConsider enabling ZenithProxy's AntiAFK module: `antiAFK on`";
                }
                embed.description(msg);
            }
        }
        if (CONFIG.discord.mentionRoleOnDisconnect) {
            sendEmbedMessage(notificationMention(), embed);
        } else {
            sendEmbedMessage(embed);
        }
        EXECUTOR.execute(this::updatePresence);
    }

    private void handleQueueWarning(QueueWarningEvent event) {
        sendEmbedMessage((event.mention() ? notificationMention() : ""), Embed.builder()
            .title("Queue Warning")
            .addField("Queue Position", "[" + queuePositionStr() + "]", false)
            .inQueueColor());
    }

    public void handleQueuePositionUpdateEvent(QueuePositionUpdateEvent event) {
        updatePresence();
    }

    public void handleAutoEatOutOfFoodEvent(final AutoEatOutOfFoodEvent event) {
        var embed = Embed.builder()
            .title("AutoEat Out Of Food")
            .description("AutoEat threshold met but player has no food")
            .errorColor();
        if (CONFIG.client.extra.autoEat.warningMention) {
            sendEmbedMessage(notificationMention(), embed);
        } else {
            sendEmbedMessage(embed);
        }
    }

    public void handleQueueCompleteEvent(QueueCompleteEvent event) {
        updatePresence();
    }

    public void handleStartQueueEvent(QueueStartEvent event) {
        var embed = Embed.builder()
            .title("Started Queuing")
            .inQueueColor()
            .addField("Regular Queue", Queue.getQueueStatus().regular(), true)
            .addField("Priority Queue", Queue.getQueueStatus().prio(), true);
        if (event.wasOnline()) {
            embed
                .addField("Info", "Detected that the client was kicked to queue", false)
                .addField("Online Duration", formatDuration(event.wasOnlineDuration()), false);
        }
        if (CONFIG.discord.mentionRoleOnStartQueue) {
            sendEmbedMessage(notificationMention(), embed);
        } else {
            sendEmbedMessage(embed);
        }
        updatePresence();
    }

    public void handleDeathEvent(ClientDeathEvent event) {
        var embed = Embed.builder()
            .title("Player Death")
            .errorColor()
            .addField("Coordinates", getCoordinates(CACHE.getPlayerCache().getThePlayer()), false)
            .addField("Dimension", World.getCurrentDimension().name(), false);
        if (CONFIG.discord.mentionRoleOnDeath) {
            sendEmbedMessage(notificationMention(), embed);
        } else {
            sendEmbedMessage(embed);
        }
    }

    public void handleSelfDeathMessageEvent(ClientDeathMessageEvent event) {
        sendEmbedMessage(Embed.builder()
                             .title("Death Message")
                             .errorColor()
                             .addField("Message", event.message(), false));
    }

    public void handleHealthAutoDisconnectEvent(HealthAutoDisconnectEvent event) {
        var embed = Embed.builder()
            .title("Health AutoDisconnect Triggered")
            .addField("Health", CACHE.getPlayerCache().getThePlayer().getHealth(), true)
            .primaryColor();
        if (CONFIG.client.extra.utility.actions.autoDisconnect.mentionOnDisconnect) {
            sendEmbedMessage(notificationMention(), embed);
        } else {
            sendEmbedMessage(embed);
        }
    }

    public void handleProxyClientConnectedEvent(PlayerConnectedEvent event) {
        if (!CONFIG.discord.clientConnectionMessages) return;
        var embed = Embed.builder()
            .title("Client Connected")
            .addField("Username", escape(event.clientGameProfile().getName()), false)
            .addField("MC Version", event.session().getMCVersion(), false)
            .thumbnail(Proxy.getInstance().getPlayerBodyURL(event.clientGameProfile().getId()).toString())
            .primaryColor();
        if (CONFIG.discord.mentionOnClientConnected) {
            sendEmbedMessage(notificationMention(), embed);
        } else {
            sendEmbedMessage(embed);
        }
    }

    public void handleProxyClientConnectedEventCheck2b2tMCVersionMatch(PlayerConnectedEvent event) {
        if (!CONFIG.discord.mcVersionMismatchWarning) return;
        if (!Proxy.getInstance().isOn2b2t() || !Proxy.getInstance().isConnected()) return;
        var client = Proxy.getInstance().getClient();
        if (client == null) return;
        var clientProtocolVersion = client.getProtocolVersion();
        var playerProtocolVersion = event.session().getProtocolVersion();
        if (!clientProtocolVersion.equalTo(playerProtocolVersion)) {
            var embed = Embed.builder()
                .title("MC Version Mismatch")
                .description("""
                     **Client MC Version**: %s
                     **ZenithProxy Client MC Version**: %s
                     
                     It is recommended to use the same MC version as the ZenithProxy client.
                     
                     Otherwise you may experience issues with 2b2t's anti-cheat, which changes its checks based on client MC version.
                     
                     To configure ZenithProxy's client ViaVersion: `via zenithToServer version <version>`
                     """.formatted(playerProtocolVersion.getName(), clientProtocolVersion.getName()))
                .errorColor();
            sendEmbedMessage(embed);
        }
    }

    public void handleProxySpectatorConnectedEvent(SpectatorConnectedEvent event) {
        if (!CONFIG.discord.clientConnectionMessages) return;
        var embed = Embed.builder()
            .title("Spectator Connected")
            .addField("Username", escape(event.clientGameProfile().getName()), false)
            .addField("MC Version", event.session().getMCVersion(), false)
            .thumbnail(Proxy.getInstance().getPlayerBodyURL(event.clientGameProfile().getId()).toString())
            .primaryColor();
        if (CONFIG.discord.mentionOnSpectatorConnected) {
            sendEmbedMessage(notificationMention(), embed);
        } else {
            sendEmbedMessage(embed);
        }
    }

    public void handleProxyClientDisconnectedEvent(PlayerDisconnectedEvent event) {
        if (!CONFIG.discord.clientConnectionMessages) return;
        var embed = Embed.builder()
            .title("Client Disconnected")
            .errorColor();
        if (nonNull(event.clientGameProfile())) {
            embed = embed.addField("Username", escape(event.clientGameProfile().getName()), false);
        }
        if (nonNull(event.reason())) {
            embed = embed.addField("Reason", escape(event.reason()), false);
        }
        if (CONFIG.discord.mentionOnClientDisconnected) {
            sendEmbedMessage(notificationMention(), embed);
        } else {
            sendEmbedMessage(embed);
        }
    }

    public void handleVisualRangeEnterEvent(VisualRangeEnterEvent event) {
        var embedCreateSpec = Embed.builder()
            .title("Player In Visual Range")
            .color(event.isFriend() ? CONFIG.theme.success.color() : CONFIG.theme.error.color())
            .addField("Player Name", escape(event.playerEntry().getName()), true)
            .addField("Player UUID", ("[" + event.playerEntry().getProfileId() + "](https://namemc.com/profile/" + event.playerEntry().getProfileId() + ")"), true)
            .thumbnail(Proxy.getInstance().getPlayerBodyURL(event.playerEntry().getProfileId()).toString());

        if (CONFIG.discord.reportCoords) {
            embedCreateSpec.addField("Coordinates", "||["
                + (int) event.playerEntity().getX() + ", "
                + (int) event.playerEntity().getY() + ", "
                + (int) event.playerEntity().getZ()
                + "]||", false);
        }
        final String buttonId = "addFriend" + ThreadLocalRandom.current().nextInt(1000000);
        final List<Button> buttons = asList(Button.primary(buttonId, "Add Friend"));
        final Consumer<ButtonInteractionEvent> mapper = e -> {
            if (e.getComponentId().equals(buttonId)) {
                DISCORD_LOG.info("{} added friend: {} [{}]",
                                 Optional.ofNullable(e.getInteraction().getMember())
                                     .map(m -> m.getUser().getName())
                                     .orElse("Unknown"),
                                 event.playerEntry().getName(),
                                 event.playerEntry().getProfileId());
                PLAYER_LISTS.getFriendsList().add(event.playerEntry().getName());
                e.replyEmbeds(Embed.builder()
                                         .title("Friend Added")
                                         .successColor()
                                         .addField("Player Name", escape(event.playerEntry().getName()), true)
                                         .addField("Player UUID", ("[" + event.playerEntry().getProfileId() + "](https://namemc.com/profile/" + event.playerEntry().getProfileId() + ")"), true)
                                         .thumbnail(Proxy.getInstance().getPlayerBodyURL(event.playerEntry().getProfileId()).toString())
                                         .toJDAEmbed())
                    .complete();
                saveConfigAsync();
            }
        };
        if (CONFIG.client.extra.visualRange.enterAlertMention)
            if (!event.isFriend())
                sendEmbedMessageWithButtons(notificationMention(), embedCreateSpec, buttons, mapper, Duration.ofHours(1));
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
            .color(event.isFriend() ? CONFIG.theme.success.color() : CONFIG.theme.error.color())
            .addField("Player Name", escape(event.playerEntry().getName()), true)
            .addField("Player UUID", ("[" + event.playerEntity().getUuid() + "](https://namemc.com/profile/" + event.playerEntry().getProfileId() + ")"), true)
            .thumbnail(Proxy.getInstance().getPlayerBodyURL(event.playerEntity().getUuid()).toString());

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
            .color(event.isFriend() ? CONFIG.theme.success.color() : CONFIG.theme.error.color())
            .addField("Player Name", escape(event.playerEntry().getName()), true)
            .addField("Player UUID", ("[" + event.playerEntity().getUuid() + "](https://namemc.com/profile/" + event.playerEntry().getProfileId() + ")"), true)
            .thumbnail(Proxy.getInstance().getPlayerBodyURL(event.playerEntity().getUuid()).toString());

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
        var embed = Embed.builder()
            .title("Non-Whitelisted Player Disconnected")
            .errorColor();
        if (nonNull(event.remoteAddress()) && CONFIG.discord.showNonWhitelistLoginIP) {
            embed = embed.addField("IP", escape(event.remoteAddress().toString()), false);
        }
        if (nonNull(event.gameProfile()) && nonNull(event.gameProfile().getId()) && nonNull(event.gameProfile().getName())) {
            embed
                .addField("Username", escape(event.gameProfile().getName()), false)
                .addField("Player UUID", ("[" + event.gameProfile().getId().toString() + "](https://namemc.com/profile/" + event.gameProfile().getId().toString() + ")"), true)
                .thumbnail(Proxy.getInstance().getPlayerBodyURL(event.gameProfile().getId()).toString());
            final String buttonId = "whitelist" + ThreadLocalRandom.current().nextInt(10000000);
            final List<Button> buttons = asList(Button.primary(buttonId, "Whitelist Player"));
            final Consumer<ButtonInteractionEvent> mapper = e -> {
                if (e.getComponentId().equals(buttonId)) {
                    if (validateButtonInteractionEventFromAccountOwner(e)) {
                        DISCORD_LOG.info("{} whitelisted {} [{}]",
                                         Optional.ofNullable(e.getInteraction().getMember()).map(m -> m.getUser().getName()).orElse("Unknown"),
                                         event.gameProfile().getName(),
                                         event.gameProfile().getId().toString());
                        PLAYER_LISTS.getWhitelist().add(event.gameProfile().getName());
                        e.replyEmbeds(Embed.builder()
                                                 .title("Player Whitelisted")
                                                 .successColor()
                                                 .addField("Player Name", escape(event.gameProfile().getName()), true)
                                                 .addField("Player UUID", ("[" + event.gameProfile().getId().toString() + "](https://namemc.com/profile/" + event.gameProfile().getId().toString() + ")"), true)
                                                 .thumbnail(Proxy.getInstance().getPlayerBodyURL(event.gameProfile().getId()).toString())
                                                 .toJDAEmbed()).complete();
                        saveConfigAsync();
                    } else {
                        DISCORD_LOG.error("{} attempted to whitelist {} [{}] but was not authorized to do so!",
                                          Optional.ofNullable(e.getInteraction().getMember()).map(m -> m.getUser().getName()).orElse("Unknown"),
                                          event.gameProfile().getName(),
                                          event.gameProfile().getId().toString());
                        e.replyEmbeds(Embed.builder()
                                                 .title("Not Authorized!")
                                                 .errorColor()
                                                 .addField("Error",
                                                           "User: " + Optional.ofNullable(e.getInteraction().getMember()).map(m -> m.getUser().getName()).orElse("Unknown")
                                                               + " is not authorized to execute this command! Contact the account owner", true)
                                                 .toJDAEmbed()).complete();
                    }
                }
            };
            sendEmbedMessageWithButtons(embed, buttons, mapper, Duration.ofHours(1L));
        } else { // shouldn't be possible if verifyUsers is enabled
            if (nonNull(event.gameProfile())) {
                embed
                    .addField("Username", escape(event.gameProfile().getName()), false);
            }
            if (CONFIG.discord.mentionOnNonWhitelistedClientConnected) {
                sendEmbedMessage(notificationMention(), embed);
            } else {
                sendEmbedMessage(embed);
            }
        }
    }

    private void handleBlacklistedPlayerConnectedEvent(BlacklistedPlayerConnectedEvent event) {
        var embed = Embed.builder()
            .title("Blacklisted Player Disconnected")
            .errorColor();
        if (nonNull(event.remoteAddress()) && CONFIG.discord.showNonWhitelistLoginIP) {
            embed = embed.addField("IP", escape(event.remoteAddress().toString()), false);
        }
        if (nonNull(event.gameProfile()) && nonNull(event.gameProfile().getId()) && nonNull(event.gameProfile().getName())) {
            embed
                .addField("Username", escape(event.gameProfile().getName()), false)
                .addField("Player UUID", ("[" + event.gameProfile().getId().toString() + "](https://namemc.com/profile/" + event.gameProfile().getId().toString() + ")"), true)
                .thumbnail(Proxy.getInstance().getPlayerBodyURL(event.gameProfile().getId()).toString());
        }
        if (CONFIG.discord.mentionOnNonWhitelistedClientConnected) {
            sendEmbedMessage(notificationMention(), embed);
        } else {
            sendEmbedMessage(embed);
        }
    }

    public void handleProxySpectatorDisconnectedEvent(SpectatorDisconnectedEvent event) {
        if (!CONFIG.discord.clientConnectionMessages) return;
        var embed = Embed.builder()
            .title("Spectator Disconnected")
            .errorColor();
        if (nonNull(event.clientGameProfile())) {
            embed = embed.addField("Username", escape(event.clientGameProfile().getName()), false);
        }
        if (CONFIG.discord.mentionOnSpectatorDisconnected) {
            sendEmbedMessage(notificationMention(), embed);
        } else {
            sendEmbedMessage(embed);
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

    private void handleWhisperChatEvent(WhisperChatEvent event) {
        if (!CONFIG.discord.chatRelay.whispers) return;
        if (!CONFIG.discord.chatRelay.enable || CONFIG.discord.chatRelay.channelId.isEmpty()) return;
        if (CONFIG.discord.chatRelay.ignoreQueue && Proxy.getInstance().isInQueue()) return;
        try {
            String message = event.message();
            String ping = "";
            if (CONFIG.discord.chatRelay.mentionWhileConnected || isNull(Proxy.getInstance().getCurrentPlayer().get())) {
                if (CONFIG.discord.chatRelay.mentionRoleOnWhisper && !event.outgoing()) {
                    if (!message.toLowerCase(Locale.ROOT).contains("discord.gg/")
                        && !PLAYER_LISTS.getIgnoreList().contains(event.sender().getName())) {
                        ping = notificationMention();
                    }
                }
            }
            message = message.replace(event.sender().getName(), "**" + event.sender().getName() + "**");
            message = message.replace(event.receiver().getName(), "**" + event.receiver().getName() + "**");
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
        if (event.isUnresolvedPublicChat())
        {
            if (!CONFIG.discord.chatRelay.publicChats) return;
        } else if (event.isUnresolvedWhisper())
        {
            if (!CONFIG.discord.chatRelay.whispers) return;
        }
        else if (!CONFIG.discord.chatRelay.serverMessages) return;

        if (!CONFIG.discord.chatRelay.enable || CONFIG.discord.chatRelay.channelId.isEmpty()) return;
        if (CONFIG.discord.chatRelay.ignoreQueue && Proxy.getInstance().isInQueue()) return;
        try {
            String message = event.message();
            final String avatarURL = Proxy.getInstance().isOn2b2t() ? Proxy.getInstance().getPlayerHeadURL("Hausemaster").toString() : null;

            var color = Color.MOON_YELLOW;
            String ping = "";

            if (event.isUnresolvedPublicChat()) color = Color.BLACK;
            if (event.isUnresolvedWhisper()) color = Color.MAGENTA;

            if (event.isUnresolvedIncomingWhisper() && CONFIG.discord.chatRelay.mentionRoleOnWhisper 
                && !CONFIG.discord.chatRelay.mentionResolvedOnly)
            {
                if (CONFIG.discord.chatRelay.mentionWhileConnected || isNull(Proxy.getInstance().getCurrentPlayer().get()))
                {
                    ping = notificationMention();
                }
            }

            var embed = Embed.builder()
                .description(escape(message))
                .footer("\u200b", avatarURL)
                .color(color);
            sendRelayEmbedMessage(ping, embed);

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
            boolean customSenderFormatting = false;
            Color color = Color.BLACK;
            if (!event.isDefaultMessageSchema()) {
                if (Proxy.getInstance().isOn2b2t()) {
                    DISCORD_LOG.error("Received non-default schema chat message on 2b2t: {}", message);
                }
            } else {
                message = event.extractMessageDefaultSchema();
                customSenderFormatting = true;
                if (message.startsWith(">")) color = Color.MEDIUM_SEA_GREEN;
            }
            String ping = "";
            if (CONFIG.discord.chatRelay.mentionWhileConnected || isNull(Proxy.getInstance().getCurrentPlayer().get())) {
                if (CONFIG.discord.chatRelay.mentionRoleOnNameMention
                    && event.sender().getName().equals(CONFIG.authentication.username)
                    && !PLAYER_LISTS.getIgnoreList().contains(event.sender().getName())
                    && Arrays.asList(message.toLowerCase().split(" ")).contains(CONFIG.authentication.username.toLowerCase())) {
                    ping = notificationMention();
                }
            }
            if (customSenderFormatting) {
                message = "**" + event.sender().getName() + ":** " + message;
            }
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

    private void handleDeathMessageChatEventKillMessage(DeathMessageChatEvent event) {
        if (!CONFIG.client.extra.killMessage) return;
        event.deathMessage().killer().ifPresent(killer -> {
            if (!killer.name().equals(CONFIG.authentication.username)) return;
            sendEmbedMessage(Embed.builder()
                                 .title("Kill Detected")
                                 .primaryColor()
                                 .addField("Victim", escape(event.deathMessage().victim()), false)
                                 .addField("Message", escape(event.message()), false)
                                 .thumbnail(Proxy.getInstance().getPlayerHeadURL(event.deathMessage().victim()).toString()));
        });
    }

    private void handleDeathMessageChatEventChatRelay(DeathMessageChatEvent event) {
        if (!CONFIG.discord.chatRelay.deathMessages) return;
        if (!CONFIG.discord.chatRelay.enable || CONFIG.discord.chatRelay.channelId.isEmpty()) return;
        if (CONFIG.discord.chatRelay.ignoreQueue && Proxy.getInstance().isInQueue()) return;
        try {
            String message = event.message();
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

    public void handleServerPlayerConnectedEventChatRelay(ServerPlayerConnectedEvent event) {
        if (!CONFIG.discord.chatRelay.enable || !CONFIG.discord.chatRelay.connectionMessages || CONFIG.discord.chatRelay.channelId.isEmpty()) return;
        if (!Proxy.getInstance().isOnlineForAtLeastDuration(Duration.ofSeconds(3))) return;
        if (CONFIG.discord.chatRelay.ignoreQueue && Proxy.getInstance().isInQueue()) return;
        sendRelayEmbedMessage(Embed.builder()
                                  .description(escape("**" + event.playerEntry().getName() + "** connected"))
                                  .successColor()
                                  .footer("\u200b", Proxy.getInstance().getPlayerHeadURL(event.playerEntry().getProfileId()).toString()));
    }

    public void handleServerPlayerConnectedEventStalk(ServerPlayerConnectedEvent event) {
        if (!CONFIG.client.extra.stalk.enabled || !PLAYER_LISTS.getStalkList().contains(event.playerEntry().getProfile())) return;
        sendEmbedMessage(notificationMention(), Embed.builder()
            .title("Stalked Player Online!")
            .successColor()
            .addField("Player Name", event.playerEntry().getName(), true)
            .thumbnail(Proxy.getInstance().getPlayerBodyURL(event.playerEntry().getProfileId()).toString()));
    }

    public void handleServerPlayerDisconnectedEventChatRelay(ServerPlayerDisconnectedEvent event) {
        if (!CONFIG.discord.chatRelay.enable || !CONFIG.discord.chatRelay.connectionMessages || CONFIG.discord.chatRelay.channelId.isEmpty()) return;
        if (!Proxy.getInstance().isOnlineForAtLeastDuration(Duration.ofSeconds(3))) return;
        if (CONFIG.discord.chatRelay.ignoreQueue && Proxy.getInstance().isInQueue()) return;
        sendRelayEmbedMessage(Embed.builder()
                                  .description(escape("**" + event.playerEntry().getName() + "** disconnected"))
                                  .errorColor()
                                  .footer("\u200b", Proxy.getInstance().getPlayerHeadURL(event.playerEntry().getProfileId()).toString()));
    }

    public void handleServerPlayerDisconnectedEventStalk(ServerPlayerDisconnectedEvent event) {
        if (!CONFIG.client.extra.stalk.enabled || !PLAYER_LISTS.getStalkList().contains(event.playerEntry().getProfile())) return;
        sendEmbedMessage(notificationMention(), Embed.builder()
            .title("Stalked Player Offline!")
            .errorColor()
            .addField("Player Name", event.playerEntry().getName(), true)
            .thumbnail(Proxy.getInstance().getPlayerBodyURL(event.playerEntry().getProfileId()).toString()));
    }

    public void handleDiscordMessageSentEvent(DiscordMessageSentEvent event) {
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
                if (DISCORD.jda.getSelfUser().getIdLong() != messageData.getAuthor().getIdLong()) return;
                final MessageEmbed embed = messageData.getEmbeds().getFirst();
                if (embed.getColor() != null && embed.getColor().getRGB() == PRIVATE_MESSAGE_EMBED_COLOR.getRGB()) {
                    // replying to private message
                    sendPrivateMessage(event.message(), event.event());
                } else {
                    final String sender = DISCORD.extractRelayEmbedSenderUsername(embed.getColor(), embed.getDescription());
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
                    if (!pm) Proxy.getInstance().getClient().sendAsync(new ServerboundChatPacket("/w " + sender + " " + event.message()));
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

    private void sendPrivateMessage(String message, MessageReceivedEvent event) {
        EVENT_BUS.postAsync(new PrivateMessageSendEvent(
            event.getMessage().getAuthor().getName(),
            message));
    }

    public void handleUpdateStartEvent(UpdateStartEvent event) {
        sendEmbedMessage(DISCORD.getUpdateMessage(event.newVersion()));
    }

    public void handleServerRestartingEvent(ServerRestartingEvent event) {
        var embed = Embed.builder()
            .title("Server Restarting")
            .errorColor()
            .addField("Message", event.message(), true);
        if (CONFIG.discord.mentionRoleOnServerRestart) {
            sendEmbedMessage(notificationMention(), embed);
        } else {
            sendEmbedMessage(embed);
        }
    }

    public void handleProxyLoginFailedEvent(ClientLoginFailedEvent event) {
        var embed = Embed.builder()
            .title("Login Failed")
            .errorColor()
            .addField("Help", "Try waiting and connecting again.", false);
        if (CONFIG.discord.mentionRoleOnLoginFailed) {
            sendEmbedMessage(notificationMention(), embed);
        } else {
            sendEmbedMessage(embed);
        }
    }

    public void handleStartConnectEvent(ClientStartConnectEvent event) {
        sendEmbedMessage(Embed.builder()
                             .title("Connecting...")
                             .inQueueColor());
    }

    public void handlePrioStatusUpdateEvent(PrioStatusUpdateEvent event) {
        if (!CONFIG.client.extra.prioStatusChangeMention) return;
        var embed = Embed.builder();
        if (event.prio()) {
            embed
                .title("Prio Queue Status Detected")
                .successColor();
        } else {
            embed
                .title("Prio Queue Status Lost")
                .errorColor();
        }
        embed.addField("User", escape(CONFIG.authentication.username), false);
        if (CONFIG.discord.mentionRoleOnPrioUpdate) {
            sendEmbedMessage(notificationMention(), embed);
        } else {
            sendEmbedMessage(embed);
        }
    }

    public void handlePrioBanStatusUpdateEvent(PrioBanStatusUpdateEvent event) {
        var embed = Embed.builder();
        if (event.prioBanned()) {
            embed
                .title("Prio Ban Detected")
                .errorColor();
        } else {
            embed
                .title("Prio Unban Detected")
                .successColor();
        }
        embed.addField("User", escape(CONFIG.authentication.username), false);
        if (CONFIG.discord.mentionRoleOnPrioBanUpdate) {
            sendEmbedMessage(notificationMention(), embed);
        } else {
            sendEmbedMessage(embed);
        }
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
            .description("Login Here: " + event.deviceCode().getDirectVerificationUri() + " \nCode: " + event.deviceCode().getUserCode());
        if (CONFIG.discord.mentionRoleOnDeviceCodeAuth)
            sendEmbedMessage(notificationMention(), embed);
        else
            sendEmbedMessage(embed);
    }

    public void handleUpdateAvailableEvent(final UpdateAvailableEvent event) {
        var embed = Embed.builder()
            .title("Update Available!")
            .primaryColor();
        event.getVersion().ifPresent(v -> embed
            .addField("Current", "`" + escape(LAUNCH_CONFIG.version) + "`", false)
            .addField("New", "`" + escape(v) + "`", false));
        embed.addField(
            "Info",
            "Update will be applied after the next disconnect.\nOr apply now: `update`",
            false);
        sendEmbedMessage(embed);
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
                // 10mb discord file attachment size limit
                long replaySizeMb = replayFile.length() / (1024 * 1024);
                if (replaySizeMb > 10) {
                    if (CONFIG.client.extra.replayMod.fileIOUploadIfTooLarge) {
                        DISCORD_LOG.info("Uploading large replay to file.io with size: {}", replayFile.length());
                        var notiEmbed = Embed.builder()
                            .title("Replay Recording Stopped")
                            .description("Replay file too large to upload directly to discord: " + replaySizeMb + "mb\nUpload to file.io in progress...")
                            .inQueueColor();
                        sendEmbedMessage(notiEmbed);
                        var fileIOResponse = FileIOApi.INSTANCE.uploadFile(replayFile.getName(), in);
                        if (fileIOResponse.isEmpty() || !fileIOResponse.get().success()) {
                            embed.description("Failed uploading to file.io and replay too large to upload to discord: " + replaySizeMb + "mb");
                        } else {
                            embed.description("Download `" + replayFile.getName() + "`: " + fileIOResponse.get().link());
                        }
                    } else {
                        embed.description("Replay too large to upload to discord: " + replaySizeMb + "mb");
                    }
                } else {
                    embed.fileAttachment(new Embed.FileAttachment(replayFile.getName(), in.readAllBytes()));
                }
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
            sendEmbedMessage(notificationMention(), embed);
        else
            sendEmbedMessage(embed);
    }

    public void handleNoTotemsEvent(final NoTotemsEvent event) {
        var embed = Embed.builder()
            .title("Player Out of Totems")
            .errorColor();
        if (CONFIG.client.extra.autoTotem.noTotemsAlertMention)
            sendEmbedMessage(notificationMention(), embed);
        else
            sendEmbedMessage(embed);
    }

    static final Color PRIVATE_MESSAGE_EMBED_COLOR = Color.RED;

    public void handlePrivateMessageSendEvent(final PrivateMessageSendEvent event) {
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

    private void handlePluginLoadFailure(PluginLoadFailureEvent event) {
        String id = event.id() != null ? event.id() : "?";
        var embed = Embed.builder()
            .title("Plugin Load Failure")
            .errorColor()
            .description("Error: " + escape(event.message()))
            .addField("Plugin ID", escape(id), false)
            .addField("Plugin Jar", escape(event.jarPath().getFileName().toString()), false);
        sendEmbedMessage(embed);
    }

    private void handlePluginLoadedEvent(PluginLoadedEvent event) {
        var embed = Embed.builder()
            .title("Plugin Loaded")
            .successColor()
            .addField("ID", escape(event.pluginInfo().id()), false)
            .addField("Description", escape(event.pluginInfo().description()))
            .addField("Version", escape(event.pluginInfo().version()), false)
            .addField("URL", escape(event.pluginInfo().url()), false)
            .addField("Author(s)", String.join(", ", event.pluginInfo().authors()), false);
        sendEmbedMessage(embed);
    }

    /**
     * Convenience proxy methods
     */
    public void sendEmbedMessage(Embed embed) {
        DISCORD.sendEmbedMessage(embed);
    }
    public void sendEmbedMessage(String message, Embed embed) {
        DISCORD.sendEmbedMessage(message, embed);
    }
    public void sendMessage(final String message) {
        DISCORD.sendMessage(message);
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
    void sendEmbedMessageWithButtons(String message, Embed embed, List<Button> buttons, Consumer<ButtonInteractionEvent> mapper, Duration timeout) {
        DISCORD.sendEmbedMessageWithButtons(message, embed, buttons, mapper, timeout);
    }
    void sendEmbedMessageWithButtons(Embed embed, List<Button> buttons, Consumer<ButtonInteractionEvent> mapper, Duration timeout) {
        DISCORD.sendEmbedMessageWithButtons(embed, buttons, mapper, timeout);
    }
    public void updatePresence(final OnlineStatus onlineStatus, final Activity activity) {
        DISCORD.updatePresence(onlineStatus, activity);
    }
    public void updatePresence() {
        DISCORD.updatePresence();
    }
    public void sendEmbedMessageWithFileAttachment(Embed embed) {
        DISCORD.sendEmbedMessageWithFileAttachment(embed);
    }
}
