package com.zenith.discord;

import com.github.steveice10.mc.protocol.data.game.PlayerListEntry;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.ServerboundChatPacket;
import com.zenith.Proxy;
import com.zenith.command.CommandContext;
import com.zenith.command.CommandOutputHelper;
import com.zenith.command.DiscordCommandContext;
import com.zenith.event.module.*;
import com.zenith.event.proxy.*;
import com.zenith.feature.autoupdater.AutoUpdater;
import com.zenith.feature.deathmessages.DeathMessageParseResult;
import com.zenith.feature.deathmessages.KillerType;
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
import discord4j.core.spec.ApplicationEditSpec;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.core.spec.UserEditSpec;
import discord4j.core.util.MentionUtil;
import discord4j.discordjson.Id;
import discord4j.discordjson.json.EmbedData;
import discord4j.discordjson.json.MessageCreateRequest;
import discord4j.discordjson.json.MessageData;
import discord4j.discordjson.possible.Possible;
import discord4j.gateway.GatewayReactorResources;
import discord4j.gateway.intent.Intent;
import discord4j.gateway.intent.IntentSet;
import discord4j.rest.RestClient;
import discord4j.rest.entity.RestChannel;
import discord4j.rest.request.RouterOptions;
import discord4j.rest.util.Color;
import discord4j.rest.util.Image;
import discord4j.rest.util.MultipartRequest;
import io.netty.resolver.DefaultAddressResolverGroup;
import lombok.Getter;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.transport.ProxyProvider;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.github.rfresh2.EventConsumer.of;
import static com.zenith.Shared.*;
import static com.zenith.command.impl.StatusCommand.getCoordinates;
import static com.zenith.util.math.MathHelper.formatDuration;
import static discord4j.common.ReactorResources.DEFAULT_BLOCKING_TASK_SCHEDULER;
import static discord4j.common.ReactorResources.DEFAULT_TIMER_TASK_SCHEDULER;
import static java.util.Arrays.asList;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

public class DiscordBot {

    public static final ClientPresence autoReconnectingPresence = ClientPresence.of(Status.IDLE, ClientActivity.custom(
        "AutoReconnecting..."));
    private RestClient restClient;
    private RestChannel mainRestChannel;
    private RestChannel relayRestChannel;
    private GatewayDiscordClient client;
    // Main channel discord message FIFO queue
    private final ConcurrentLinkedQueue<MultipartRequest<MessageCreateRequest>> mainChannelMessageQueue;
    private final ConcurrentLinkedQueue<MultipartRequest<MessageCreateRequest>> relayChannelMessageQueue;
    private final ClientPresence disconnectedPresence = ClientPresence.of(Status.DO_NOT_DISTURB, ClientActivity.custom(
        "Disconnected"));
    private final Supplier<ClientPresence> defaultConnectedPresence = () -> ClientPresence.of(Status.ONLINE, ClientActivity.custom(
        (Proxy.getInstance().isOn2b2t() ? "2b2t" : CONFIG.client.server.address)));
    public Optional<Instant> lastRelaymessage = Optional.empty();

    @Getter
    private boolean isRunning;
    private ScheduledFuture<?> presenceUpdateFuture;
    private ScheduledFuture<?> mainChannelMessageQueueProcessFuture;
    private ScheduledFuture<?> relayChannelMessageQueueProcessFuture;

    public DiscordBot() {
        this.mainChannelMessageQueue = new ConcurrentLinkedQueue<>();
        this.relayChannelMessageQueue = new ConcurrentLinkedQueue<>();
        this.isRunning = false;
    }

    public void initEventHandlers() {
        if (EVENT_BUS.isSubscribed(this)) throw new RuntimeException("Event handlers already initialized");
        EVENT_BUS.subscribe(this,
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
                            of(NewPlayerInVisualRangeEvent.class, this::handleNewPlayerInVisualRangeEvent),
                            of(PlayerLeftVisualRangeEvent.class, this::handlePlayerLeftVisualRangeEvent),
                            of(PlayerLogoutInVisualRangeEvent.class, this::handlePlayerLogoutInVisualRangeEvent),
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

    public void createClient() {
        if (CONFIG.discord.channelId.isEmpty()) throw new RuntimeException("Discord bot is enabled but channel id is not set");
        if (CONFIG.discord.chatRelay.enable) {
            if (CONFIG.discord.chatRelay.channelId.isEmpty()) throw new RuntimeException("Discord chat relay is enabled and channel id is not set");
            if (CONFIG.discord.channelId.equals(CONFIG.discord.chatRelay.channelId)) throw new RuntimeException("Discord channel id and chat relay channel id cannot be the same");
        }
        if (CONFIG.discord.accountOwnerRoleId.isEmpty()) throw new RuntimeException("Discord account owner role id is not set");
        try {
            Snowflake.of(CONFIG.discord.accountOwnerRoleId);
        } catch (final NumberFormatException e) {
            throw new RuntimeException("Invalid account owner role ID set: " + CONFIG.discord.accountOwnerRoleId);
        }
        DiscordClient discordClient = buildProxiedClient(DiscordClientBuilder.create(CONFIG.discord.token)).build();
        this.client = discordClient.gateway()
                .setGatewayReactorResources(reactorResources -> GatewayReactorResources.builder(discordClient.getCoreResources().getReactorResources()).build())
                .setEnabledIntents((IntentSet.of(Intent.MESSAGE_CONTENT, Intent.GUILD_MESSAGES)))
                .setInitialPresence(shardInfo -> disconnectedPresence)
                .login()
                .block();
        restClient = client.getRestClient();
        mainRestChannel = restClient.getChannelById(Snowflake.of(CONFIG.discord.channelId));
        if (CONFIG.discord.chatRelay.enable)
            relayRestChannel = restClient.getChannelById(Snowflake.of(CONFIG.discord.chatRelay.channelId));
        client.getEventDispatcher().on(MessageCreateEvent.class).subscribe(event -> {
            if (CONFIG.discord.chatRelay.enable && !CONFIG.discord.chatRelay.channelId.isEmpty() && event.getMessage().getChannelId().equals(Snowflake.of(CONFIG.discord.chatRelay.channelId))) {
                if (!event.getMember().get().getId().equals(this.client.getSelfId())) {
                    EVENT_BUS.postAsync(new DiscordMessageSentEvent(sanitizeRelayInputMessage(event.getMessage().getContent()), event));
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
                    CommandOutputHelper.logEmbedOutputToTerminal(context.getEmbed());
                }
                if (!context.getMultiLineOutput().isEmpty()) {
                    for (final String line : context.getMultiLineOutput()) {
                        mainChannelMessageQueue.add(MessageCreateSpec.builder().content(line).build().asRequest());
                    }
                    CommandOutputHelper.logMultiLineOutputToTerminal(context);
                }
            } catch (final Exception e) {
                DISCORD_LOG.error("Failed processing discord command: {}", message, e);
            }
        });
    }

    public synchronized void start() {
        createClient();
        initEventHandlers();

        if (CONFIG.discord.isUpdating) {
            handleProxyUpdateComplete();
        }
        this.presenceUpdateFuture = SCHEDULED_EXECUTOR_SERVICE.scheduleAtFixedRate(this::updatePresence, 0L,
                15L, // discord rate limit
                TimeUnit.SECONDS);
        this.mainChannelMessageQueueProcessFuture = SCHEDULED_EXECUTOR_SERVICE.scheduleAtFixedRate(this::processMessageQueue, 0L, 100L, TimeUnit.MILLISECONDS);
        if (CONFIG.discord.chatRelay.enable)
            this.relayChannelMessageQueueProcessFuture = SCHEDULED_EXECUTOR_SERVICE.scheduleAtFixedRate(this::processRelayMessageQueue, 0L, 100L, TimeUnit.MILLISECONDS);
        this.isRunning = true;
    }

    public void setBotNickname(final String nick) {
        try {
            final Id guildId = mainRestChannel.getData().block().guildId().get();
            this.client.getGuildById(Snowflake.of(guildId))
                .flatMap(g -> g.changeSelfNickname(nick))
                .block();
        } catch (final Exception e) {
            DISCORD_LOG.warn("Failed updating bot's nickname. Check that the bot has correct permissions");
            DISCORD_LOG.debug("Failed updating bot's nickname. Check that the bot has correct permissions", e);
        }
    }

    public void setBotDescription(String description) {
        try {
            restClient.getApplicationService()
                .setCurrentApplicationInfo(ApplicationEditSpec.builder()
                                               .description(description)
                                               .build()
                                               .asRequest())
                .block();
        } catch (final Exception e) {
            DISCORD_LOG.warn("Failed updating bot's description. Check that the bot has correct permissions");
            DISCORD_LOG.debug("Failed updating bot's description", e);
        }
    }

    public synchronized void stop(boolean clearQueue) {
        if (!this.isRunning) return;
        if (this.presenceUpdateFuture != null)
            this.presenceUpdateFuture.cancel(true);
        if (this.mainChannelMessageQueueProcessFuture != null)
            this.mainChannelMessageQueueProcessFuture.cancel(true);
        if (this.relayChannelMessageQueueProcessFuture != null)
            this.relayChannelMessageQueueProcessFuture.cancel(true);
        EVENT_BUS.unsubscribe(this);
        if (client != null) {
            client.logout().block();
            client = null;
        }
        if (restClient != null) restClient = null;
        if (mainRestChannel != null) mainRestChannel = null;
        if (relayRestChannel != null) relayRestChannel = null;
        if (clearQueue) {
            this.mainChannelMessageQueue.clear();
            this.relayChannelMessageQueue.clear();
        }
        this.isRunning = false;
    }

    private MultipartRequest<MessageCreateRequest> commandEmbedOutputToMessage(final CommandContext context) {
        var embed = context.getEmbed();
        if (embed.title() == null) return null;
        var msgBuilder = MessageCreateSpec.builder()
            .addEmbed(embed.toSpec());
        if (embed.fileAttachment() != null) {
            msgBuilder.addFile(embed.fileAttachment.name(), new ByteArrayInputStream(embed.fileAttachment.data()));
        }
        return msgBuilder
            .build().asRequest();
    }

    private void processMessageQueue() {
        try {
            var message = mainChannelMessageQueue.peek();
            if (nonNull(message)) {
                this.mainRestChannel.createMessage(message).retry(1).block();
                mainChannelMessageQueue.poll();
            }
        } catch (final Throwable e) {
            DISCORD_LOG.error("Failed sending message to main channel. Check bot permissions.");
            DISCORD_LOG.debug("Failed sending message to main channel. Check bot permissions.", e);
            if (mainChannelMessageQueue.size() > 100) {
                DISCORD_LOG.error("Flushing main channel message queue to reclaim memory, current size: {}", mainChannelMessageQueue.size());
                mainChannelMessageQueue.clear();
            }
        }
    }

    private void processRelayMessageQueue() {
        try {
            var message = relayChannelMessageQueue.peek();
            if (nonNull(message)
                && (!message.getJsonPayload().embeds().isAbsent()
                    || !(message.getJsonPayload().content().isAbsent() || message.getJsonPayload().content().get().isEmpty())))
                this.relayRestChannel.createMessage(message).retry(1).block();
            relayChannelMessageQueue.poll();
        } catch (final Throwable e) {
            DISCORD_LOG.error("Failed sending message to relay channel. Check bot permissions.");
            DISCORD_LOG.debug("Failed sending message to relay channel. Check bot permissions.", e);
            if (relayChannelMessageQueue.size() > 100) {
                DISCORD_LOG.error("Flushing relay channel message queue to reclaim memory, current size: {}", relayChannelMessageQueue.size());
                relayChannelMessageQueue.clear();
            }
        }
    }

    private void updatePresence() {
        try {
            if (LAUNCH_CONFIG.auto_update) {
                final AutoUpdater autoUpdater = Proxy.getInstance().getAutoUpdater();
                if (autoUpdater != null
                    && autoUpdater.getUpdateAvailable()
                    && Math.random() > 0.75 // 25% chance to show update available
                ) {
                    this.client.updatePresence(getUpdateAvailablePresence(autoUpdater))
                        .block();
                    return;
                }
            }
            if (Proxy.getInstance().autoReconnectIsInProgress()) {
                this.client.updatePresence(autoReconnectingPresence)
                    .block();
                return;
            }
            if (Proxy.getInstance().isInQueue())
                this.client.updatePresence(getQueuePresence()).block();
            else if (Proxy.getInstance().isConnected())
                this.client.updatePresence(getOnlinePresence()).block();
            else
                this.client.updatePresence(disconnectedPresence).block();
        } catch (final IllegalStateException e) {
            if (e.getMessage().contains("Backpressure overflow")) {
                DISCORD_LOG.error("Caught backpressure overflow, restarting discord session");
                SCHEDULED_EXECUTOR_SERVICE.execute(() -> {
                    this.stop(false);
                    this.start();
                });
            } else throw e;
        } catch (final Exception e) {
            DISCORD_LOG.error("Failed updating discord presence. Check that the bot has correct permissions.");
            DISCORD_LOG.debug("Failed updating discord presence. Check that the bot has correct permissions.", e);
        }
    }

    private ClientPresence getUpdateAvailablePresence(final AutoUpdater autoUpdater) {
        return ClientPresence.of(Status.ONLINE, ClientActivity.custom(
            "Update Available" + autoUpdater.getNewVersion().map(v -> ": " + v).orElse("")));
    }

    private ClientPresence getOnlinePresence() {
        long onlineSeconds = Instant.now().getEpochSecond() - Proxy.getInstance().getConnectTime().getEpochSecond();
        return ClientPresence.of(Status.ONLINE, ClientActivity.custom(
      (Proxy.getInstance().isOn2b2t() ? "2b2t" : CONFIG.client.server.address) + " [" + Queue.getEtaStringFromSeconds(onlineSeconds) + "]"));
    }

    private void handleProxyUpdateComplete() {
        CONFIG.discord.isUpdating = false;
        saveConfigAsync();
        sendEmbedMessage(Embed.builder()
                             .title("Update complete!")
                             .description("Current Version: `" + escape(LAUNCH_CONFIG.version) + "`")
                             .color(Color.GREEN));
    }

    private void sendQueueWarning() {
        sendEmbedMessage((CONFIG.discord.queueWarning.mentionRole ? mentionAccountOwner() : ""), Embed.builder()
            .title("Queue Warning")
            .addField("Queue Position", "[" + queuePositionStr() + "]", false)
            .color(Color.MOON_YELLOW));
    }

    private String mentionAccountOwner() {
        return mentionRole(CONFIG.discord.accountOwnerRoleId);
    }

    private String mentionRole(final String roleId) {
        try {
            return MentionUtil.forRole(Snowflake.of(roleId));
        } catch (final NumberFormatException e) {
            DISCORD_LOG.error("Unable to generate mention for role ID: {}", roleId, e);
            return "";
        }
    }

    private String queuePositionStr() {
        if (Proxy.getInstance().isPrio())
            return Proxy.getInstance().getQueuePosition() + " / " + Queue.getQueueStatus().prio() + " - ETA: " + Queue.getQueueEta(Proxy.getInstance().getQueuePosition());
        else
            return Proxy.getInstance().getQueuePosition() + " / " + Queue.getQueueStatus().regular() + " - ETA: " + Queue.getQueueEta(Proxy.getInstance().getQueuePosition());
    }

    static boolean validateButtonInteractionEventFromAccountOwner(final ButtonInteractionEvent event) {
        return event.getInteraction().getMember()
            .map(m -> m.getRoleIds().stream()
                .map(Snowflake::asString)
                .anyMatch(roleId -> roleId.equals(CONFIG.discord.accountOwnerRoleId)))
            .orElse(false);
    }

    private Embed getUpdateMessage(final Optional<String> newVersion) {
        String verString = "Current Version: `" + escape(LAUNCH_CONFIG.version) + "`";
        if (newVersion.isPresent()) verString += "\nNew Version: `" + escape(newVersion.get()) + "`";
        return Embed.builder()
            .title("Updating and restarting...")
            .description(verString)
            .color(Color.CYAN);
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
            this.client.edit(UserEditSpec.builder()
                                     .avatar(Image.ofRaw(imageBytes, Image.Format.PNG))
                                     .build())
                .block();
        } catch (final Exception e) {
            DISCORD_LOG.warn("Failed updating discord profile image. Check that the bot has correct permissions");
            DISCORD_LOG.debug("Failed updating discord profile image. Check that the bot has correct permissions", e);
        }
    }

    public void sendEmbedMessageWithFileAttachment(Embed embed) {
        try {
            var msgBuilder = MessageCreateSpec.builder()
                .addEmbed(embed.toSpec());
            if (embed.fileAttachment() != null) {
                msgBuilder.addFile(embed.fileAttachment.name(), new ByteArrayInputStream(embed.fileAttachment.data()));
            }
            mainChannelMessageQueue.add(msgBuilder.build().asRequest());
            CommandOutputHelper.logEmbedOutputToTerminal(embed);
        } catch (final Exception e) {
            DISCORD_LOG.error("Failed sending discord embed message. Check that the bot has correct permissions");
            DISCORD_LOG.debug("Failed sending discord embed message. Check that the bot has correct permissions", e);
        }
    }

    public void sendEmbedMessage(Embed embed) {
        try {
            mainChannelMessageQueue.add(MessageCreateSpec.builder()
                                            .addEmbed(embed.toSpec())
                                            .build().asRequest());
            CommandOutputHelper.logEmbedOutputToTerminal(embed);
        } catch (final Exception e) {
            DISCORD_LOG.error("Failed sending discord embed message. Check that the bot has correct permissions");
            DISCORD_LOG.debug("Failed sending discord embed message. Check that the bot has correct permissions", e);
        }
    }

    public void sendRelayEmbedMessage(Embed embedCreateSpec) {
        relayChannelMessageQueue.add(MessageCreateSpec.builder()
                                        .addEmbed(embedCreateSpec.toSpec())
                                        .build().asRequest());
    }

    private void sendEmbedMessage(String message, Embed embed) {
        mainChannelMessageQueue.add(MessageCreateSpec.builder()
                                        .content(message)
                                        .addEmbed(embed.toSpec())
                                        .build().asRequest());
        TERMINAL_LOG.info(message);
        CommandOutputHelper.logEmbedOutputToTerminal(embed);
    }

    private void sendRelayEmbedMessage(String message, Embed embed) {
        relayChannelMessageQueue.add(MessageCreateSpec.builder()
                                         .content(message)
                                         .addEmbed(embed.toSpec())
                                         .build().asRequest());
    }

    public void sendMessage(final String message) {
        mainChannelMessageQueue.add(MessageCreateSpec.builder()
                                        .content(message)
                                        .build().asRequest());
        TERMINAL_LOG.info(message);
    }

    public void sendRelayMessage(final String message) {
        relayChannelMessageQueue.add(MessageCreateSpec.builder()
                                         .content(message)
                                         .build().asRequest());
    }

    private void sendEmbedMessageWithButtons(String message, Embed embed, List<Button> buttons, Function<ButtonInteractionEvent, Publisher<Mono<?>>> mapper, Duration timeout) {
        mainChannelMessageQueue.add(MessageCreateSpec.builder()
                                        .content(message)
                                        .addEmbed(embed.toSpec())
                                        .components(ActionRow.of(buttons))
                                        .build().asRequest());
        TERMINAL_LOG.info(message);
        CommandOutputHelper.logEmbedOutputToTerminal(embed);
        client.getEventDispatcher()
            .on(ButtonInteractionEvent.class, mapper)
            .timeout(timeout)
            .onErrorResume(TimeoutException.class, e -> Mono.empty())
            .subscribe();
    }

    private void sendEmbedMessageWithButtons(Embed embed, List<Button> buttons, Function<ButtonInteractionEvent, Publisher<Mono<?>>> mapper, Duration timeout) {
        mainChannelMessageQueue.add(MessageCreateSpec.builder()
                                        .addEmbed(embed.toSpec())
                                        .components(ActionRow.of(buttons))
                                        .build().asRequest());
        CommandOutputHelper.logEmbedOutputToTerminal(embed);
        client.getEventDispatcher()
            .on(ButtonInteractionEvent.class, mapper)
            .timeout(timeout)
            .onErrorResume(TimeoutException.class, e -> Mono.empty())
            .subscribe();
    }

    private ClientPresence getQueuePresence() {
        return ClientPresence.of(Status.IDLE, ClientActivity.custom(queuePositionStr()));
    }

    public static String escape(String message) {
        return message.replaceAll("_", "\\\\_");
    }

    public boolean isMessageQueueEmpty() {
        return mainChannelMessageQueue.isEmpty();
    }

    public DiscordClientBuilder<DiscordClient, RouterOptions> buildProxiedClient(final DiscordClientBuilder<DiscordClient, RouterOptions> builder) {
        HttpClient httpClient = HttpClient.create().resolver(DefaultAddressResolverGroup.INSTANCE).compress(true).followRedirect(true).secure();
        if (CONFIG.discord.connectionProxy.enabled)
            httpClient = getProxiedHttpClient(httpClient);
        builder.setReactorResources(new ReactorResources(httpClient,
                                                         DEFAULT_TIMER_TASK_SCHEDULER.get(),
                                                         DEFAULT_BLOCKING_TASK_SCHEDULER.get()));
        return builder;
    }

    public HttpClient getProxiedHttpClient(HttpClient baseClient) {
        return baseClient.proxy((ProxyProvider.TypeSpec provider) -> {
            var proxy = switch (CONFIG.discord.connectionProxy.type){
                case SOCKS4 -> ProxyProvider.Proxy.SOCKS4;
                case SOCKS5 -> ProxyProvider.Proxy.SOCKS5;
                case HTTP -> ProxyProvider.Proxy.HTTP;
            };
            var proxyBuilder = provider
                .type(proxy)
                .host(CONFIG.discord.connectionProxy.host)
                .port(CONFIG.discord.connectionProxy.port);
            if(!CONFIG.discord.connectionProxy.user.isEmpty())
                proxyBuilder.username(CONFIG.discord.connectionProxy.user);
            if(!CONFIG.discord.connectionProxy.password.isEmpty())
                proxyBuilder.password(s -> CONFIG.discord.connectionProxy.password);
        });
    }

    private String extractRelayEmbedSenderUsername(final Possible<Integer> color, final String msgContent) {
        final String sender;
        if (!color.isAbsent() && color.get() == Color.MAGENTA.getRGB()) {
            // extract whisper sender
            sender = msgContent.split("\\*\\*")[1];
        } else if (!color.isAbsent() && color.get().equals(Color.BLACK.getRGB())) {
            // extract public chat sender
            sender = msgContent.split("\\*\\*")[1].replace(":", "");
        // todo: we could support death messages here if we remove any bolded discord formatting and feed the message content into the parser
        } else {
            throw new RuntimeException("Unhandled message being replied to, aborting relay");
        }
        return sender;
    }

    public void handleConnectEvent(ConnectEvent event) {
        sendEmbedMessage(Embed.builder()
                .title("Proxy Connected")
                .color(Color.MOON_YELLOW)
                .addField("Server", CONFIG.client.server.address, true)
                .addField("Proxy IP", CONFIG.server.getProxyAddress(), false));
        this.client.updatePresence(defaultConnectedPresence.get()).block();
    }

    public void handlePlayerOnlineEvent(PlayerOnlineEvent event) {
        var embedBuilder = Embed.builder()
            .title("Proxy Online")
            .color(Color.MEDIUM_SEA_GREEN);
        event.queueWait()
            .ifPresent(duration -> embedBuilder.addField("Queue Duration", formatDuration(duration), true));
        sendEmbedMessage(embedBuilder);
    }

    public void handleDisconnectEvent(DisconnectEvent event) {
        var embed = Embed.builder()
            .title("Proxy Disconnected")
            .addField("Reason", event.reason(), false)
            .addField("Online Duration", formatDuration(event.onlineDuration()), false)
            .color(Color.RUBY);
        if (Proxy.getInstance().isOn2b2t()
            && !Proxy.getInstance().isPrio()
            && event.reason().startsWith("You have lost connection")
            && event.onlineDuration().toSeconds() >= 0L
            && event.onlineDuration().toSeconds() <= 1L) {
            embed.description("You have likely been kicked for reaching the 2b2t non-prio account IP limit."
                                  + "\nConsider configuring a connection proxy with the `clientConnection` command."
                                  + "\nOr migrate ZenithProxy instances to multiple hosts/IP's.");
        }
        sendEmbedMessage(embed);
        SCHEDULED_EXECUTOR_SERVICE.execute(() -> this.client.updatePresence(disconnectedPresence).block());
    }

    public void handleQueuePositionUpdateEvent(QueuePositionUpdateEvent event) {
        if (CONFIG.discord.queueWarning.enabled) {
            if (event.position() == CONFIG.discord.queueWarning.position) {
                sendQueueWarning();
            } else if (event.position() <= 3) {
                sendQueueWarning();
            }
        }
        this.client.updatePresence(getQueuePresence()).block();
    }

    public void handleAutoEatOutOfFoodEvent(final AutoEatOutOfFoodEvent event) {
        sendEmbedMessage(Embed.builder()
                .title("AutoEat Out Of Food")
                .description("AutoEat threshold met but player has no food")
                .color(Color.RUBY));
    }

    public void handleQueueCompleteEvent(QueueCompleteEvent event) {
        this.client.updatePresence(defaultConnectedPresence.get()).block();
    }

    public void handleStartQueueEvent(StartQueueEvent event) {
        sendEmbedMessage(Embed.builder()
                .title("Started Queuing")
                .color(Color.MOON_YELLOW)
                .addField("Regular Queue", Queue.getQueueStatus().regular(), true)
                .addField("Priority Queue", Queue.getQueueStatus().prio(), true));
        this.client.updatePresence(getQueuePresence()).block();
    }

    public void handleDeathEvent(DeathEvent event) {
        sendEmbedMessage(Embed.builder()
                .title("Player Death")
                .color(Color.RUBY)
                .addField("Coordinates", getCoordinates(CACHE.getPlayerCache()), false));
    }

    public void handleSelfDeathMessageEvent(SelfDeathMessageEvent event) {
        sendEmbedMessage(Embed.builder()
                .title("Death Message")
                .color(Color.RUBY)
                .addField("Message", event.message(), false));
    }

    public void handleHealthAutoDisconnectEvent(HealthAutoDisconnectEvent event) {
        sendEmbedMessage(Embed.builder()
                .title("Health AutoDisconnect Triggered")
                .addField("Health", CACHE.getPlayerCache().getThePlayer().getHealth(), true)
                .color(Color.CYAN));
    }

    public void handleProxyClientConnectedEvent(ProxyClientConnectedEvent event) {
        if (CONFIG.client.extra.clientConnectionMessages) {
            sendEmbedMessage(Embed.builder()
                    .title("Client Connected")
                    .addField("Username", event.clientGameProfile().getName(), true)
                    .color(Color.CYAN));
        }
    }

    public void handleProxySpectatorConnectedEvent(ProxySpectatorConnectedEvent event) {
        if (CONFIG.client.extra.clientConnectionMessages) {
            sendEmbedMessage(Embed.builder()
                    .title("Spectator Connected")
                    .addField("Username", escape(event.clientGameProfile().getName()), true)
                    .color(Color.CYAN));
        }
    }

    public void handleProxyClientDisconnectedEvent(ProxyClientDisconnectedEvent event) {
        if (!CONFIG.client.extra.clientConnectionMessages) return;
        var builder = Embed.builder()
                .title("Client Disconnected")
                .color(Color.RUBY);
        if (nonNull(event.clientGameProfile())) {
            builder = builder.addField("Username", escape(event.clientGameProfile().getName()), false);
        }
        if (nonNull(event.reason())) {
            builder = builder.addField("Reason", escape(event.reason()), false);
        }
        sendEmbedMessage(builder);
    }

    public void handleNewPlayerInVisualRangeEvent(NewPlayerInVisualRangeEvent event) {
        if (!CONFIG.client.extra.visualRangeAlert) return;
        boolean isFriend = PLAYER_LISTS.getFriendsList().contains(event.playerEntity().getUuid());
        if (isFriend && CONFIG.client.extra.visualRangeIgnoreFriends) {
            DISCORD_LOG.debug("Ignoring visual range alert for friend: " + event.playerEntry().getName());
            return;
        }
        var embedCreateSpec = Embed.builder()
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
                PLAYER_LISTS.getFriendsList().add(event.playerEntry().getName());
                e.reply().withEmbeds(Embed.builder()
                        .title("Friend Added")
                        .color(Color.GREEN)
                        .addField("Player Name", escape(event.playerEntry().getName()), true)
                        .addField("Player UUID", ("[" + event.playerEntry().getProfileId() + "](https://namemc.com/profile/" + event.playerEntry().getProfileId() + ")"), true)
                        .thumbnail(Proxy.getInstance().getAvatarURL(event.playerEntry().getProfileId()).toString())
                         .toSpec())
                    .block();
                saveConfigAsync();
            }
            return Mono.empty();
        };
        if (CONFIG.client.extra.visualRangeAlertMention) {
            if (!isFriend) {
                if (CONFIG.discord.visualRangeMentionRoleId.length() > 3) {
                    sendEmbedMessageWithButtons(mentionRole(CONFIG.discord.visualRangeMentionRoleId), embedCreateSpec, buttons, mapper, Duration.ofHours(1));
                } else {
                    sendEmbedMessageWithButtons(mentionAccountOwner(), embedCreateSpec, buttons, mapper, Duration.ofHours(1));
                }
            } else {
                sendEmbedMessage(embedCreateSpec);
            }
        } else {
            if (!isFriend) {
                sendEmbedMessageWithButtons(embedCreateSpec, buttons, mapper, Duration.ofHours(1));
            } else {
                sendEmbedMessage(embedCreateSpec);
            }
        }
    }

    public void handlePlayerLeftVisualRangeEvent(final PlayerLeftVisualRangeEvent event) {
        if (!CONFIG.client.extra.visualRangeLeftAlert) return;
        boolean isFriend = PLAYER_LISTS.getFriendsList().contains(event.playerEntity().getUuid());
        if (isFriend && CONFIG.client.extra.visualRangeIgnoreFriends) {
            DISCORD_LOG.debug("Ignoring visual range left alert for friend: " + event.playerEntry().getName());
            return;
        }
        var embedCreateSpec = Embed.builder()
            .title("Player Left Visual Range")
            .color(isFriend ? Color.GREEN : Color.RUBY)
            .addField("Player Name", escape(event.playerEntry().getName()), true)
            .addField("Player UUID", ("[" + event.playerEntity().getUuid() + "](https://namemc.com/profile/" + event.playerEntry().getProfileId().toString() + ")"), true)
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

    public void handlePlayerLogoutInVisualRangeEvent(final PlayerLogoutInVisualRangeEvent event) {
        if (!CONFIG.client.extra.visualRangeLeftAlert || !CONFIG.client.extra.visualRangeLeftLogoutAlert) return;
        boolean isFriend = PLAYER_LISTS.getFriendsList().contains(event.playerEntry().getProfileId());
        if (isFriend && CONFIG.client.extra.visualRangeIgnoreFriends) {
            DISCORD_LOG.debug("Ignoring visual range logout alert for friend: " + event.playerEntry().getName());
            return;
        }
        var embedCreateSpec = Embed.builder()
            .title("Player Logout In Visual Range")
            .color(isFriend ? Color.GREEN : Color.RUBY)
            .addField("Player Name", escape(event.playerEntry().getName()), true)
            .addField("Player UUID", ("[" + event.playerEntity().getUuid() + "](https://namemc.com/profile/" + event.playerEntry().getProfileId().toString() + ")"), true)
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
                .color(Color.RUBY);
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
                                .color(Color.GREEN)
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
                                .color(Color.RUBY)
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
                    .color(Color.RUBY);
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
        sendEmbedMessage(Embed.builder()
                .title("Active Hours Connect Triggered")
                .addField("ETA", Queue.getQueueEta(queueLength), false)
                .color(Color.CYAN));
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
                message = message.replace(death.getVictim(), "**" + death.getVictim() + "**");
                var k = death.getKiller().filter(killer -> killer.getType() == KillerType.PLAYER);
                if (k.isPresent()) message = message.replace(k.get().getName(), "**" + k.get().getName() + "**");
                senderName = death.getVictim();
                senderUUID = CACHE.getTabListCache().getFromName(death.getVictim()).map(PlayerListEntry::getProfileId).orElse(null);
            } else {
                if (!CONFIG.discord.chatRelay.serverMessages) return;
                senderName = "Hausemaster";
                senderUUID = null;
            }
            final String avatarURL = senderUUID != null ? Proxy.getInstance().getAvatarURL(senderUUID).toString() : Proxy.getInstance().getAvatarURL(senderName).toString();
            var embed = Embed.builder()
                .description(escape(message))
                .footer("\u200b", avatarURL)
                .color(event.isPublicChat() ? (event.message().startsWith(">") ? Color.MEDIUM_SEA_GREEN : Color.BLACK)
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
            if (CONFIG.discord.chatRelay.ignoreQueue && Proxy.getInstance().isInQueue()) return;
            sendRelayEmbedMessage(Embed.builder()
                                      .description(escape("**" + event.playerEntry().getName() + "** connected"))
                                      .color(Color.MEDIUM_SEA_GREEN)
                                      .footer("\u200b", Proxy.getInstance().getAvatarURL(event.playerEntry().getProfileId()).toString())
                                      .timestamp(Instant.now()));
        }
        if (CONFIG.client.extra.stalk.enabled && PLAYER_LISTS.getStalkList().contains(event.playerEntry().getProfile())) {
            sendEmbedMessage(mentionAccountOwner(), Embed.builder()
                .title("Stalked Player Online!")
                .color(Color.MEDIUM_SEA_GREEN)
                .addField("Player Name", event.playerEntry().getName(), true)
                .thumbnail(Proxy.getInstance().getAvatarURL(event.playerEntry().getProfileId()).toString()));
        }
    }

    public void handleServerPlayerDisconnectedEvent(ServerPlayerDisconnectedEvent event) {
        if (CONFIG.discord.chatRelay.enable && CONFIG.discord.chatRelay.connectionMessages && !CONFIG.discord.chatRelay.channelId.isEmpty()) {
            if (CONFIG.discord.chatRelay.ignoreQueue && Proxy.getInstance().isInQueue()) return;
            sendRelayEmbedMessage(Embed.builder()
                                      .description(escape("**" + event.playerEntry().getName() + "** disconnected"))
                                      .color(Color.RUBY)
                                      .footer("\u200b", Proxy.getInstance().getAvatarURL(event.playerEntry().getProfileId()).toString())
                                      .timestamp(Instant.now()));
        }
        if (CONFIG.client.extra.stalk.enabled && PLAYER_LISTS.getStalkList().contains(event.playerEntry().getProfile())) {
            sendEmbedMessage(mentionAccountOwner(), Embed.builder()
                .title("Stalked Player Offline!")
                .color(Color.RUBY)
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
                if (this.client.getSelfId().asLong() != messageData.author().id().asLong()) return;
                final EmbedData embed = messageData.embeds().get(0);
                final String sender = extractRelayEmbedSenderUsername(embed.color(), embed.description().get());
                Proxy.getInstance().getClient().sendAsync(new ServerboundChatPacket("/w " + sender + " " + event.message()));
            } catch (final Exception e) {
                DISCORD_LOG.error("Error performing chat relay reply", e);
            }
        } else Proxy.getInstance().getClient().sendAsync(new ServerboundChatPacket(event.message()));
        lastRelaymessage = Optional.of(Instant.now());
    }

    public void handleUpdateStartEvent(UpdateStartEvent event) {
        sendEmbedMessage(getUpdateMessage(event.newVersion()));
    }

    public void handleServerRestartingEvent(ServerRestartingEvent event) {
        sendEmbedMessage(Embed.builder()
                .title("Server Restarting")
                .color(Color.RUBY)
                .addField("Message", event.message(), true));
    }

    public void handleProxyLoginFailedEvent(ProxyLoginFailedEvent event) {
        sendEmbedMessage(Embed.builder()
                .title("Login Failed")
                .color(Color.RUBY)
                .addField("Help", "Try waiting and connecting again.", false));
    }

    public void handleStartConnectEvent(StartConnectEvent event) {
        sendEmbedMessage(Embed.builder()
                .title("Connecting...")
                .color(Color.MOON_YELLOW));
    }

    public void handlePrioStatusUpdateEvent(PrioStatusUpdateEvent event) {
        if (!CONFIG.client.extra.prioStatusChangeMention) return;
        var embedCreateSpec = Embed.builder();
        if (event.prio()) {
            embedCreateSpec
                    .title("Prio Queue Status Detected")
                    .color(Color.GREEN);
        } else {
            embedCreateSpec
                    .title("Prio Queue Status Lost")
                    .color(Color.RED);
        }
        embedCreateSpec.addField("User", escape(CONFIG.authentication.username), false);
        sendEmbedMessage((CONFIG.discord.mentionRoleOnPrioUpdate ? mentionAccountOwner() : ""), embedCreateSpec);
    }

    public void handlePrioBanStatusUpdateEvent(PrioBanStatusUpdateEvent event) {
        var embedCreateSpec = Embed.builder();
        if (event.prioBanned()) {
            embedCreateSpec
                    .title("Prio Ban Detected")
                    .color(Color.RED);
        } else {
            embedCreateSpec
                    .title("Prio Unban Detected")
                    .color(Color.GREEN);
        }
        embedCreateSpec.addField("User", escape(CONFIG.authentication.username), false);
        sendEmbedMessage((CONFIG.discord.mentionRoleOnPrioBanUpdate ? mentionAccountOwner() : ""), embedCreateSpec);
    }

    public void handleAutoReconnectEvent(final AutoReconnectEvent event) {
        sendEmbedMessage(Embed.builder()
                .title("AutoReconnecting in " + event.delaySeconds() + "s")
                .color(Color.MOON_YELLOW));
    }

    public void handleMsaDeviceCodeLoginEvent(final MsaDeviceCodeLoginEvent event) {
        final var embed = Embed.builder()
            .title("Microsoft Device Code Login")
            .color(Color.CYAN)
            .description("Login Here: " + event.deviceCode().getDirectVerificationUri());
        if (CONFIG.discord.mentionRoleOnDeviceCodeAuth)
            sendEmbedMessage(mentionAccountOwner(), embed);
        else
            sendEmbedMessage(embed);
    }

    public void handleDeathMessageEvent(final DeathMessageEvent event) {
        if (!CONFIG.client.extra.killMessage) return;
        event.deathMessageParseResult().getKiller().ifPresent(killer -> {
            if (!killer.getName().equals(CONFIG.authentication.username)) return;
            sendEmbedMessage(Embed.builder()
                                 .title("Kill Detected")
                                 .color(Color.CYAN)
                                 .addField("Victim", escape(event.deathMessageParseResult().getVictim()), false)
                                 .addField("Message", escape(event.deathMessageRaw()), false));
        });
    }

    public void handleUpdateAvailableEvent(final UpdateAvailableEvent event) {
        var embedBuilder = Embed.builder()
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
        sendEmbedMessage(embedBuilder);
    }

    public void handleReplayStartedEvent(final ReplayStartedEvent event) {
        sendEmbedMessage(Embed.builder()
                             .title("Replay Recording Started")
                             .color(Color.CYAN));
    }

    public void handleReplayStoppedEvent(final ReplayStoppedEvent event) {
        var embed = Embed.builder()
            .title("Replay Recording Stopped")
            .color(Color.CYAN);
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
            .color(Color.RUBY);
        if (CONFIG.client.extra.autoTotem.totemPopAlertMention)
            sendEmbedMessage(mentionAccountOwner(), embed);
        else
            sendEmbedMessage(embed);
    }

    public void handleNoTotemsEvent(final NoTotemsEvent event) {
        var embed = Embed.builder()
            .title("Player Out of Totems")
            .color(Color.RUBY);
        if (CONFIG.client.extra.autoTotem.noTotemsAlertMention)
            sendEmbedMessage(mentionAccountOwner(), embed);
        else
            sendEmbedMessage(embed);
    }
}
