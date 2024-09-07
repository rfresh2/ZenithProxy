package com.zenith.discord;

import com.zenith.Proxy;
import com.zenith.command.brigadier.CommandContext;
import com.zenith.command.brigadier.DiscordCommandContext;
import com.zenith.command.util.CommandOutputHelper;
import com.zenith.event.proxy.DiscordMessageSentEvent;
import com.zenith.feature.autoupdater.AutoUpdater;
import com.zenith.feature.queue.Queue;
import com.zenith.module.impl.AutoReconnect;
import discord4j.common.ReactorResources;
import discord4j.common.close.CloseException;
import discord4j.common.sinks.EmissionStrategy;
import discord4j.common.store.Store;
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
import discord4j.discordjson.json.MessageCreateRequest;
import discord4j.discordjson.possible.Possible;
import discord4j.gateway.GatewayReactorResources;
import discord4j.gateway.intent.Intent;
import discord4j.gateway.intent.IntentSet;
import discord4j.rest.RestClient;
import discord4j.rest.entity.RestChannel;
import discord4j.rest.http.client.ClientException;
import discord4j.rest.request.RequestQueueFactory;
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
import reactor.util.concurrent.Queues;

import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.zenith.Shared.*;
import static discord4j.common.ReactorResources.DEFAULT_BLOCKING_TASK_SCHEDULER;
import static discord4j.common.ReactorResources.DEFAULT_TIMER_TASK_SCHEDULER;
import static java.util.Objects.nonNull;

public class DiscordBot {
    public static final ClientPresence autoReconnectingPresence = ClientPresence.of(Status.IDLE, ClientActivity.custom(
        "AutoReconnecting..."));
    private static final Duration BLOCK_TIMEOUT = Duration.ofSeconds(10);
    private RestClient restClient;
    private RestChannel mainRestChannel;
    private RestChannel relayRestChannel;
    GatewayDiscordClient client;
    // Main channel discord message FIFO queue
    private final ConcurrentLinkedQueue<MultipartRequest<MessageCreateRequest>> mainChannelMessageQueue;
    private final ConcurrentLinkedQueue<MultipartRequest<MessageCreateRequest>> relayChannelMessageQueue;
    final ClientPresence disconnectedPresence = ClientPresence.of(Status.DO_NOT_DISTURB, ClientActivity.custom(
        "Disconnected"));
    final Supplier<ClientPresence> defaultConnectedPresence = () -> ClientPresence.of(Status.ONLINE, ClientActivity.custom(
        (Proxy.getInstance().isOn2b2t() ? "2b2t" : CONFIG.client.server.address)));
    public Optional<Instant> lastRelaymessage = Optional.empty();

    @Getter
    private boolean isRunning;
    private ScheduledFuture<?> presenceUpdateFuture;
    private ScheduledFuture<?> mainChannelMessageQueueProcessFuture;
    private ScheduledFuture<?> relayChannelMessageQueueProcessFuture;

    private final DiscordEventListener eventListener = new DiscordEventListener(this);

    public DiscordBot() {
        this.mainChannelMessageQueue = new ConcurrentLinkedQueue<>();
        this.relayChannelMessageQueue = new ConcurrentLinkedQueue<>();
        this.isRunning = false;
    }

    public void initEventHandlers() {
        eventListener.subscribeEvents();
    }

    public synchronized void start() {
        createClient();
        initEventHandlers();

        if (CONFIG.discord.isUpdating) {
            handleProxyUpdateComplete();
        }
        this.presenceUpdateFuture = EXECUTOR.scheduleAtFixedRate(this::updatePresence, 0L,
                                                                 15L, // discord rate limit
                                                                 TimeUnit.SECONDS);
        this.mainChannelMessageQueueProcessFuture = EXECUTOR.scheduleAtFixedRate(this::processMessageQueue, 0L, 100L, TimeUnit.MILLISECONDS);
        if (CONFIG.discord.chatRelay.enable)
            this.relayChannelMessageQueueProcessFuture = EXECUTOR.scheduleAtFixedRate(this::processRelayMessageQueue, 0L, 100L, TimeUnit.MILLISECONDS);
        this.isRunning = true;
    }

    public synchronized void stop(boolean clearQueue) {
        if (!this.isRunning) return;
        if (this.presenceUpdateFuture != null)
            this.presenceUpdateFuture.cancel(true);
        if (this.mainChannelMessageQueueProcessFuture != null)
            this.mainChannelMessageQueueProcessFuture.cancel(true);
        if (this.relayChannelMessageQueueProcessFuture != null)
            this.relayChannelMessageQueueProcessFuture.cancel(true);
        EVENT_BUS.unsubscribe(eventListener);
        if (client != null) {
            try {
                client.logout().block(Duration.ofSeconds(20));
            } catch (final Throwable e){
                DISCORD_LOG.error("Failed logging out of discord. Things might break?", e);
                // fall through
            }
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
        client = buildGatewayClient();
        restClient = client.getRestClient();
        mainRestChannel = restClient.getChannelById(Snowflake.of(CONFIG.discord.channelId));
        if (CONFIG.discord.chatRelay.enable)
            relayRestChannel = restClient.getChannelById(Snowflake.of(CONFIG.discord.chatRelay.channelId));
        client.getEventDispatcher().on(MessageCreateEvent.class).subscribe(this::handleDiscordMessageCreateEvent);
        if (LAUNCH_CONFIG.release_channel.endsWith(".pre")) {
            sendEmbedMessage(Embed.builder()
                                 .title("ZenithProxy Prerelease")
                                 .description(
                                     """
                                     You are currently using a ZenithProxy prerelease
                                     
                                     Prereleases include experiments that may contain bugs and are not always updated with fixes
                                     
                                     Switch to a stable release with the `channel` command
                                     """));
        }
        if (CONFIG.deprecationWarning_1_20_4) {
            sendEmbedMessage(
                Embed.builder()
                    .title("1.20.4 Deprecated")
                    .description(
                        """
                        **ZenithProxy for 1.20.4 has been deprecated and will no longer receive updates or support.**
                        
                        Update to 1.21: `channel set <java/linux> 1.21.0`
                        
                        To disable this warning: `debug deprecationWarning off`
                        """)
            );
        }
    }

    private GatewayDiscordClient buildGatewayClient() {
        DiscordClient discordClient = buildProxiedClient(DiscordClientBuilder.create(CONFIG.discord.token))
            .setRequestQueueFactory(RequestQueueFactory.createFromSink(
                spec -> spec.multicast().onBackpressureBuffer(Queues.SMALL_BUFFER_SIZE, false),
                EmissionStrategy.timeoutDrop(Duration.ofSeconds(3))))
            .build();
        try {
            return discordClient.gateway()
                .setStore(Store.noOp())
                .setGatewayReactorResources(reactorResources -> GatewayReactorResources.builder(discordClient.getCoreResources().getReactorResources()).build())
                .setEnabledIntents((IntentSet.of(Intent.MESSAGE_CONTENT, Intent.GUILD_MESSAGES)))
                .setInitialPresence(shardInfo -> disconnectedPresence)
                .login()
                .block(Duration.ofSeconds(20));
        } catch (final CloseException e) {
            if (e.getReason().map(r -> r.contains("Disallowed intent")).orElse(false)) {
                DISCORD_LOG.error("Enable Message Content intent on the Discord developer's website. For help see: https://github.com/rfresh2/ZenithProxy/?tab=readme-ov-file#discord-bot-setup");
            } else {
                DISCORD_LOG.error("Failed logging into discord: {}", e.getReason().orElse(e.getMessage()));
            }
            throw e;
        } catch (final ClientException e) {
            if (e.getStatus().code() == 401) {
                DISCORD_LOG.error("Invalid or incorrect discord token");
            }
            throw e;
        }
    }

    private void handleDiscordMessageCreateEvent(final MessageCreateEvent event) {
        if (CONFIG.discord.chatRelay.enable
            && !CONFIG.discord.chatRelay.channelId.isEmpty()
            && event.getMessage().getChannelId().equals(Snowflake.of(CONFIG.discord.chatRelay.channelId))
            && !event.getMember().get().getId().equals(this.client.getSelfId())) {
            EVENT_BUS.postAsync(new DiscordMessageSentEvent(sanitizeRelayInputMessage(event.getMessage().getContent()), event));
            return;
        }
        if (!event.getMessage().getChannelId().equals(Snowflake.of(CONFIG.discord.channelId))) return;
        final String message = event.getMessage().getContent();
        if (!message.startsWith(CONFIG.discord.prefix)) return;
        try {
            final String inputMessage = message.substring(1);
            DISCORD_LOG.info(event.getMember().map(User::getTag).orElse("unknown user") + " (" + event.getMember().get().getId().asString() +") executed discord command: {}", inputMessage);
            final CommandContext context = DiscordCommandContext.create(inputMessage, event, mainRestChannel);
            COMMAND.execute(context);
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
                this.mainRestChannel.createMessage(message).retry(1).block(BLOCK_TIMEOUT);
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
                this.relayRestChannel.createMessage(message).retry(1).block(BLOCK_TIMEOUT);
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
                if (autoUpdater.getUpdateAvailable()
                    && ThreadLocalRandom.current().nextDouble() < 0.25
                ) {
                    this.client.updatePresence(getUpdateAvailablePresence(autoUpdater))
                        .block(BLOCK_TIMEOUT);
                    return;
                }
            }
            if (MODULE.get(AutoReconnect.class).autoReconnectIsInProgress()) {
                this.client.updatePresence(autoReconnectingPresence)
                    .block(BLOCK_TIMEOUT);
                return;
            }
            if (Proxy.getInstance().isInQueue())
                this.client.updatePresence(getQueuePresence()).block(BLOCK_TIMEOUT);
            else if (Proxy.getInstance().isConnected())
                this.client.updatePresence(getOnlinePresence()).block(BLOCK_TIMEOUT);
            else
                this.client.updatePresence(disconnectedPresence).block(BLOCK_TIMEOUT);
        } catch (final IllegalStateException e) {
            if (e.getMessage().contains("Backpressure overflow")) {
                DISCORD_LOG.error("Caught backpressure overflow, restarting discord session");
                EXECUTOR.execute(() -> {
                    this.stop(false);
                    this.start();
                });
            } else throw e;
        } catch (final Throwable e) {
            DISCORD_LOG.error("Failed updating discord presence. Check that the bot has correct permissions.");
            DISCORD_LOG.debug("Failed updating discord presence. Check that the bot has correct permissions.", e);
        }
    }

    private ClientPresence getUpdateAvailablePresence(final AutoUpdater autoUpdater) {
        return ClientPresence.of(Status.ONLINE, ClientActivity.custom(
            "Update Available" + autoUpdater.getNewVersion().map(v -> ": " + v).orElse("")));
    }

    private ClientPresence getOnlinePresence() {
        return ClientPresence.of(Status.ONLINE, ClientActivity.custom(
      (Proxy.getInstance().isOn2b2t() ? "2b2t" : CONFIG.client.server.address) + " [" + Proxy.getInstance().getOnlineTimeString() + "]"));
    }

    public void setBotNickname(final String nick) {
        try {
            final Id guildId = mainRestChannel.getData().block(BLOCK_TIMEOUT).guildId().get();
            this.client.getGuildById(Snowflake.of(guildId))
                .flatMap(g -> g.changeSelfNickname(nick))
                .block(BLOCK_TIMEOUT);
        } catch (final Exception e) {
            DISCORD_LOG.warn("Failed updating bot's nickname. Check that the bot has correct permissions");
            DISCORD_LOG.debug("Failed updating bot's nickname. Check that the bot has correct permissions", e);
        }
    }

    public void setBotDescription(String description) {
        try {
            restClient.getApplicationService()
                .modifyCurrentApplicationInfo(ApplicationEditSpec.builder()
                                                  .description(description)
                                                  .build()
                                                  .asRequest())
                .block(BLOCK_TIMEOUT);
        } catch (final Exception e) {
            DISCORD_LOG.warn("Failed updating bot's description. Check that the bot has correct permissions");
            DISCORD_LOG.debug("Failed updating bot's description", e);
        }
    }

    private void handleProxyUpdateComplete() {
        CONFIG.discord.isUpdating = false;
        saveConfigAsync();
        sendEmbedMessage(Embed.builder()
                             .title("Update complete!")
                             .description("Current Version: `" + escape(LAUNCH_CONFIG.version) + "`")
                             .successColor());
    }

    void sendQueueWarning() {
        sendEmbedMessage((CONFIG.discord.queueWarning.mentionRole ? mentionAccountOwner() : ""), Embed.builder()
            .title("Queue Warning")
            .addField("Queue Position", "[" + queuePositionStr() + "]", false)
            .inQueueColor());
    }

    static String mentionAccountOwner() {
        return DiscordBot.mentionRole(CONFIG.discord.accountOwnerRoleId);
    }

    static String mentionRole(final String roleId) {
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

    Embed getUpdateMessage(final Optional<String> newVersion) {
        String verString = "Current Version: `" + escape(LAUNCH_CONFIG.version) + "`";
        if (newVersion.isPresent()) verString += "\nNew Version: `" + escape(newVersion.get()) + "`";
        var embed = Embed.builder()
            .title("Updating and restarting...")
            .description(verString)
            .primaryColor();
        if (!LAUNCH_CONFIG.auto_update) {
            embed.addField("Info", "`autoUpdate` must be enabled for new updates to apply", false);
        }
        return embed;
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
                .block(BLOCK_TIMEOUT);
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

    public void sendEmbedMessage(String message, Embed embed) {
        mainChannelMessageQueue.add(MessageCreateSpec.builder()
                                        .content(message)
                                        .addEmbed(embed.toSpec())
                                        .build().asRequest());
        TERMINAL_LOG.info(message);
        CommandOutputHelper.logEmbedOutputToTerminal(embed);
    }

    public void sendRelayEmbedMessage(String message, Embed embed) {
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

    void sendEmbedMessageWithButtons(String message, Embed embed, List<Button> buttons, Function<ButtonInteractionEvent, Publisher<Mono<?>>> mapper, Duration timeout) {
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

    void sendEmbedMessageWithButtons(Embed embed, List<Button> buttons, Function<ButtonInteractionEvent, Publisher<Mono<?>>> mapper, Duration timeout) {
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

    ClientPresence getQueuePresence() {
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
        builder.setReactorResources(new ReactorResources(
            httpClient,
            DEFAULT_TIMER_TASK_SCHEDULER.get(),
            DEFAULT_BLOCKING_TASK_SCHEDULER.get())
        );
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

    String extractRelayEmbedSenderUsername(final Possible<Integer> color, final String msgContent) {
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

    public void updatePresence(final ClientPresence presence) {
        this.client.updatePresence(presence).block(BLOCK_TIMEOUT);
    }
}
