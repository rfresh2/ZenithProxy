package com.zenith.discord;

import com.github.steveice10.mc.protocol.packet.ingame.serverbound.ServerboundChatPacket;
import com.google.common.base.Suppliers;
import com.zenith.Proxy;
import com.zenith.command.CommandContext;
import com.zenith.command.DiscordCommandContext;
import com.zenith.event.Subscription;
import com.zenith.event.module.AutoEatOutOfFoodEvent;
import com.zenith.event.proxy.*;
import com.zenith.feature.queue.Queue;
import discord4j.common.ReactorResources;
import discord4j.common.util.Snowflake;
import discord4j.core.DiscordClient;
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
import discord4j.gateway.GatewayReactorResources;
import discord4j.gateway.intent.Intent;
import discord4j.gateway.intent.IntentSet;
import discord4j.rest.RestClient;
import discord4j.rest.entity.RestChannel;
import discord4j.rest.request.RouterOptions;
import discord4j.rest.util.Color;
import discord4j.rest.util.MultipartRequest;
import lombok.Getter;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.transport.ProxyProvider;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.zenith.Shared.*;
import static com.zenith.command.impl.StatusCommand.getCoordinates;
import static com.zenith.event.SimpleEventBus.pair;
import static discord4j.common.ReactorResources.DEFAULT_BLOCKING_TASK_SCHEDULER;
import static discord4j.common.ReactorResources.DEFAULT_TIMER_TASK_SCHEDULER;
import static java.util.Arrays.asList;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

public class DiscordBot {

    private RestClient restClient;
    private RestChannel mainRestChannel;
    private RestChannel relayRestChannel;
    private GatewayDiscordClient client;
    // Main channel discord message FIFO queue
    private final ConcurrentLinkedQueue<MultipartRequest<MessageCreateRequest>> mainChannelMessageQueue;
    private final ConcurrentLinkedQueue<MessageCreateRequest> relayChannelMessageQueue;
    private final Supplier<ClientPresence> disconnectedPresence = Suppliers.memoize(() -> ClientPresence.of(Status.DO_NOT_DISTURB, ClientActivity.custom(
        "Disconnected")));
    private final Supplier<ClientPresence> defaultConnectedPresence = () -> ClientPresence.of(Status.ONLINE, ClientActivity.custom(
        (CONFIG.client.server.address.toLowerCase().endsWith("2b2t.org") ? "2b2t" : CONFIG.client.server.address)));
    public Optional<Instant> lastRelaymessage = Optional.empty();

    @Getter
    private boolean isRunning;
    private Subscription eventSubscription;

    public DiscordBot() {
        this.mainChannelMessageQueue = new ConcurrentLinkedQueue<>();
        this.relayChannelMessageQueue = new ConcurrentLinkedQueue<>();
        this.isRunning = false;
    }

    public void initEventHandlers() {
        if (eventSubscription != null) throw new RuntimeException("Event handlers already initialized");
        eventSubscription = EVENT_BUS.subscribe(
            pair(ConnectEvent.class, this::handleConnectEvent),
            pair(PlayerOnlineEvent.class, this::handlePlayerOnlineEvent),
            pair(DisconnectEvent.class, this::handleDisconnectEvent),
            pair(QueuePositionUpdateEvent.class, this::handleQueuePositionUpdateEvent),
            pair(AutoEatOutOfFoodEvent.class, this::handleAutoEatOutOfFoodEvent),
            pair(QueueCompleteEvent.class, this::handleQueueCompleteEvent),
            pair(StartQueueEvent.class, this::handleStartQueueEvent),
            pair(DeathEvent.class, this::handleDeathEvent),
            pair(SelfDeathMessageEvent.class, this::handleSelfDeathMessageEvent),
            pair(HealthAutoDisconnectEvent.class, this::handleHealthAutoDisconnectEvent),
            pair(ProxyClientConnectedEvent.class, this::handleProxyClientConnectedEvent),
            pair(ProxySpectatorConnectedEvent.class, this::handleProxySpectatorConnectedEvent),
            pair(ProxyClientDisconnectedEvent.class, this::handleProxyClientDisconnectedEvent),
            pair(NewPlayerInVisualRangeEvent.class, this::handleNewPlayerInVisualRangeEvent),
            pair(NonWhitelistedPlayerConnectedEvent.class, this::handleNonWhitelistedPlayerConnectedEvent),
            pair(ProxySpectatorDisconnectedEvent.class, this::handleProxySpectatorDisconnectedEvent),
            pair(ActiveHoursConnectEvent.class, this::handleActiveHoursConnectEvent),
            pair(ServerChatReceivedEvent.class, this::handleServerChatReceivedEvent),
            pair(ServerPlayerConnectedEvent.class, this::handleServerPlayerConnectedEvent),
            pair(ServerPlayerDisconnectedEvent.class, this::handleServerPlayerDisconnectedEvent),
            pair(DiscordMessageSentEvent.class, this::handleDiscordMessageSentEvent),
            pair(UpdateStartEvent.class, this::handleUpdateStartEvent),
            pair(ServerRestartingEvent.class, this::handleServerRestartingEvent),
            pair(ProxyLoginFailedEvent.class, this::handleProxyLoginFailedEvent),
            pair(StartConnectEvent.class, this::handleStartConnectEvent),
            pair(PrioStatusUpdateEvent.class, this::handlePrioStatusUpdateEvent),
            pair(PrioBanStatusUpdateEvent.class, this::handlePrioBanStatusUpdateEvent),
            pair(AutoReconnectEvent.class, this::handleAutoReconnectEvent),
            pair(MsaDeviceCodeLoginEvent.class, this::handleMsaDeviceCodeLoginEvent),
            pair(DeathMessageEvent.class, this::handleDeathMessageEvent),
            pair(UpdateAvailableEvent.class, this::handleUpdateAvailableEvent)
        );
    }

    public void createClient() {
        if (CONFIG.discord.channelId.isEmpty()) throw new RuntimeException("Discord bot is enabled but channel id is not set");
        if (CONFIG.discord.chatRelay.enable) {
            if (CONFIG.discord.chatRelay.channelId.isEmpty()) throw new RuntimeException("Discord chat relay is enabled and channel id is not set");
            if (CONFIG.discord.channelId.equals(CONFIG.discord.chatRelay.channelId)) throw new RuntimeException("Discord channel id and chat relay channel id cannot be the same");
        }

        DiscordClient discordClient = buildProxiedClient(DiscordClientBuilder.create(CONFIG.discord.token)).build();
        this.client = discordClient.gateway()
                .setGatewayReactorResources(reactorResources -> GatewayReactorResources.builder(discordClient.getCoreResources().getReactorResources()).build())
                .setEnabledIntents((IntentSet.of(Intent.MESSAGE_CONTENT, Intent.GUILD_MESSAGES)))
                .setInitialPresence(shardInfo -> disconnectedPresence.get())
                .login()
                .block();

        restClient = client.getRestClient();
        mainRestChannel = restClient.getChannelById(Snowflake.of(CONFIG.discord.channelId));
        relayRestChannel = restClient.getChannelById(Snowflake.of(CONFIG.discord.chatRelay.channelId));
        client.getEventDispatcher().on(MessageCreateEvent.class).subscribe(event -> {
            if (CONFIG.discord.chatRelay.enable && !CONFIG.discord.chatRelay.channelId.isEmpty() && event.getMessage().getChannelId().equals(Snowflake.of(CONFIG.discord.chatRelay.channelId))) {
                if (!event.getMember().get().getId().equals(this.client.getSelfId())) {
                    EVENT_BUS.postAsync(new DiscordMessageSentEvent(sanitizeRelayInputMessage(event.getMessage().getContent())));
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
                final CommandContext context = DiscordCommandContext.create(inputMessage, event, mainRestChannel);
                COMMAND_MANAGER.execute(context);
                final MultipartRequest<MessageCreateRequest> request = commandEmbedOutputToMessage(context);
                if (request != null) {
                    DISCORD_LOG.debug("Discord bot response: {}", request.getJsonPayload());
                    mainChannelMessageQueue.add(request);
                    if (CONFIG.interactiveTerminal.enable) TERMINAL_MANAGER.logEmbedOutput(context.getEmbedBuilder().build());
                }
                if (!context.getMultiLineOutput().isEmpty()) {
                    for (final String line : context.getMultiLineOutput()) {
                        mainChannelMessageQueue.add(MessageCreateSpec.builder().content(line).build().asRequest());
                    }
                    if (CONFIG.interactiveTerminal.enable) TERMINAL_MANAGER.logMultiLineOutput(context);
                }
            } catch (final Exception e) {
                DISCORD_LOG.error("Failed processing discord command: {}", message, e);
            }
        });
    }

    public void start() {
        createClient();
        initEventHandlers();

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
                this.mainRestChannel.createMessage(message).block();
            }
        } catch (final Throwable e) {
            DISCORD_LOG.error("Message processor error", e);
        }
    }

    private void processRelayMessageQueue() {
        try {
            MessageCreateRequest message = relayChannelMessageQueue.poll();
            if (nonNull(message) && !message.content().isAbsent() && !message.content().get().isBlank())
                this.relayRestChannel.createMessage(message).block();
        } catch (final Throwable e) {
            DISCORD_LOG.error("Message processor error", e);
        }
    }

    private void updatePresence() {
        try {
            if (Proxy.getInstance().isInQueue())
                this.client.updatePresence(getQueuePresence()).block();
            else if (Proxy.getInstance().isConnected())
                this.client.updatePresence(getOnlinePresence()).block();
            else
                this.client.updatePresence(disconnectedPresence.get()).block();
        } catch (final IllegalStateException e) {
            if (e.getMessage().contains("Backpressure overflow")) {
                DISCORD_LOG.error("Caught backpressure overflow, restarting discord session", e);
                this.client.logout().block();
                createClient();
            } else throw e;
        } catch (final Exception e) {
            DISCORD_LOG.error("Failed updating discord presence", e);
        }
    }

    private ClientPresence getOnlinePresence() {
        long onlineSeconds = Instant.now().getEpochSecond() - Proxy.getInstance().getConnectTime().getEpochSecond();
        return ClientPresence.of(Status.ONLINE, ClientActivity.custom(
      (CONFIG.client.server.address.toLowerCase().endsWith("2b2t.org") ? "2b2t" : CONFIG.client.server.address) + " [" + Queue.getEtaStringFromSeconds(onlineSeconds) + "]"));
    }

    private void handleProxyUpdateComplete() {
        CONFIG.discord.isUpdating = false;
        saveConfig();
        sendEmbedMessage(EmbedCreateSpec.builder()
                             .title("Update complete!")
                             .description("Current Version: `" + escape(LAUNCH_CONFIG.version) + "`")
                             .color(Color.GREEN)
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
        if (Proxy.getInstance().getIsPrio().orElse(false))
            return Proxy.getInstance().getQueuePosition() + " / " + Queue.getQueueStatus().prio + " - ETA: " + Queue.getQueueEta(Proxy.getInstance().getQueuePosition());
        else
            return Proxy.getInstance().getQueuePosition() + " / " + Queue.getQueueStatus().regular + " - ETA: " + Queue.getQueueEta(Proxy.getInstance().getQueuePosition());
    }

    static boolean validateButtonInteractionEventFromAccountOwner(final ButtonInteractionEvent event) {
        return event.getInteraction().getMember()
            .map(m -> m.getRoleIds().stream()
                .map(Snowflake::asString)
                .anyMatch(roleId -> roleId.equals(CONFIG.discord.accountOwnerRoleId)))
            .orElse(false);
    }


    private EmbedCreateSpec getUpdateMessage(final Optional<String> newVersion) {
        String verString = "Current Version: " + escape(LAUNCH_CONFIG.version);
        if (newVersion.isPresent()) verString += "\nNew Version: " + escape(newVersion.get());
        return EmbedCreateSpec.builder()
            .title("Updating and restarting...")
            .description(verString)
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

    public void updateProfileImage(final byte[] imageBytes) {
        try {
            this.restClient.edit(ImmutableUserModifyRequest.builder()
                                     .avatar("data:image/png;base64," + Base64.getEncoder().encodeToString(imageBytes))
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
            TERMINAL_MANAGER.logEmbedOutput(embedCreateSpec);
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
            TERMINAL_LOG.info(message);
            TERMINAL_MANAGER.logEmbedOutput(embedCreateSpec);
        } catch (final Exception e) {
            DISCORD_LOG.error("Failed sending discord embed message", e);
        }
    }

    public void sendMessage(final String message) {
        try {
            mainChannelMessageQueue.add(MessageCreateSpec.builder()
                                            .content(message)
                                            .build().asRequest());
            TERMINAL_LOG.info(message);
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
            TERMINAL_LOG.info(message);
            TERMINAL_MANAGER.logEmbedOutput(embedCreateSpec);
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
            TERMINAL_MANAGER.logEmbedOutput(embedCreateSpec);
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
        return ClientPresence.of(Status.IDLE, ClientActivity.custom(queuePositionStr()));
    }

    public static String escape(String message) {
        return message.replaceAll("_", "\\\\_").replaceAll("@", "\\\\@");
    }

    public boolean isMessageQueueEmpty() {
        return mainChannelMessageQueue.isEmpty();
    }

    public DiscordClientBuilder<DiscordClient, RouterOptions> buildProxiedClient(final DiscordClientBuilder<DiscordClient, RouterOptions> builder) {
        if (!CONFIG.discord.connectionProxy.enabled) return builder;
        builder.setReactorResources(new ReactorResources(getProxiedHttpClient(),
                                                         DEFAULT_TIMER_TASK_SCHEDULER.get(),
                                                         DEFAULT_BLOCKING_TASK_SCHEDULER.get()));
        return builder;
    }

    public HttpClient getProxiedHttpClient() {
        return HttpClient.create().compress(true).followRedirect(true).secure().proxy((ProxyProvider.TypeSpec provider) -> {
            ProxyProvider.AddressSpec addressSpec;
            switch (CONFIG.discord.connectionProxy.type){
                case SOCKS4 -> addressSpec = provider.type(ProxyProvider.Proxy.SOCKS4);
                case SOCKS5 -> addressSpec = provider.type(ProxyProvider.Proxy.SOCKS5);
                case HTTP -> addressSpec = provider.type(ProxyProvider.Proxy.HTTP);
                default -> throw new RuntimeException("Invalid proxy type: " + CONFIG.discord.connectionProxy.type);
            }
            ProxyProvider.Builder proxyBuilder = addressSpec
                .host(CONFIG.discord.connectionProxy.host)
                .port(CONFIG.discord.connectionProxy.port);
            if(!CONFIG.discord.connectionProxy.user.isEmpty())
                proxyBuilder.username(CONFIG.discord.connectionProxy.user);
            if(!CONFIG.discord.connectionProxy.password.isEmpty())
                proxyBuilder.password(s -> CONFIG.discord.connectionProxy.password);
        });
    }

    public void handleConnectEvent(ConnectEvent event) {
        sendEmbedMessage(EmbedCreateSpec.builder()
                .title("Proxy Connected")
                .color(Color.MOON_YELLOW)
                .addField("Server", CONFIG.client.server.address, true)
                .addField("Proxy IP", CONFIG.server.getProxyAddress(), false)
                .build());
        this.client.updatePresence(defaultConnectedPresence.get()).block();
    }

    public void handlePlayerOnlineEvent(PlayerOnlineEvent event) {
        sendEmbedMessage(EmbedCreateSpec.builder()
                .title("Proxy Online")
                .color(Color.MEDIUM_SEA_GREEN)
                .build());
    }

    public void handleDisconnectEvent(DisconnectEvent event) {
        boolean sus = event.reason.startsWith("Login failed: Authentication error: Your account has been suspended for the next ");
        sendEmbedMessage((sus ? "<@&" + CONFIG.discord.accountOwnerRoleId + ">" : ""), EmbedCreateSpec.builder()
                .title("Proxy Disconnected")
                .addField("Reason", event.reason, true)
                .color(Color.RUBY)
                .build());
        SCHEDULED_EXECUTOR_SERVICE.submit(() -> this.client.updatePresence(disconnectedPresence.get()).block());
        if (sus) { Proxy.getInstance().cancelAutoReconnect(); }
    }

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

    public void handleAutoEatOutOfFoodEvent(final AutoEatOutOfFoodEvent event) {
        sendEmbedMessage(EmbedCreateSpec.builder()
                .title("AutoEat Out Of Food")
                .description("AutoEat threshold met but player has no food")
                .color(Color.RUBY)
                .build());
    }

    public void handleQueueCompleteEvent(QueueCompleteEvent event) {
        this.client.updatePresence(defaultConnectedPresence.get()).block();
    }

    public void handleStartQueueEvent(StartQueueEvent event) {
        sendEmbedMessage(EmbedCreateSpec.builder()
                .title("Started Queuing")
                .color(Color.MOON_YELLOW)
                .addField("Regular Queue", "" + Queue.getQueueStatus().regular, true)
                .addField("Priority Queue", "" + Queue.getQueueStatus().prio, true)
                .build());
        this.client.updatePresence(getQueuePresence()).block();
    }

    public void handleDeathEvent(DeathEvent event) {
        sendEmbedMessage(EmbedCreateSpec.builder()
                .title("Player Death")
                .color(Color.RUBY)
                .addField("Coordinates", getCoordinates(CACHE.getPlayerCache()), false)
                .build());
    }

    public void handleSelfDeathMessageEvent(SelfDeathMessageEvent event) {
        sendEmbedMessage(EmbedCreateSpec.builder()
                .title("Death Message")
                .color(Color.RUBY)
                .addField("Message", event.message, false)
                .build());
    }

    public void handleHealthAutoDisconnectEvent(HealthAutoDisconnectEvent event) {
        sendEmbedMessage(EmbedCreateSpec.builder()
                .title("Health AutoDisconnect Triggered")
                .addField("Health", "" + (CACHE.getPlayerCache().getThePlayer().getHealth()), true)
                .color(Color.CYAN)
                .build());
    }

    public void handleProxyClientConnectedEvent(ProxyClientConnectedEvent event) {
        if (CONFIG.client.extra.clientConnectionMessages) {
            sendEmbedMessage(EmbedCreateSpec.builder()
                    .title("Client Connected")
                    .addField("Username", event.clientGameProfile.getName(), true)
                    .color(Color.CYAN)
                    .build());
        }
    }

    public void handleProxySpectatorConnectedEvent(ProxySpectatorConnectedEvent event) {
        if (CONFIG.client.extra.clientConnectionMessages) {
            sendEmbedMessage(EmbedCreateSpec.builder()
                    .title("Spectator Connected")
                    .addField("Username", escape(event.clientGameProfile.getName()), true)
                    .color(Color.CYAN)
                    .build());
        }
    }

    public void handleProxyClientDisconnectedEvent(ProxyClientDisconnectedEvent event) {
        if (!CONFIG.client.extra.clientConnectionMessages) return;
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

    public void handleNewPlayerInVisualRangeEvent(NewPlayerInVisualRangeEvent event) {
        if (!CONFIG.client.extra.visualRangeAlert) return;
        boolean isFriend = CONFIG.client.extra.friendsList.stream()
                .anyMatch(friend -> friend.uuid.equals(event.playerEntry().getProfileId()));
        if (isFriend && CONFIG.client.extra.visualRangeIgnoreFriends) {
            DISCORD_LOG.debug("Ignoring visual range alert for friend: " + event.playerEntry().getName());
            return;
        }
        EmbedCreateSpec.Builder embedCreateSpec = EmbedCreateSpec.builder()
                .title("Player In Visual Range")
                .color(isFriend ? Color.GREEN : Color.RUBY)
                .addField("Player Name", escape(event.playerEntry().getName()), true)
                .addField("Player UUID", ("[" + event.playerEntry().getProfileId().toString() + "](https://namemc.com/profile/" + event.playerEntry().getProfileId().toString() + ")"), true)
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
                WHITELIST_MANAGER.addFriendWhitelistEntryByUsername(event.playerEntry().getName());
                e.reply().withEmbeds(EmbedCreateSpec.builder()
                        .title("Friend Added")
                        .color(Color.GREEN)
                        .addField("Player Name", escape(event.playerEntry().getName()), true)
                        .addField("Player UUID", ("[" + event.playerEntry().getProfileId() + "](https://namemc.com/profile/" + event.playerEntry().getProfileId() + ")"), true)
                        .thumbnail(Proxy.getInstance().getAvatarURL(event.playerEntry().getProfileId()).toString())
                        .build()).block();
                saveConfig();
            }
            return Mono.empty();
        };
        if (CONFIG.client.extra.visualRangeAlertMention) {
            if (!isFriend) {
                if (CONFIG.discord.visualRangeMentionRoleId.length() > 3) {
                    sendEmbedMessageWithButtons("<@&" + CONFIG.discord.visualRangeMentionRoleId + ">", embedCreateSpec.build(), buttons, mapper, Duration.ofHours(1));
                } else {
                    sendEmbedMessageWithButtons("<@&" + CONFIG.discord.accountOwnerRoleId + ">", embedCreateSpec.build(), buttons, mapper, Duration.ofHours(1));
                }
            } else {
                sendEmbedMessage(embedCreateSpec.build());
            }
        } else {
            if (!isFriend) {
                sendEmbedMessageWithButtons(embedCreateSpec.build(), buttons, mapper, Duration.ofHours(1));
            } else {
                sendEmbedMessage(embedCreateSpec.build());
            }
        }
    }

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
                    .thumbnail(Proxy.getInstance().getAvatarURL(event.gameProfile().getId()).toString());
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
                                .thumbnail(Proxy.getInstance().getAvatarURL(event.gameProfile().getId()).toString())
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

    public void handleActiveHoursConnectEvent(ActiveHoursConnectEvent event) {
        int queueLength;
        if (Proxy.getInstance().getIsPrio().orElse(false)) {
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

    public void handleServerChatReceivedEvent(ServerChatReceivedEvent event) {
        if (!CONFIG.discord.chatRelay.enable || CONFIG.discord.chatRelay.channelId.isEmpty()) return;
        if (CONFIG.discord.chatRelay.ignoreQueue && Proxy.getInstance().isInQueue()) return;
        try {
            String message = escape(event.message());
            if (CONFIG.discord.chatRelay.mentionWhileConnected || isNull(Proxy.getInstance().getCurrentPlayer().get())) {
                if (CONFIG.discord.chatRelay.mentionRoleOnWhisper || CONFIG.discord.chatRelay.mentionRoleOnNameMention) {
                    if (!message.startsWith("<")) {
                        if (event.isWhisper()
                            && CONFIG.discord.chatRelay.mentionRoleOnWhisper
                            && !message.toLowerCase(Locale.ROOT).contains("discord.gg/")
                            && event.sender().map(s -> !WHITELIST_MANAGER.isPlayerIgnored(s.getName())).orElse(true)) {
                            message = "<@&" + CONFIG.discord.accountOwnerRoleId + "> " + message;
                        }
                    } else {
                        if (CONFIG.discord.chatRelay.mentionRoleOnNameMention) {
                            if (event.sender().filter(sender -> sender.getName().equals(CONFIG.authentication.username)).isEmpty()
                                && event.sender().map(s -> !WHITELIST_MANAGER.isPlayerIgnored(s.getName())).orElse(true)
                                && Arrays.asList(message.toLowerCase().split(" ")).contains(CONFIG.authentication.username.toLowerCase())) {
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

    public void handleServerPlayerConnectedEvent(ServerPlayerConnectedEvent event) {
        if (CONFIG.discord.chatRelay.enable && CONFIG.discord.chatRelay.connectionMessages && !CONFIG.discord.chatRelay.channelId.isEmpty()) {
            if (CONFIG.discord.chatRelay.ignoreQueue && Proxy.getInstance().isInQueue()) return;
            try {
                relayChannelMessageQueue.add(MessageCreateRequest.builder().content(escape(event.playerEntry().getName() + " connected")).build());
            } catch (final Throwable e) {
                DISCORD_LOG.error("", e);
            }
        }
        if (CONFIG.client.extra.stalk.enabled && !CONFIG.client.extra.stalk.stalkList.isEmpty()) {
            CONFIG.client.extra.stalk.stalkList.stream()
                    .map(s -> s.toLowerCase(Locale.ROOT))
                    .filter(s -> s.equalsIgnoreCase(event.playerEntry().getName()))
                    .findFirst()
                    .ifPresent(player -> {
                        sendEmbedMessage("<@&" + CONFIG.discord.accountOwnerRoleId + ">", EmbedCreateSpec.builder()
                                .title("Stalked Player Online!")
                                .color(Color.MEDIUM_SEA_GREEN)
                                .addField("Player Name", event.playerEntry().getName(), true)
                                .thumbnail(Proxy.getInstance().getAvatarURL(event.playerEntry().getProfileId()).toString())
                                .build());
                    });
        }
    }

    public void handleServerPlayerDisconnectedEvent(ServerPlayerDisconnectedEvent event) {
        if (CONFIG.discord.chatRelay.enable && CONFIG.discord.chatRelay.connectionMessages && !CONFIG.discord.chatRelay.channelId.isEmpty()) {
            if (CONFIG.discord.chatRelay.ignoreQueue && Proxy.getInstance().isInQueue()) return;
            try {
                relayChannelMessageQueue.add(MessageCreateRequest.builder().content(escape(event.playerEntry().getName()) + " disconnected").build());
            } catch (final Throwable e) {
                DISCORD_LOG.error("", e);
            }
        }
        if (CONFIG.client.extra.stalk.enabled && !CONFIG.client.extra.stalk.stalkList.isEmpty()) {
            CONFIG.client.extra.stalk.stalkList.stream()
                    .map(s -> s.toLowerCase(Locale.ROOT))
                    .filter(s -> s.equalsIgnoreCase(event.playerEntry().getName()))
                    .findFirst()
                    .ifPresent(player -> {
                        sendEmbedMessage("<@&" + CONFIG.discord.accountOwnerRoleId + ">", EmbedCreateSpec.builder()
                                .title("Stalked Player Offline!")
                                .color(Color.RUBY)
                                .addField("Player Name", event.playerEntry().getName(), true)
                                .thumbnail(Proxy.getInstance().getAvatarURL(event.playerEntry().getProfileId()).toString())
                                .build());
                    });
        }
    }

    public void handleDiscordMessageSentEvent(DiscordMessageSentEvent event) {
        if (!CONFIG.discord.chatRelay.enable) return;
        if (!Proxy.getInstance().isConnected() || event.message.isEmpty()) return;
        Proxy.getInstance().getClient().send(new ServerboundChatPacket(event.message));
        lastRelaymessage = Optional.of(Instant.now());
    }

    public void handleUpdateStartEvent(UpdateStartEvent event) {
        sendEmbedMessage(getUpdateMessage(event.newVersion()));
    }

    public void handleServerRestartingEvent(ServerRestartingEvent event) {
        sendEmbedMessage(EmbedCreateSpec.builder()
                .title("Server Restarting")
                .color(Color.RUBY)
                .addField("Message", event.message, true)
                .build());
    }

    public void handleProxyLoginFailedEvent(ProxyLoginFailedEvent event) {
        sendEmbedMessage(EmbedCreateSpec.builder()
                .title("Login Failed")
                .color(Color.RUBY)
                .addField("Help", "Try waiting and connecting again.", false)
                .build());
    }

    public void handleStartConnectEvent(StartConnectEvent event) {
        sendEmbedMessage(EmbedCreateSpec.builder()
                .title("Connecting...")
                .color(Color.MOON_YELLOW)
                .build());
    }

    public void handlePrioStatusUpdateEvent(PrioStatusUpdateEvent event) {
        if (!CONFIG.client.extra.prioStatusChangeMention) return;
        EmbedCreateSpec.Builder embedCreateSpec = EmbedCreateSpec.builder();
        if (event.prio) {
            embedCreateSpec
                    .title("Prio Queue Status Detected")
                    .color(Color.GREEN);
        } else {
            embedCreateSpec
                    .title("Prio Queue Status Lost")
                    .color(Color.RED);
        }
        embedCreateSpec.addField("User", escape(CONFIG.authentication.username), false);
        sendEmbedMessage((CONFIG.discord.mentionRoleOnPrioUpdate ? "<@&" + CONFIG.discord.accountOwnerRoleId + ">" : ""), embedCreateSpec.build());
    }

    public void handlePrioBanStatusUpdateEvent(PrioBanStatusUpdateEvent event) {
        EmbedCreateSpec.Builder embedCreateSpec = EmbedCreateSpec.builder();
        if (event.prioBanned) {
            embedCreateSpec
                    .title("Prio Ban Detected")
                    .color(Color.RED);
        } else {
            embedCreateSpec
                    .title("Prio Unban Detected")
                    .color(Color.GREEN);
        }
        embedCreateSpec.addField("User", escape(CONFIG.authentication.username), false);
        sendEmbedMessage((CONFIG.discord.mentionRoleOnPrioBanUpdate ? "<@&" + CONFIG.discord.accountOwnerRoleId + ">" : ""), embedCreateSpec.build());
    }

    public void handleAutoReconnectEvent(final AutoReconnectEvent event) {
        sendEmbedMessage(EmbedCreateSpec.builder()
                .title("AutoReconnecting in " + event.delaySeconds + "s")
                .color(Color.MOON_YELLOW)
                .build());
    }

    public void handleMsaDeviceCodeLoginEvent(final MsaDeviceCodeLoginEvent event) {
        sendEmbedMessage("<@&" + CONFIG.discord.accountOwnerRoleId + ">", EmbedCreateSpec.builder()
            .title("Microsoft Device Code Login")
            .color(Color.CYAN)
            .description("Login [here]("
                             + event.getDeviceCode().verificationUri()
                             + ") with code: " + event.getDeviceCode().userCode())
            .build());
    }

    public void handleDeathMessageEvent(final DeathMessageEvent event) {
        if (!CONFIG.client.extra.killMessage) return;
        event.deathMessageParseResult.getKiller().ifPresent(killer -> {
            if (!killer.getName().equals(CONFIG.authentication.username)) return;
            sendEmbedMessage(EmbedCreateSpec.builder()
                                 .title("Kill Detected")
                                 .color(Color.CYAN)
                                 .addField("Victim", escape(event.deathMessageParseResult.getVictim()), false)
                                 .addField("Message", escape(event.deathMessageRaw), false)
                    .build());
        });
    }

    public void handleUpdateAvailableEvent(final UpdateAvailableEvent event) {
        EmbedCreateSpec.Builder embedBuilder = EmbedCreateSpec.builder()
            .title("Update Available!")
            .color(Color.CYAN);
        event.getVersion().ifPresent(v -> {
            embedBuilder
                .addField("Current", "`" + escape(LAUNCH_CONFIG.version) + "`", false)
                .addField("New", "`" + escape(v) + "`", false);
        });
        embedBuilder.addField(
            "Info",
            "Update will be applied at next opportunity.\nOr apply the update now: `.update`",
            false);
        sendEmbedMessage(embedBuilder.build());
    }
}
