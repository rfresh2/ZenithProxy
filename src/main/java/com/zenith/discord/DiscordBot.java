package com.zenith.discord;

import com.github.rfresh2.SimpleEventBus;
import com.zenith.Proxy;
import com.zenith.command.api.CommandContext;
import com.zenith.command.api.CommandOutputHelper;
import com.zenith.command.api.DiscordCommandContext;
import com.zenith.event.message.DiscordMainChannelCommandReceivedEvent;
import com.zenith.event.message.DiscordRelayChannelMessageReceivedEvent;
import com.zenith.feature.autoupdater.AutoUpdater;
import com.zenith.feature.queue.Queue;
import com.zenith.module.impl.AutoReconnect;
import com.zenith.util.MentionUtil;
import lombok.Getter;
import lombok.experimental.Accessors;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.components.ActionComponent;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.ISnowflake;
import net.dv8tion.jda.api.entities.Icon;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.StatusChangeEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.session.*;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.exceptions.PermissionException;
import net.dv8tion.jda.api.hooks.SimpleEventBusListener;
import net.dv8tion.jda.api.requests.ErrorResponse;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.api.utils.ShutdownException;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.internal.utils.ShutdownReason;
import org.jspecify.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.github.rfresh2.EventConsumer.of;
import static com.zenith.Globals.*;
import static java.util.Arrays.asList;

@Accessors(fluent = true)
public class DiscordBot {
    private TextChannel mainChannel; // Null if not running
    private TextChannel relayChannel; // Null if not running or relay disabled
    private ScheduledFuture<?> presenceUpdateFuture;
    @Getter private JDA jda; // Null if not running
    @Getter private final SimpleEventBus jdaEventBus = new SimpleEventBus();
    public Optional<Instant> lastRelayMessage = Optional.empty();

    public DiscordBot() {
        jdaEventBus.subscribe(
            this,
            of(MessageReceivedEvent.class, this::onMessageReceived),
            of(SessionRecreateEvent.class, e -> DISCORD_LOG.info("Session recreated")),
            of(SessionResumeEvent.class, e -> DISCORD_LOG.info("Session resumed")),
            of(ReadyEvent.class, e -> DISCORD_LOG.info("JDA ready")),
            of(SessionDisconnectEvent.class, e -> DISCORD_LOG.info("Session disconnected")),
            of(SessionInvalidateEvent.class, e -> DISCORD_LOG.info("Session invalidated")),
            of(StatusChangeEvent.class, e -> DISCORD_LOG.debug("JDA Status: {}", e.getNewStatus()))
        );
        EVENT_BUS.subscribe(
            this,
            of(DiscordMainChannelCommandReceivedEvent.class, this::executeDiscordCommand)
        );
    }

    public synchronized void start() {
        if (isRunning()) return;
        try {
            initializeJda();
        } catch (Throwable e) {
            if (this.jda != null) {
                jda.shutdownNow();
            }
            this.mainChannel = null;
            this.relayChannel = null;
            jda = null;
            throw e;
        }

        if (CONFIG.discord.isUpdating) {
            handleProxyUpdateComplete();
        }
        this.presenceUpdateFuture = EXECUTOR.scheduleWithFixedDelay(
            this::tickPresence, 0L,
            15L, // discord rate limit
            TimeUnit.SECONDS);
    }

    public synchronized void stop(boolean clearQueue) {
        if (!isRunning()) return;
        if (presenceUpdateFuture != null) presenceUpdateFuture.cancel(true);
        try {
            if (clearQueue) {
                jda.shutdownNow();
            } else {
                jda.shutdown();
            }
            jda.awaitShutdown(Duration.ofSeconds(20));
        } catch (final Exception e) {
            DISCORD_LOG.warn("Exception during JDA shutdown", e);
        }
    }

    public boolean isRunning() {
        var status = getJdaStatus();
        return status != JDA.Status.SHUTDOWN && status != JDA.Status.FAILED_TO_LOGIN;
    }

    public JDA.Status getJdaStatus() {
        var jda = this.jda;
        if (jda == null) return JDA.Status.SHUTDOWN;
        return jda.getStatus();
    }

    private void initializeJda() {
        if (CONFIG.discord.channelId.isEmpty()) throw new RuntimeException("Discord bot is enabled but channel id is not set");
        if (CONFIG.discord.chatRelay.enable) {
            if (CONFIG.discord.chatRelay.channelId.isEmpty()) throw new RuntimeException("Discord chat relay is enabled and channel id is not set");
            if (CONFIG.discord.channelId.equals(CONFIG.discord.chatRelay.channelId)) throw new RuntimeException("Discord channel id and chat relay channel id cannot be the same");
        }
        if (CONFIG.discord.accountOwnerRoleId.isEmpty()) throw new RuntimeException("Discord account owner role id is not set");
        try {
            Long.parseUnsignedLong(CONFIG.discord.accountOwnerRoleId);
        } catch (final Exception e) {
            throw new RuntimeException("Invalid account owner role ID set: " + CONFIG.discord.accountOwnerRoleId);
        }

        JDABuilder builder = JDABuilder.createLight(
                CONFIG.discord.token,
                asList(GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_MESSAGES))
            .setActivity(Activity.customStatus("Disconnected"))
            .setStatus(OnlineStatus.DO_NOT_DISTURB)
            .addEventListeners(new SimpleEventBusListener(jdaEventBus));
        this.jda = builder.build();
        try {
            jda.awaitReady();
        } catch (ShutdownException e) {
            if (e.getShutdownReason() == ShutdownReason.DISALLOWED_INTENTS) {
                throw new RuntimeException("You must enable MESSAGE CONTENT INTENT on the Discord developer website: https://wiki.2b2t.vc/_assets/img/discord-setup/DiscordSetup2.png");
            }
            throw e;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        this.mainChannel = Objects.requireNonNull(
            jda.getChannelById(TextChannel.class, CONFIG.discord.channelId),
            "Discord channel not found with ID: " + CONFIG.discord.channelId);
        if (CONFIG.discord.chatRelay.enable) {
            this.relayChannel = Objects.requireNonNull(
                jda.getChannelById(TextChannel.class, CONFIG.discord.chatRelay.channelId),
                "Discord relay channel not found with ID: " + CONFIG.discord.chatRelay.channelId);
        }
    }

    private void onMessageReceived(MessageReceivedEvent event) {
        var member = event.getMember();
        if (member == null) return;
        if (member.getUser().isBot() && CONFIG.discord.ignoreOtherBots) return;
        if (member.getId().equals(jda.getSelfUser().getId())) return;
        if (CONFIG.discord.chatRelay.enable
            && !CONFIG.discord.chatRelay.channelId.isEmpty()
            && event.getMessage().getChannelId().equals(CONFIG.discord.chatRelay.channelId)
            && !member.getId().equals(jda.getSelfUser().getId())) {
            EVENT_BUS.postAsync(new DiscordRelayChannelMessageReceivedEvent(event));
            return;
        }
        if (!event.getMessage().getChannelId().equals(CONFIG.discord.channelId)) return;
        final String message = event.getMessage().getContentRaw();
        if (!message.startsWith(CONFIG.discord.prefix)) return;
        EVENT_BUS.postAsync(new DiscordMainChannelCommandReceivedEvent(event));
    }

    private void executeDiscordCommand(final DiscordMainChannelCommandReceivedEvent event) {
        try {
            var jdaEvent = event.event();
            var member = event.member();
            var inputMessage = event.message().substring(CONFIG.discord.prefix.length());
            var memberName = member.getUser().getName();
            var memberId = member.getId();
            DISCORD_LOG.info("{} ({}) executed discord command: {}", memberName, memberId, inputMessage);
            final CommandContext context = DiscordCommandContext.create(inputMessage, jdaEvent);
            COMMAND.execute(context);
            final MessageCreateData request = commandEmbedOutputToMessage(context);
            if (request != null) {
                mainChannel.sendMessage(request).queue();
                CommandOutputHelper.logEmbedOutputToTerminal(context.getEmbed());
            }
            if (!context.getMultiLineOutput().isEmpty()) {
                for (final String line : context.getMultiLineOutput()) {
                    mainChannel.sendMessage(line).queue();
                }
                CommandOutputHelper.logMultiLineOutputToTerminal(context.getMultiLineOutput());
            }
        } catch (final Exception e) {
            DISCORD_LOG.error("Failed processing discord command: {}", event.message(), e);
        }
    }

    private MessageCreateData commandEmbedOutputToMessage(final CommandContext context) {
        var embed = context.getEmbed();
        if (embed.title() == null) return null;
        var msgBuilder = new MessageCreateBuilder()
            .addEmbeds(embed.toJDAEmbed());
        if (embed.fileAttachment() != null) {
            msgBuilder.addFiles(FileUpload.fromData(new ByteArrayInputStream(embed.fileAttachment.data()), embed.fileAttachment.name()));
        }
        return msgBuilder
            .build();
    }

    public static String mentionAccountOwner() {
        return mentionRole(CONFIG.discord.accountOwnerRoleId);
    }

    public static String mentionRole(final String roleId) {
        try {
            return MentionUtil.forRole(roleId);
        } catch (final NumberFormatException e) {
            DISCORD_LOG.error("Unable to generate mention for role ID: {}", roleId, e);
            return "";
        }
    }

    public void setBotNickname(final String nick) {
        if (!isRunning()) return;
        try {
            if (nick.equals(mainChannel.getGuild().getSelfMember().getNickname())) return;
            mainChannel.getGuild().getSelfMember().modifyNickname(nick).complete();
        } catch (PermissionException e) {
            DISCORD_LOG.warn("Failed updating bot's nickname. Check that the bot has correct permissions: {}", e.getMessage());
            DISCORD_LOG.debug("Failed updating bot's nickname. Check that the bot has correct permissions", e);
        } catch (final Exception e) {
            DISCORD_LOG.warn("Failed updating bot's nickname: {}", e.getMessage());
            DISCORD_LOG.debug("Failed updating bot's nickname", e);
        }
    }

    public void setBotDescription(String description) {
        if (!isRunning()) return;
        try {
            jda.getApplicationManager().setDescription(description).complete();
        } catch (final Exception e) {
            DISCORD_LOG.warn("Failed updating bot's description: {}", e.getMessage());
            DISCORD_LOG.debug("Failed updating bot's description", e);
        }
    }

    private void handleProxyUpdateComplete() {
        CONFIG.discord.isUpdating = false;
        saveConfigAsync();
        var embed = Embed.builder()
            .title("Update complete!")
            .description("Current Version: `" + escape(LAUNCH_CONFIG.version) + "`")
            .successColor();
        if (!LAUNCH_CONFIG.auto_update) {
            embed
                .title("Restart complete!");
        }
        sendEmbedMessage(embed);
    }

    public static String escape(String message) {
        return message.replaceAll("_", "\\\\_");
    }

    void tickPresence() {
        if (!isRunning()) return;
        if (!CONFIG.discord.managePresence) return;
        try {
            if (LAUNCH_CONFIG.auto_update) {
                final AutoUpdater autoUpdater = Proxy.getInstance().getAutoUpdater();
                if (autoUpdater.getUpdateAvailable() && ThreadLocalRandom.current().nextDouble() < 0.25) {
                    jda.getPresence().setPresence(
                        Proxy.getInstance().isConnected()
                            ? Proxy.getInstance().isInQueue() || MODULE.get(AutoReconnect.class).autoReconnectIsInProgress()
                                ? OnlineStatus.IDLE
                                : OnlineStatus.ONLINE
                            : OnlineStatus.DO_NOT_DISTURB,
                        Activity.customStatus("Update Available" + autoUpdater.getNewVersion().map(v -> ": " + v).orElse(""))
                    );
                }
            }
            if (MODULE.get(AutoReconnect.class).autoReconnectIsInProgress()) {
                jda.getPresence().setPresence(OnlineStatus.IDLE, Activity.customStatus("AutoReconnecting..."));
                return;
            }
            if (Proxy.getInstance().isInQueue()) {
                jda.getPresence().setPresence(OnlineStatus.IDLE, Activity.customStatus(Queue.queuePositionStr()));
            } else if (Proxy.getInstance().isConnected()) {
                jda.getPresence().setPresence(
                    OnlineStatus.ONLINE,
                    Activity.customStatus((Proxy.getInstance().isOn2b2t() ? "2b2t" : CONFIG.client.server.address)
                                              + " [" + Proxy.getInstance().getOnlineTimeString() + "]"));
            } else {
                jda.getPresence().setPresence(OnlineStatus.DO_NOT_DISTURB, Activity.customStatus("Disconnected"));
            }
        } catch (final Throwable e) {
            DISCORD_LOG.error("Failed updating discord presence. Check that the bot has correct permissions: {}", e.getMessage());
            DISCORD_LOG.debug("Failed updating discord presence. Check that the bot has correct permissions.", e);
        }
    }

    public void setPresence(final OnlineStatus onlineStatus, final Activity activity) {
        if (!isRunning()) return;
        jda.getPresence().setPresence(onlineStatus, activity);
    }

    public static boolean validateButtonInteractionEventFromAccountOwner(final ButtonInteractionEvent event) {
        return Optional.ofNullable(event.getInteraction().getMember())
            .map(m -> m.getRoles().stream()
                .map(ISnowflake::getId)
                .anyMatch(roleId -> roleId.equals(CONFIG.discord.accountOwnerRoleId)))
            .orElse(false);
    }

    public void updateBotNickname() {
        if (CONFIG.discord.manageNickname)
            DISCORD.setBotNickname(CONFIG.authentication.username + " | ZenithProxy");
    }

    public void updateBotInfo() {
        updateBotNickname();
        if (CONFIG.discord.manageDescription)
            DISCORD.setBotDescription(
                """
                ZenithProxy %s
                **Official Discord**:
                  https://discord.gg/nJZrSaRKtb
                **Github**:
                  https://github.com/rfresh2/ZenithProxy
                """.formatted(VERSION));
    }

    public void updateBotAvatar() {
        if (CONFIG.discord.manageProfileImage)
            DISCORD.setProfileImage(Proxy.getInstance().getServerIcon());
    }

    public void setProfileImage(final byte[] imageBytes) {
        if (!isRunning()) return;
        try {
            jda.getSelfUser().getManager().setAvatar(Icon.from(imageBytes)).complete();
        } catch (ErrorResponseException e) {
            if (e.getErrorResponse() == ErrorResponse.INVALID_FORM_BODY) {
                DISCORD_LOG.debug("Rate limited while updating discord profile image.", e);
                return;
            }
            DISCORD_LOG.warn("Failed updating discord profile image: {}", e.getMessage());
            DISCORD_LOG.debug("Failed updating discord profile image", e);
        } catch (final Exception e) {
            DISCORD_LOG.warn("Failed updating discord profile image: {}", e.getMessage());
            DISCORD_LOG.debug("Failed updating discord profile image", e);
        }
    }

    public void defaultEmbedDecoration(Embed embed) {
        if (embed.timestamp() == null) embed.timestamp(Instant.now());
    }

    public void sendEmbedMessageTo(TextChannel channel, @Nullable String message, Embed embed) {
        defaultEmbedDecoration(embed);
        if (isRunning()) {
            try {
                var msgBuilder = new MessageCreateBuilder()
                    .setContent(message)
                    .addEmbeds(embed.toJDAEmbed());
                if (embed.fileAttachment() != null) {
                    msgBuilder.addFiles(FileUpload.fromData(new ByteArrayInputStream(embed.fileAttachment.data()), embed.fileAttachment.name()));
                }
                channel.sendMessage(msgBuilder.build()).queue();
            } catch (final Exception e) {
                DISCORD_LOG.error("Failed sending embed message", e);
            }
        }
        if (message != null) TERMINAL_LOG.info(message);
    }

    public void sendEmbedMessage(Embed embed) {
        sendEmbedMessage(null, embed);
    }

    public void sendEmbedMessage(@Nullable String message, Embed embed) {
        sendEmbedMessageTo(mainChannel, message, embed);
        CommandOutputHelper.logEmbedOutputToTerminal(embed);
    }

    public void sendRelayEmbedMessage(Embed embed) {
        sendRelayEmbedMessage(null, embed);
    }

    public void sendRelayEmbedMessage(@Nullable String message, Embed embed) {
        if (!CONFIG.discord.chatRelay.enable) return;
        sendEmbedMessageTo(relayChannel, message, embed);
    }

    public void sendMessageTo(TextChannel channel, String message) {
        if (isRunning()) {
            try {
                channel.sendMessage(
                    new MessageCreateBuilder()
                        .setContent(message)
                        .build())
                    .queue();
            } catch (Exception e) {
                DISCORD_LOG.error("Failed sending message: {}", e.getMessage());
            }
        }
    }

    public void sendMessage(final String message) {
        sendMessageTo(mainChannel, message);
        TERMINAL_LOG.info(message);
    }

    public void sendRelayMessage(final String message) {
        if (!CONFIG.discord.chatRelay.enable) return;
        sendMessageTo(relayChannel, message);
    }

    public void sendEmbedMessageWithButtonsTo(TextChannel channel, @Nullable String message, Embed embed, List<Button> buttons, Consumer<ButtonInteractionEvent> eventConsumer, Duration timeout) {
        defaultEmbedDecoration(embed);
        if (isRunning()) {
            try {
                channel.sendMessage(
                        new MessageCreateBuilder()
                            .setEmbeds(embed.toJDAEmbed())
                            .setContent(message)
                            .addComponents(ActionRow.of(buttons))
                            .build())
                    .queue();
                var buttonIds = buttons.stream().map(ActionComponent::getCustomId).collect(Collectors.toSet());
                jda.listenOnce(ButtonInteractionEvent.class)
                    .filter(e -> buttonIds.contains(e.getComponentId()))
                    .timeout(timeout)
                    .subscribe(eventConsumer);
            } catch (final Exception e) {
                DISCORD_LOG.error("Failed sending embed message with buttons to discord", e);
            }
        }
    }

    public void sendEmbedMessageWithButtons(@Nullable String message, Embed embed, List<Button> buttons, Consumer<ButtonInteractionEvent> eventConsumer, Duration timeout) {
        sendEmbedMessageWithButtonsTo(mainChannel, message, embed, buttons, eventConsumer, timeout);
        TERMINAL_LOG.info(message);
        CommandOutputHelper.logEmbedOutputToTerminal(embed);
    }

    public void sendEmbedMessageWithButtons(Embed embed, List<Button> buttons, Consumer<ButtonInteractionEvent> mapper, Duration timeout) {
        sendEmbedMessageWithButtons(null, embed, buttons, mapper, timeout);
    }
}
