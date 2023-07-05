package com.zenith.discord;

import com.collarmc.pounce.Subscribe;
import com.github.steveice10.mc.protocol.packet.ingame.client.ClientChatPacket;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.zenith.Proxy;
import com.zenith.command.CommandContext;
import com.zenith.command.DiscordCommandContext;
import com.zenith.event.module.AntiAfkStuckEvent;
import com.zenith.event.module.AutoEatOutOfFoodEvent;
import com.zenith.event.proxy.*;
import com.zenith.util.Queue;
import discord4j.common.util.Snowflake;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.entity.User;
import discord4j.core.object.presence.ClientActivity;
import discord4j.core.object.presence.ClientPresence;
import discord4j.core.object.presence.Status;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.discordjson.json.ImmutableUserModifyRequest;
import discord4j.discordjson.json.MessageCreateRequest;
import discord4j.rest.RestClient;
import discord4j.rest.entity.RestChannel;
import discord4j.rest.util.Color;
import discord4j.rest.util.MultipartRequest;
import lombok.Getter;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

import static com.zenith.command.impl.StatusCommand.getCoordinates;
import static com.zenith.util.Constants.*;
import static java.util.Arrays.asList;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

public class DiscordBot {

    private RestClient restClient;
    private final Supplier<RestChannel> mainRestChannel = Suppliers.memoize(() -> restClient.getChannelById(Snowflake.of(CONFIG.discord.channelId)));
    private final Supplier<RestChannel> relayRestChannel = Suppliers.memoize(() -> restClient.getChannelById(Snowflake.of(CONFIG.discord.chatRelay.channelId)));
    private GatewayDiscordClient client;
    private Proxy proxy;
    // Main channel discord message FIFO queue
    private final ConcurrentLinkedQueue<MultipartRequest<MessageCreateRequest>> mainChannelMessageQueue;
    private final ConcurrentLinkedQueue<MessageCreateRequest> relayChannelMessageQueue;
    private static final ClientPresence DISCONNECTED_PRESENCE = ClientPresence.of(Status.DO_NOT_DISTURB, ClientActivity.playing("Disconnected"));
    private static final ClientPresence DEFAULT_CONNECTED_PRESENCE = ClientPresence.of(Status.ONLINE, ClientActivity.playing((CONFIG.client.server.address.toLowerCase().endsWith("2b2t.org") ? "2b2t" : CONFIG.client.server.address)));
    public Optional<Instant> lastRelaymessage = Optional.empty();

    @Getter
    private boolean isRunning;

    public DiscordBot() {
        this.mainChannelMessageQueue = new ConcurrentLinkedQueue<>();
        this.relayChannelMessageQueue = new ConcurrentLinkedQueue<>();
        this.isRunning = false;
    }

    public void start() {
        this.client = DiscordClientBuilder.create(CONFIG.discord.token)
                .build()
                .gateway()
                .setInitialPresence(shardInfo -> DISCONNECTED_PRESENCE)
                .login()
                .block();
        EVENT_BUS.subscribe(this);

        restClient = client.getRestClient();

        client.getEventDispatcher().on(MessageCreateEvent.class).subscribe(event -> {
            if (CONFIG.discord.chatRelay.channelId.length() > 0 && event.getMessage().getChannelId().equals(Snowflake.of(CONFIG.discord.chatRelay.channelId))) {
                if (!event.getMember().get().getId().equals(this.client.getSelfId())) {
                    EVENT_BUS.dispatch(new DiscordMessageSentEvent(sanitizeRelayInputMessage(event.getMessage().getContent())));
                    return;
                }
            }
            if (!event.getMessage().getChannelId().equals(Snowflake.of(CONFIG.discord.channelId))) {
                return;
            }
            final String message = event.getMessage().getContent();
            if (!message.startsWith(CONFIG.discord.prefix)) {
                return;
            }
            try {
                final String inputMessage = message.substring(1);
                DISCORD_LOG.info(event.getMember().map(User::getTag).orElse("unknown user") + " (" + event.getMember().get().getId().asString() +") executed discord command: {}", inputMessage);
                final CommandContext context = DiscordCommandContext.create(inputMessage, event, mainRestChannel.get());
                COMMAND_MANAGER.execute(context);
                final MultipartRequest<MessageCreateRequest> request = commandEmbedOutputToMessage(context);
                if (request != null) {
                    DISCORD_LOG.debug("Discord bot response: {}", request.getJsonPayload());
                    mainChannelMessageQueue.add(request);
                }
                if (!context.getMultiLineOutput().isEmpty()) {
                    for (final String line : context.getMultiLineOutput()) {
                        mainChannelMessageQueue.add(MessageCreateSpec.builder().content(line).build().asRequest());
                    }
                }
            } catch (final Exception e) {
                DISCORD_LOG.error("Failed processing discord command: {}", message, e);
            }
        });

        if (CONFIG.discord.isUpdating) {
            handleProxyUpdateComplete();
        }
        SCHEDULED_EXECUTOR_SERVICE.scheduleAtFixedRate(this::updatePresence, 0L,
                15L, // discord rate limit
                TimeUnit.SECONDS);
        SCHEDULED_EXECUTOR_SERVICE.scheduleAtFixedRate(this::processMessageQueue, 0L, 100L, TimeUnit.MILLISECONDS);
        SCHEDULED_EXECUTOR_SERVICE.scheduleAtFixedRate(this::processRelayMessageQueue, 0L, 100L, TimeUnit.MILLISECONDS);
        this.isRunning = true;
    }

    private MultipartRequest<MessageCreateRequest> commandEmbedOutputToMessage(final CommandContext context) {
        EmbedCreateSpec embedCreateSpec = context.getEmbedBuilder().build();
        if (!embedCreateSpec.isTitlePresent()) {
            return null;
        }
        return MessageCreateSpec.builder()
                .addEmbed(embedCreateSpec)
                .build().asRequest();
    }

    private void processMessageQueue() {
        try {
            MultipartRequest<MessageCreateRequest> message = mainChannelMessageQueue.poll();
            if (nonNull(message)) {
                this.mainRestChannel.get().createMessage(message).block();
            }
        } catch (final Throwable e) {
            DISCORD_LOG.error("Message processor error", e);
        }
    }

    private void processRelayMessageQueue() {
        try {
            MessageCreateRequest message = relayChannelMessageQueue.poll();
            if (nonNull(message)) {
                this.relayRestChannel.get().createMessage(message).block();
            }
        } catch (final Throwable e) {
            DISCORD_LOG.error("Message processor error", e);
        }
    }

    private void updatePresence() {
        if (this.proxy.isInQueue()) {
            this.client.updatePresence(getQueuePresence()).block();
        } else if (this.proxy.isConnected()) {
            this.client.updatePresence(getOnlinePresence()).block();
        } else {
            this.client.updatePresence(DISCONNECTED_PRESENCE).block();
        }
    }

    private ClientPresence getOnlinePresence() {
        long onlineSeconds = Instant.now().getEpochSecond() - this.proxy.getConnectTime().getEpochSecond();
        return ClientPresence.of(Status.ONLINE, ClientActivity.playing((CONFIG.client.server.address.toLowerCase().endsWith("2b2t.org") ? "2b2t" : CONFIG.client.server.address) + " [" + Queue.getEtaStringFromSeconds(onlineSeconds) + "]"));
    }

    private void handleProxyUpdateComplete() {
        CONFIG.discord.isUpdating = false;
        saveConfig();
        sendEmbedMessage(EmbedCreateSpec.builder()
                .title("Update complete!")
                .color(Color.CYAN)
                .build());
    }

    @Subscribe
    public void handleConnectEvent(ConnectEvent event) {
        sendEmbedMessage(EmbedCreateSpec.builder()
                .title("Proxy Connected")
                .color(Color.CYAN)
                .addField("Server", CONFIG.client.server.address, true)
                .addField("Proxy IP", CONFIG.server.getProxyAddress(), false)
                .build());
        this.client.updatePresence(DEFAULT_CONNECTED_PRESENCE).block();
    }

    @Subscribe
    public void handlePlayerOnlineEvent(PlayerOnlineEvent event) {
        sendEmbedMessage(EmbedCreateSpec.builder()
                .title("Proxy Online")
                .color(Color.CYAN)
                .build());
    }

    @Subscribe
    public void handleDisconnectEvent(DisconnectEvent event) {
        boolean sus = event.reason.startsWith("Login failed: Authentication error: Your account has been suspended for the next ");
        sendEmbedMessage((sus ? "<@&" + CONFIG.discord.accountOwnerRoleId + ">" : ""), EmbedCreateSpec.builder()
                .title("Proxy Disconnected")
                .addField("Reason", event.reason, true)
                .color(Color.CYAN)
                .build());
        SCHEDULED_EXECUTOR_SERVICE.submit(() -> this.client.updatePresence(DISCONNECTED_PRESENCE).block());
        if (sus) { Proxy.getInstance().cancelAutoReconnect(); }
    }

    @Subscribe
    public void handleQueuePositionUpdateEvent(QueuePositionUpdateEvent event) {
        if (CONFIG.discord.queueWarning.enabled) {
            if (event.position == CONFIG.discord.queueWarning.position) {
                sendQueueWarning();
            } else if (event.position <= 3) {
                sendQueueWarning();
            }
        }
        this.client.updatePresence(getQueuePresence()).block();
    }

    @Subscribe
    public void handleAutoEatOutOfFood(final AutoEatOutOfFoodEvent event) {
        sendEmbedMessage(EmbedCreateSpec.builder()
                .title("AutoEat Out Of Food")
                .description("AutoEat threshold met but player has no food")
                .color(Color.RUBY)
                .build());
    }

    private void sendQueueWarning() {
        sendEmbedMessage((CONFIG.discord.queueWarning.mentionRole ? "<@&" + CONFIG.discord.accountOwnerRoleId + ">" : ""), EmbedCreateSpec.builder()
                .title("Queue Warning")
                .addField("Queue Position", "[" + queuePositionStr() + "]", false)
                .color(Color.MOON_YELLOW)
                .build());
    }

    private String queuePositionStr() {
        if (proxy.getIsPrio().isPresent()) {
            if (proxy.getIsPrio().get()) {
                return this.proxy.getQueuePosition() + " / " + Queue.getQueueStatus().prio + " - ETA: " + Queue.getQueueEta(this.proxy.getQueuePosition());
            } else {
                return this.proxy.getQueuePosition() + " / " + Queue.getQueueStatus().regular + " - ETA: " + Queue.getQueueEta(this.proxy.getQueuePosition());
            }
        } else {
            return "?";
        }
    }

    @Subscribe
    public void handleQueueCompleteEvent(QueueCompleteEvent event) {
        this.client.updatePresence(DEFAULT_CONNECTED_PRESENCE).block();
    }

    @Subscribe
    public void handleStartQueueEvent(StartQueueEvent event) {
        sendEmbedMessage(EmbedCreateSpec.builder()
                .title("Started Queuing")
                .color(Color.CYAN)
                .addField("Regular Queue", "" + Queue.getQueueStatus().regular, true)
                .addField("Priority Queue", "" + Queue.getQueueStatus().prio, true)
                .build());
        this.client.updatePresence(getQueuePresence()).block();
    }

    @Subscribe
    public void handleDeathEvent(DeathEvent event) {
        sendEmbedMessage(EmbedCreateSpec.builder()
                .title("Player Death")
                .color(Color.RUBY)
                .addField("Coordinates", getCoordinates(CACHE.getPlayerCache()), false)
                .build());
    }

    @Subscribe
    public void handleDeathMessageEvent(SelfDeathMessageEvent event) {
        sendEmbedMessage(EmbedCreateSpec.builder()
                .title("Death Message")
                .color(Color.RUBY)
                .addField("Message", event.message, false)
                .build());
    }

    static boolean validateButtonInteractionEventFromAccountOwner(final ButtonInteractionEvent event) {
        return event.getInteraction().getMember()
                .map(m -> m.getRoleIds().stream()
                        .map(Snowflake::asString)
                        .anyMatch(roleId -> roleId.equals(CONFIG.discord.accountOwnerRoleId)))
                .orElse(false);
    }

    @Subscribe
    public void handleAutoDisconnectEvent(HealthAutoDisconnectEvent event) {
        sendEmbedMessage(EmbedCreateSpec.builder()
                .title("Health AutoDisconnect Triggered")
                .addField("Health", "" + ((int) CACHE.getPlayerCache().getThePlayer().getHealth()), true)
                .color(Color.CYAN)
                .build());
    }

    @Subscribe
    public void handleProxyClientConnectedEvent(ProxyClientConnectedEvent event) {
        if (CONFIG.client.extra.clientConnectionMessages) {
            sendEmbedMessage(EmbedCreateSpec.builder()
                    .title("Client Connected")
                    .addField("Username", event.clientGameProfile.getName(), true)
                    .color(Color.CYAN)
                    .build());
        }
    }

    @Subscribe
    public void handleProxySpectatorConnectedEvent(ProxySpectatorConnectedEvent event) {
        if (CONFIG.client.extra.clientConnectionMessages) {
            sendEmbedMessage(EmbedCreateSpec.builder()
                    .title("Spectator Connected")
                    .addField("Username", escape(event.clientGameProfile.getName()), true)
                    .color(Color.CYAN)
                    .build());
        }
    }

    @Subscribe
    public void handleProxyClientDisconnectedEvent(ProxyClientDisconnectedEvent event) {
        if (CONFIG.client.extra.clientConnectionMessages) {
            EmbedCreateSpec.Builder builder = EmbedCreateSpec.builder()
                    .title("Client Disconnected")
                    .color(Color.RUBY);
            if (nonNull(event.clientGameProfile)) {
                builder = builder.addField("Username", escape(event.clientGameProfile.getName()), false);
            }
            if (nonNull(event.reason)) {
                builder = builder.addField("Reason", escape(event.reason), false);
            }
            sendEmbedMessage(builder
                    .build());
        }
    }

    @Subscribe
    public void handleNewPlayerInVisualRangeEvent(NewPlayerInVisualRangeEvent event) {
        if (CONFIG.client.extra.visualRangeAlert) {
            boolean notFriend = CONFIG.client.extra.friendsList.stream()
                    .noneMatch(friend -> friend.username.equalsIgnoreCase(event.playerEntry.getName()));
            EmbedCreateSpec.Builder embedCreateSpec = EmbedCreateSpec.builder()
                    .title("Player In Visual Range")
                    .color(notFriend ? Color.RUBY : Color.GREEN)
                    .addField("Player Name", escape(event.playerEntry.getName()), true)
                    .addField("Player UUID", ("[" + event.playerEntry.getId().toString() + "](https://namemc.com/profile/" + event.playerEntry.getId().toString() + ")"), true)
                    .thumbnail(this.proxy.getAvatarURL(event.playerEntry.getId()).toString());

            if (CONFIG.discord.reportCoords) {
                embedCreateSpec.addField("Coordinates", "||["
                        + (int) event.playerEntity.getX() + ", "
                        + (int) event.playerEntity.getY() + ", "
                        + (int) event.playerEntity.getZ()
                        + "]||", false);
            }
            final String buttonId = "addFriend" + ThreadLocalRandom.current().nextInt(1000000);
            final List<Button> buttons = asList(Button.primary(buttonId, "Add Friend"));
            final Function<ButtonInteractionEvent, Publisher<Mono<?>>> mapper = e -> {
                if (e.getCustomId().equals(buttonId)) {
                    DISCORD_LOG.info(e.getInteraction().getMember()
                            .map(User::getTag).orElse("Unknown")
                            + " added friend: " + event.playerEntry.getName() + " [" + event.playerEntry.getId() + "]");
                    WHITELIST_MANAGER.addFriendWhitelistEntryByUsername(event.playerEntry.getName());
                    e.reply().withEmbeds(EmbedCreateSpec.builder()
                            .title("Friend Added")
                            .color(Color.GREEN)
                            .addField("Player Name", escape(event.playerEntry.getName()), true)
                            .addField("Player UUID", ("[" + event.playerEntry.getId() + "](https://namemc.com/profile/" + event.playerEntry.getId() + ")"), true)
                            .thumbnail(this.proxy.getAvatarURL(event.playerEntry.getId()).toString())
                            .build()).block();
                    saveConfig();
                }
                return Mono.empty();
            };
            if (CONFIG.client.extra.visualRangeAlertMention) {
                if (notFriend) {
                    if (CONFIG.discord.visualRangeMentionRoleId.length() > 3) {
                        sendEmbedMessageWithButtons("<@&" + CONFIG.discord.visualRangeMentionRoleId + ">", embedCreateSpec.build(), buttons, mapper, Duration.ofHours(1));
                    } else {
                        sendEmbedMessageWithButtons("<@&" + CONFIG.discord.accountOwnerRoleId + ">", embedCreateSpec.build(), buttons, mapper, Duration.ofHours(1));
                    }
                } else {
                    sendEmbedMessage(embedCreateSpec.build());
                }
            } else {
                if (notFriend) {
                    sendEmbedMessageWithButtons(embedCreateSpec.build(), buttons, mapper, Duration.ofHours(1));
                } else {
                    sendEmbedMessage(embedCreateSpec.build());
                }
            }
        }
    }

    @Subscribe
    public void handleNonWhitelistedPlayerConnectedEvent(NonWhitelistedPlayerConnectedEvent event) {
        EmbedCreateSpec.Builder builder = EmbedCreateSpec.builder()
                .title("Non-Whitelisted Player Connected")
                .color(Color.RUBY);
        if (nonNull(event.remoteAddress())) {
            builder = builder.addField("IP", escape(event.remoteAddress().toString()), false);
        }
        if (nonNull(event.gameProfile()) && nonNull(event.gameProfile().getId()) && nonNull(event.gameProfile().getName())) {
            builder
                    .addField("Username", escape(event.gameProfile().getName()), false)
                    .addField("Player UUID", ("[" + event.gameProfile().getId().toString() + "](https://namemc.com/profile/" + event.gameProfile().getId().toString() + ")"), true)
                    .thumbnail(this.proxy.getAvatarURL(event.gameProfile().getId()).toString());
            final String buttonId = "whitelist" + ThreadLocalRandom.current().nextInt(10000000);
            final List<Button> buttons = asList(Button.primary(buttonId, "Whitelist Player"));
            final Function<ButtonInteractionEvent, Publisher<Mono<?>>> mapper = e -> {
                if (e.getCustomId().equals(buttonId)) {
                    if (validateButtonInteractionEventFromAccountOwner(e)) {
                        DISCORD_LOG.info(e.getInteraction().getMember()
                                .map(User::getTag).orElse("Unknown")
                                + " whitelisted " + event.gameProfile().getName() + " [" + event.gameProfile().getId().toString() + "]");
                        WHITELIST_MANAGER.addWhitelistEntryByUsername(event.gameProfile().getName());
                        e.reply().withEmbeds(EmbedCreateSpec.builder()
                                .title("Player Whitelisted")
                                .color(Color.GREEN)
                                .addField("Player Name", escape(event.gameProfile().getName()), true)
                                .addField("Player UUID", ("[" + event.gameProfile().getId().toString() + "](https://namemc.com/profile/" + event.gameProfile().getId().toString() + ")"), true)
                                .thumbnail(this.proxy.getAvatarURL(event.gameProfile().getId()).toString())
                                .build()).block();
                        saveConfig();
                    } else {
                        DISCORD_LOG.error(e.getInteraction().getMember()
                                .map(User::getTag).orElse("Unknown")
                                + " attempted to whitelist " + event.gameProfile().getName() + " [" + event.gameProfile().getId().toString() + "] but was not authorized to do so!");
                        e.reply().withEmbeds(EmbedCreateSpec.builder()
                                .title("Not Authorized!")
                                .color(Color.RUBY)
                                .addField("Error",
                                        "User: " + e.getInteraction().getMember().map(User::getTag).orElse("Unknown")
                                                + " is not authorized to execute this command! Contact the account owner", true)
                                .build()).block();
                    }
                }
                return Mono.empty();
            };
            sendEmbedMessageWithButtons(builder.build(), buttons, mapper, Duration.ofHours(1L));
        } else { // shouldn't be possible if verifyUsers is enabled
            if (nonNull(event.gameProfile())) {
                builder
                        .addField("Username", escape(event.gameProfile().getName()), false);
            }
            sendEmbedMessage(builder
                    .build());
        }
    }

    @Subscribe
    public void handleProxySpectatorDisconnectedEvent(ProxySpectatorDisconnectedEvent event) {
        if (CONFIG.client.extra.clientConnectionMessages) {
            EmbedCreateSpec.Builder builder = EmbedCreateSpec.builder()
                    .title("Spectator Disconnected")
                    .color(Color.RUBY);
            if (nonNull(event.clientGameProfile)) {
                builder = builder.addField("Username", escape(event.clientGameProfile.getName()), false);
            }
            sendEmbedMessage(builder
                    .build());
        }
    }

    @Subscribe
    public void handleActiveHoursConnectEvent(ActiveHoursConnectEvent event) {
        int queueLength;
        if (proxy.getIsPrio().orElse(false)) {
            queueLength = Queue.getQueueStatus().prio;
        } else {
            queueLength = Queue.getQueueStatus().regular;
        }
        sendEmbedMessage(EmbedCreateSpec.builder()
                .title("Active Hours Connect Triggered")
                .addField("ETA", Queue.getQueueEta(queueLength), false)
                .color(Color.CYAN)
                .build());
    }

    @Subscribe
    public void handleServerChatReceivedEvent(ServerChatReceivedEvent event) {
        if (CONFIG.discord.chatRelay.enable && CONFIG.discord.chatRelay.channelId.length() > 0) {
            if (CONFIG.discord.chatRelay.ignoreQueue && this.proxy.isInQueue()) return;
            try {
                String message = escape(event.message);
                if (CONFIG.discord.chatRelay.mentionWhileConnected || isNull(this.proxy.getCurrentPlayer().get())) {
                    if (CONFIG.discord.chatRelay.mentionRoleOnWhisper || CONFIG.discord.chatRelay.mentionRoleOnNameMention) {
                        if (!message.startsWith("<")) {
                            if (event.isWhisper
                                    && CONFIG.discord.chatRelay.mentionRoleOnWhisper
                                    && !message.toLowerCase(Locale.ROOT).contains("discord.gg/")
                                    && event.sender.map(s -> !WHITELIST_MANAGER.isPlayerIgnored(s.getName())).orElse(true)) {
                                message = "<@&" + CONFIG.discord.accountOwnerRoleId + "> " + message;
                            }
                        } else {
                            if (CONFIG.discord.chatRelay.mentionRoleOnNameMention) {
                                if (event.sender.filter(sender -> sender.getName().equals(CONFIG.authentication.username)).isPresent()) {
                                    message = "<@&" + CONFIG.discord.accountOwnerRoleId + "> " + message;
                                }
                            }
                        }
                    }
                }
                relayChannelMessageQueue.add(MessageCreateRequest.builder().content(message).build());
            } catch (final Throwable e) {
                DISCORD_LOG.error("", e);
            }
        }
    }

    @Subscribe
    public void handleServerPlayerConnectedEvent(ServerPlayerConnectedEvent event) {
        if (CONFIG.discord.chatRelay.enable && CONFIG.discord.chatRelay.connectionMessages && CONFIG.discord.chatRelay.channelId.length() > 0) {
            if (CONFIG.discord.chatRelay.ignoreQueue && this.proxy.isInQueue()) return;
            try {
                relayChannelMessageQueue.add(MessageCreateRequest.builder().content(escape(event.playerEntry.getName() + " connected")).build());
            } catch (final Throwable e) {
                DISCORD_LOG.error("", e);
            }
        }
        if (CONFIG.client.extra.stalk.enabled && !CONFIG.client.extra.stalk.stalkList.isEmpty()) {
            CONFIG.client.extra.stalk.stalkList.stream()
                    .map(s -> s.toLowerCase(Locale.ROOT))
                    .filter(s -> s.equalsIgnoreCase(event.playerEntry.getName()))
                    .findFirst()
                    .ifPresent(player -> {
                        sendEmbedMessage("<@&" + CONFIG.discord.accountOwnerRoleId + ">", EmbedCreateSpec.builder()
                                .title("Stalked Player Online!")
                                .color(Color.GREEN)
                                .addField("Player Name", event.playerEntry.getName(), true)
                                .thumbnail(this.proxy.getAvatarURL(event.playerEntry.getId()).toString())
                                .build());
                    });
        }
    }

    @Subscribe
    public void handleServerPlayerDisconnectedEvent(ServerPlayerDisconnectedEvent event) {
        if (CONFIG.discord.chatRelay.enable && CONFIG.discord.chatRelay.connectionMessages && CONFIG.discord.chatRelay.channelId.length() > 0) {
            if (CONFIG.discord.chatRelay.ignoreQueue && this.proxy.isInQueue()) return;
            try {
                relayChannelMessageQueue.add(MessageCreateRequest.builder().content(escape(event.playerEntry.getName()) + " disconnected").build());
            } catch (final Throwable e) {
                DISCORD_LOG.error("", e);
            }
        }
        if (CONFIG.client.extra.stalk.enabled && !CONFIG.client.extra.stalk.stalkList.isEmpty()) {
            CONFIG.client.extra.stalk.stalkList.stream()
                    .map(s -> s.toLowerCase(Locale.ROOT))
                    .filter(s -> s.equalsIgnoreCase(event.playerEntry.getName()))
                    .findFirst()
                    .ifPresent(player -> {
                        sendEmbedMessage("<@&" + CONFIG.discord.accountOwnerRoleId + ">", EmbedCreateSpec.builder()
                                .title("Stalked Player Offline!")
                                .color(Color.RUBY)
                                .addField("Player Name", event.playerEntry.getName(), true)
                                .thumbnail(this.proxy.getAvatarURL(event.playerEntry.getId()).toString())
                                .build());
                    });
        }
    }

    @Subscribe
    public void handleDiscordMessageSentEvent(DiscordMessageSentEvent event) {
        if (CONFIG.discord.chatRelay.enable) {
            if (this.proxy.isConnected() && !event.message.isEmpty()) {
                this.proxy.getClient().send(new ClientChatPacket(event.message));
                lastRelaymessage = Optional.of(Instant.now());
            }
        }
    }

    @Subscribe
    public void handleUpdateStartEvent(UpdateStartEvent event) {
        sendEmbedMessage(getUpdateMessage());
    }

    @Subscribe
    public void handleServerRestartingEvent(ServerRestartingEvent event) {
        sendEmbedMessage(EmbedCreateSpec.builder()
                .title("Server Restarting")
                .color(Color.RUBY)
                .addField("Message", event.message, true)
                .build());
    }

    @Subscribe
    public void handleProxyLoginFailedEvent(ProxyLoginFailedEvent event) {
        sendEmbedMessage(EmbedCreateSpec.builder()
                .title("Login Failed")
                .color(Color.RUBY)
                .addField("Help", "Try waiting and connecting again.", false)
                .build());
    }

    @Subscribe
    public void handleStartConnectEvent(StartConnectEvent event) {
        sendEmbedMessage(EmbedCreateSpec.builder()
                .title("Connecting...")
                .color(Color.CYAN)
                .build());
    }

    @Subscribe
    public void handlePrioStatusUpdateEvent(PrioStatusUpdateEvent event) {
        EmbedCreateSpec.Builder embedCreateSpec = EmbedCreateSpec.builder();
        if (event.prio) {
            embedCreateSpec
                    .title("Prio Queue Status Detected")
                    .color(Color.GREEN);
        } else {
            embedCreateSpec
                    .title("Prio Queue Status Lost")
                    .color(Color.RUBY);
        }
        embedCreateSpec.addField("User", escape(CONFIG.authentication.username), false);
        sendEmbedMessage((CONFIG.discord.mentionRoleOnPrioUpdate ? "<@&" + CONFIG.discord.accountOwnerRoleId + ">" : ""), embedCreateSpec.build());
    }

    @Subscribe
    public void handlePrioBanStatusUpdateEvent(PrioBanStatusUpdateEvent event) {
        EmbedCreateSpec.Builder embedCreateSpec = EmbedCreateSpec.builder();
        if (event.prioBanned) {
            embedCreateSpec
                    .title("Prio Ban Detected")
                    .color(Color.RUBY);
        } else {
            embedCreateSpec
                    .title("Prio Unban Detected")
                    .color(Color.GREEN);
        }
        embedCreateSpec.addField("User", escape(CONFIG.authentication.username), false);
        sendEmbedMessage((CONFIG.discord.mentionRoleOnPrioBanUpdate ? "<@&" + CONFIG.discord.accountOwnerRoleId + ">" : ""), embedCreateSpec.build());
    }

    @Subscribe
    public void handleAntiAfkStuckEvent(final AntiAfkStuckEvent event) {
        sendEmbedMessage((CONFIG.client.extra.antiafk.actions.stuckWarningMention ? "<@&" + CONFIG.discord.accountOwnerRoleId + ">" : ""), EmbedCreateSpec.builder()
                .title("AntiAFK Warning")
                .color(Color.RUBY)
                .description("AntiAFK enabled but player may be stuck. "
                        + "Log in and move player to a location with " + CONFIG.client.extra.antiafk.actions.walkDistance + "+ flat blocks to move within.")
                .addField("Distance Walked", "" + (int) event.distanceMovedDelta, false)
                .build());
    }

    @Subscribe
    public void handleAutoReconnectEvent(final AutoReconnectEvent event) {
        sendEmbedMessage(EmbedCreateSpec.builder()
                .title("AutoReconnecting in " + event.delaySeconds + "s")
                .color(Color.CYAN)
                .build());
    }

    private EmbedCreateSpec getUpdateMessage() {
        return EmbedCreateSpec.builder()
                .title("Updating and restarting...")
                .color(Color.CYAN)
                .build();
    }

    public static boolean isAllowedChatCharacter(char c0) {
        return c0 != 167 && c0 >= 32 && c0 != 127;
    }

    public static String sanitizeRelayInputMessage(final String input) {
        StringBuilder stringbuilder = new StringBuilder();
        for (char c0 : input.toCharArray()) {
            if (isAllowedChatCharacter(c0)) {
                stringbuilder.append(c0);
            }
        }
        return stringbuilder.toString();
    }

    public void updateProfileImage(final BufferedImage bufferedImage) {
        try {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            ImageIO.write(bufferedImage, "png", os);
            this.restClient.edit(ImmutableUserModifyRequest.builder()
                            .avatar("data:image/png;base64," + Base64.getEncoder().encodeToString(os.toByteArray()))
                            .build())
                    .block();
        } catch (final Exception e) {
            DISCORD_LOG.error("Failed updating discord profile image", e);
        }
    }

    public void sendEmbedMessage(EmbedCreateSpec embedCreateSpec) {
        try {
            mainChannelMessageQueue.add(MessageCreateSpec.builder()
                    .addEmbed(embedCreateSpec)
                    .build().asRequest());
        } catch (final Exception e) {
            DISCORD_LOG.error("Failed sending discord embed message", e);
        }
    }

    private void sendEmbedMessage(String message, EmbedCreateSpec embedCreateSpec) {
        try {
            mainChannelMessageQueue.add(MessageCreateSpec.builder()
                    .content(message)
                    .addEmbed(embedCreateSpec)
                    .build().asRequest());
        } catch (final Exception e) {
            DISCORD_LOG.error("Failed sending discord embed message", e);
        }
    }

    public void sendMessage(final String message) {
        try {
            mainChannelMessageQueue.add(MessageCreateSpec.builder()
                    .content(message)
                    .build().asRequest());
        } catch (final Exception e) {
            DISCORD_LOG.error("Failed sending discord message", e);
        }
    }

    private void sendEmbedMessageWithButtons(String message, EmbedCreateSpec embedCreateSpec, List<Button> buttons, Function<ButtonInteractionEvent, Publisher<Mono<?>>> mapper, Duration timeout) {
        try {
            mainChannelMessageQueue.add(MessageCreateSpec.builder()
                    .content(message)
                    .addEmbed(embedCreateSpec)
                    .components(ActionRow.of(buttons))
                    .build().asRequest());
            client.getEventDispatcher()
                    .on(ButtonInteractionEvent.class, mapper)
                    .timeout(timeout)
                    .onErrorResume(TimeoutException.class, e -> Mono.empty())
                    .subscribe();
        } catch (final Exception e) {
            DISCORD_LOG.error("Failed sending discord message", e);
        }
    }

    private void sendEmbedMessageWithButtons(EmbedCreateSpec embedCreateSpec, List<Button> buttons, Function<ButtonInteractionEvent, Publisher<Mono<?>>> mapper, Duration timeout) {
        try {
            mainChannelMessageQueue.add(MessageCreateSpec.builder()
                    .addEmbed(embedCreateSpec)
                    .components(ActionRow.of(buttons))
                    .build().asRequest());
            client.getEventDispatcher()
                    .on(ButtonInteractionEvent.class, mapper)
                    .timeout(timeout)
                    .onErrorResume(TimeoutException.class, e -> Mono.empty())
                    .subscribe();
        } catch (final Exception e) {
            DISCORD_LOG.error("Failed sending discord message", e);
        }
    }

    private ClientPresence getQueuePresence() {
        return ClientPresence.of(Status.IDLE, ClientActivity.watching(queuePositionStr()));
    }

    public static String escape(String message) {
        return message.replaceAll("_", "\\\\_");
    }

    public boolean isMessageQueueEmpty() {
        return mainChannelMessageQueue.isEmpty();
    }
}
