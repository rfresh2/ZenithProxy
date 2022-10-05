package com.zenith.discord;

import com.collarmc.pounce.Subscribe;
import com.github.steveice10.mc.protocol.packet.ingame.client.ClientChatPacket;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.zenith.Proxy;
import com.zenith.discord.command.*;
import com.zenith.event.proxy.*;
import com.zenith.util.Queue;
import discord4j.common.util.Snowflake;
import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
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

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.zenith.discord.command.StatusCommand.getCoordinates;
import static com.zenith.util.Constants.*;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

public class DiscordBot {

    private RestClient restClient;
    private Supplier<RestChannel> mainRestChannel = Suppliers.memoize(() -> restClient.getChannelById(Snowflake.of(CONFIG.discord.channelId)));
    private Supplier<RestChannel> relayRestChannel = Suppliers.memoize(() -> restClient.getChannelById(Snowflake.of(CONFIG.discord.chatRelay.channelId)));
    private GatewayDiscordClient client;
    private Proxy proxy;
    // Main channel discord message FIFO queue
    private final ConcurrentLinkedQueue<MultipartRequest<MessageCreateRequest>> mainChannelMessageQueue;
    private static final ClientPresence DISCONNECTED_PRESENCE = ClientPresence.of(Status.DO_NOT_DISTURB, ClientActivity.playing("Disconnected"));
    private static final ClientPresence DEFAULT_CONNECTED_PRESENCE = ClientPresence.of(Status.ONLINE, ClientActivity.playing(CONFIG.client.server.address));
    private final ScheduledExecutorService scheduledExecutorService;
    private static HashMap<String, Long> repliedPlayers = new HashMap<String, Long>();
    public List<Command> commands;

    public DiscordBot() {
        this.scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        this.mainChannelMessageQueue = new ConcurrentLinkedQueue<>();
        this.commands = new ArrayList<>();
    }

    public void start(Proxy proxy) {
        this.proxy = proxy;
        this.client = DiscordClient.create(CONFIG.discord.token)
                .gateway()
                .setInitialPresence(shardInfo -> DISCONNECTED_PRESENCE)
                .login()
                .block();
        EVENT_BUS.subscribe(this);

        restClient = client.getRestClient();

        commands.add(new ConnectCommand(this.proxy));
        commands.add(new DisconnectCommand(this.proxy));
        commands.add(new StatusCommand(this.proxy));
        commands.add(new HelpCommand(this.proxy, this.commands));
        commands.add(new WhitelistCommand(this.proxy));
        commands.add(new AutoDisconnectCommand(this.proxy));
        commands.add(new AutoReconnectCommand(this.proxy));
        commands.add(new AutoRespawnCommand(this.proxy));
        commands.add(new ServerCommand(this.proxy));
        commands.add(new AntiAFKCommand(this.proxy));
        commands.add(new VisualRangeCommand(this.proxy));
        commands.add(new UpdateCommand(this.proxy));
        commands.add(new ProxyClientConnectionCommand(this.proxy));
        commands.add(new ActiveHoursCommand(this.proxy));
        commands.add(new DisplayCoordsCommand(this.proxy));
        commands.add(new ChatRelayCommand(this.proxy));
        commands.add(new ReconnectCommand(this.proxy));
        commands.add(new AutoUpdateCommand(this.proxy));
        commands.add(new StalkCommand(this.proxy));
        commands.add(new TablistCommand(this.proxy));
        commands.add(new SpectatorCommand(this.proxy));
        commands.add(new PrioCommand(this.proxy));
        commands.add(new AutoReplyCommand(this.proxy));

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
            commands.stream()
                    .filter(command -> message.toLowerCase(Locale.ROOT).startsWith(CONFIG.discord.prefix + command.getName().toLowerCase(Locale.ROOT)))
                    .findFirst()
                    .ifPresent(command -> {
                        try {
                            MultipartRequest<MessageCreateRequest> m = command.execute(event, mainRestChannel.get());
                            if (m != null) {
                                mainChannelMessageQueue.add(m);
                            }
                        } catch (final Exception e) {
                            DISCORD_LOG.error("Error executing discord command: " + command, e);
                        }
                    });
        });

        if (CONFIG.discord.isUpdating) {
            handleProxyUpdateComplete();
        }
        scheduledExecutorService.scheduleAtFixedRate(this::updatePresence, 0L,
                15L, // discord rate limit
                TimeUnit.SECONDS);
        scheduledExecutorService.scheduleAtFixedRate(this::processMessageQueue, 0L, 100L, TimeUnit.MILLISECONDS);
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

    private void updatePresence() {
        if (this.proxy.isInQueue()) {
            this.client.updatePresence(getQueuePresence()).subscribe();
        } else if (this.proxy.isConnected()) {
            this.client.updatePresence(getOnlinePresence()).subscribe();
        } else {
            this.client.updatePresence(DISCONNECTED_PRESENCE).subscribe();
        }
    }

    private ClientPresence getOnlinePresence() {
        long onlineSeconds = Instant.now().getEpochSecond() - this.proxy.getConnectTime().getEpochSecond();
        return ClientPresence.of(Status.ONLINE, ClientActivity.playing(CONFIG.client.server.address + " [" + Queue.getEtaStringFromSeconds(onlineSeconds) + "]"));
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
        this.client.updatePresence(DEFAULT_CONNECTED_PRESENCE).subscribe();
        sendEmbedMessage(EmbedCreateSpec.builder()
                .title("Proxy Connected")
               .color(Color.CYAN)
               .addField("Server", CONFIG.client.server.address, true)
               .addField("Regular Queue", ""+Queue.getQueueStatus().regular, true)
               .addField("Priority Queue", ""+Queue.getQueueStatus().prio, true)
               .addField("Proxy IP", CONFIG.server.getProxyAddress(), false)
               .build());
    }

    @Subscribe
    public void handlePlayerOnlineEvent(PlayerOnlineEvent event) {
        sendEmbedMessage(EmbedCreateSpec.builder()
                .title("Proxy Online")
                .color(Color.CYAN)
                .addField("Server", CONFIG.client.server.address, true)
                .build());
    }

    @Subscribe
    public void handleDisconnectEvent(DisconnectEvent event) {
        this.client.updatePresence(DISCONNECTED_PRESENCE).subscribe();
        sendEmbedMessage(EmbedCreateSpec.builder()
                .title("Proxy Disconnected")
                .addField("Reason", event.reason, true)
                .color(Color.CYAN)
                .build());
    }

    @Subscribe
    public void handleQueuePositionUpdateEvent(QueuePositionUpdateEvent event) {
        this.client.updatePresence(getQueuePresence()).subscribe();
        if (event.position == CONFIG.server.queueWarning) {
            sendQueueWarning();
        } else if (event.position <= 3) {
            sendQueueWarning();
        }
    }

    private void sendQueueWarning() {
        sendEmbedMessage(EmbedCreateSpec.builder()
                .title("Proxy Queue Warning")
                .color(this.proxy.isConnected() ? Color.CYAN : Color.RUBY)
                .addField("Server", CONFIG.client.server.address, true)
                .addField("Queue Position", "[" + queuePositionStr() + "]", false)
                .build());

    }

    private String queuePositionStr() {
        if (proxy.getIsPrio().isPresent()) {
            if (proxy.getIsPrio().get()) {
                return this.proxy.getQueuePosition() + " / " + Queue.getQueueStatus().prio + " - ETA: " + Queue.getQueueEta(Queue.getQueueStatus().prio, this.proxy.getQueuePosition());
            } else {
                return this.proxy.getQueuePosition() + " / " + Queue.getQueueStatus().regular + " - ETA: " + Queue.getQueueEta(Queue.getQueueStatus().regular, this.proxy.getQueuePosition());
            }
        } else {
            return "?";
        }
    }

    @Subscribe
    public void handleQueueCompleteEvent(QueueCompleteEvent event) {
        this.client.updatePresence(DEFAULT_CONNECTED_PRESENCE).subscribe();
    }

    @Subscribe
    public void handleStartQueueEvent(StartQueueEvent event) {
        this.client.updatePresence(getQueuePresence()).subscribe();
        sendEmbedMessage(EmbedCreateSpec.builder()
                .title("Proxy Started Queuing")
                .color(Color.CYAN)
                .addField("Regular Queue", ""+Queue.getQueueStatus().regular, true)
                .addField("Priority Queue", ""+Queue.getQueueStatus().prio, true)
                .build());
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
    public void handleDeathMessageEvent(DeathMessageEvent event) {
        sendEmbedMessage(EmbedCreateSpec.builder()
                .title("Death Message")
                .color(Color.RUBY)
                .addField("Message", event.message, false)
                .build());
    }

    @Subscribe
    public void handleNewPlayerInVisualRangeEvent(NewPlayerInVisualRangeEvent event) {
        if (CONFIG.client.extra.visualRangeAlert) {
            EmbedCreateSpec.Builder embedCreateSpec = EmbedCreateSpec.builder()
                    .title("Player In Visual Range")
                    .addField("Player Name", Optional.ofNullable(event.playerEntry.getName()).orElse("Unknown"), true)
                    .addField("Player UUID", event.playerEntry.getId().toString(), true)
                    .image(this.proxy.getAvatarURL(event.playerEntry.getId()).toString());

            if (CONFIG.discord.reportCoords) {
                embedCreateSpec.addField("Coordinates", "||["
                        + (int) event.playerEntity.getX() + ", "
                        + (int) event.playerEntity.getY() + ", "
                        + (int) event.playerEntity.getZ()
                        + "]||", false);
            }
            if (CONFIG.client.extra.visualRangeAlertMention) {
                boolean notFriend = CONFIG.client.extra.friendList.stream()
                        .noneMatch(friend -> friend.equalsIgnoreCase(Optional.ofNullable(event.playerEntry.getName()).orElse("Unknown")));
                if (notFriend) {
                    if (CONFIG.discord.visualRangeMentionRoleId.length() > 3) {
                        sendEmbedMessage("<@&" + CONFIG.discord.visualRangeMentionRoleId + ">", embedCreateSpec.build());
                    } else {
                        sendEmbedMessage("<@&" + CONFIG.discord.accountOwnerRoleId + ">", embedCreateSpec.build());
                    }
                } else {
                    sendEmbedMessage(embedCreateSpec.build());
                }
            } else {
                sendEmbedMessage(embedCreateSpec.build());
            }
        }
    }

    @Subscribe
    public void handleAutoDisconnectEvent(AutoDisconnectEvent event) {
        sendEmbedMessage(EmbedCreateSpec.builder()
                .title("Proxy AutoDisconnect Triggered")
                .addField("Health", ""+((int)CACHE.getPlayerCache().getThePlayer().getHealth()), true)
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
                    .addField("Username", event.clientGameProfile.getName(), true)
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
                builder = builder.addField("Username", event.clientGameProfile.getName(), false);
            }
            if (nonNull(event.reason)) {
                builder = builder.addField("Reason" , event.reason, false);
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
                builder = builder.addField("Username", event.clientGameProfile.getName(), false);
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
                .addField("ETA", Queue.getQueueEta(queueLength, queueLength), false)
                .color(Color.CYAN)
                .build());
    }

    @Subscribe
    public void handleServerChatReceivedEvent(ServerChatReceivedEvent event) {
        if (CONFIG.client.extra.autoReply.enabled && isNull(this.proxy.getCurrentPlayer().get())) {
            try {
                if (!escape(event.message).startsWith("<")) {
                    String[] split = escape(event.message).split(" ");
                    final String sender = split[0];
                    if (split.length > 2 && split[1].startsWith("whispers") && !sender.equalsIgnoreCase(CONFIG.authentication.username)) { // make sending chat relay messages pause autoreply for (x) minutes
                        repliedPlayers.entrySet().removeIf(entry -> entry.getValue() < Instant.now().getEpochSecond());
                        if (isNull(repliedPlayers.get(sender))) {
                            this.proxy.getClient().send(new ClientChatPacket("/w " + sender + " " + CONFIG.client.extra.autoReply.message)); // 236 char max ( 256 - 4(command) - 16(max name length)
                            repliedPlayers.put(sender, Instant.now().getEpochSecond() + CONFIG.client.extra.autoReply.cooldownSeconds);
                        }
                    }
                }
            } catch (final Throwable e) {
                CLIENT_LOG.error("", e);
            }
        }
        if (CONFIG.discord.chatRelay.enable && CONFIG.discord.chatRelay.channelId.length() > 0) {
            if (CONFIG.discord.chatRelay.ignoreQueue && this.proxy.isInQueue()) return;
            try {
                String message = escape(event.message);
                if (CONFIG.discord.chatRelay.mentionRoleOnWhisper || CONFIG.discord.chatRelay.mentionRoleOnNameMention) {
                    if (!message.startsWith("<")) {
                        if (CONFIG.discord.chatRelay.mentionRoleOnWhisper) {
                            String[] split = message.split(" ");
                            if (split.length > 2 && split[1].startsWith("whispers") && !message.toLowerCase(Locale.ROOT).contains("discord.gg/")) {
                                message = "<@&" + CONFIG.discord.accountOwnerRoleId + "> " + message;
                            }
                        }
                    } else {
                        if (CONFIG.discord.chatRelay.mentionRoleOnNameMention) {
                            if (message.split(" ", 2)[1].toLowerCase().contains(CONFIG.authentication.username.toLowerCase())) {
                                message = "<@&" + CONFIG.discord.accountOwnerRoleId + "> " + message;
                            }
                        }
                    }
                }
                relayRestChannel.get().createMessage(message).subscribe();
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
                relayRestChannel.get().createMessage(escape(event.playerEntry.getName() + " connected")).subscribe();
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
                        sendEmbedMessage("<@&" + CONFIG.discord.accountOwnerRoleId + ">",  EmbedCreateSpec.builder()
                                .title("Stalked Player Online!")
                                .addField("Player Name", event.playerEntry.getName(), true)
                                .image(this.proxy.getAvatarURL(event.playerEntry.getId()).toString())
                                .build());
                    });
        }
    }

    @Subscribe
    public void handleServerPlayerDisconnectedEvent(ServerPlayerDisconnectedEvent event) {
        if (CONFIG.discord.chatRelay.enable && CONFIG.discord.chatRelay.connectionMessages && CONFIG.discord.chatRelay.channelId.length() > 0) {
            if (CONFIG.discord.chatRelay.ignoreQueue && this.proxy.isInQueue()) return;
            try {
                relayRestChannel.get().createMessage(escape(event.playerEntry.getName()) + " disconnected").subscribe();
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
                        sendEmbedMessage("<@&" + CONFIG.discord.accountOwnerRoleId + ">",  EmbedCreateSpec.builder()
                                .title("Stalked Player Offline!")
                                .addField("Player Name", event.playerEntry.getName(), true)
                                .image(this.proxy.getAvatarURL(event.playerEntry.getId()).toString())
                                .build());
                    });
        }
    }

    @Subscribe
    public void handleDiscordMessageSentEvent(DiscordMessageSentEvent event) {
        if (CONFIG.discord.chatRelay.enable) {
            if (this.proxy.isConnected() && !event.message.isEmpty()) {
                this.proxy.getClient().send(new ClientChatPacket(event.message));
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
                .color(Color.CYAN)
                .addField("Message", event.message, true)
                .addField("Server", CONFIG.client.server.address, false)
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
                    .title("PRIO QUEUE STATUS DETECTED")
                    .color(Color.GREEN);
        } else {
            embedCreateSpec
                    .title("PRIO QUEUE STATUS LOST")
                    .color(Color.RED);
        }
        embedCreateSpec.addField("User", CONFIG.authentication.username, false);
        sendEmbedMessage((CONFIG.discord.mentionRoleOnPrioUpdate ? "<@&" + CONFIG.discord.accountOwnerRoleId + ">" : ""), embedCreateSpec.build());
    }

    @Subscribe
    public void handlePrioBanStatusUpdateEvent(PrioBanStatusUpdateEvent event) {
        EmbedCreateSpec.Builder embedCreateSpec = EmbedCreateSpec.builder();
        if (event.prioBanned) {
            embedCreateSpec
                    .title("PRIO BAN DETECTED")
                    .color(Color.RED);
        } else {
            embedCreateSpec
                    .title("PRIO UNBAN DETECTED")
                    .color(Color.GREEN);
        }
        embedCreateSpec.addField("User", CONFIG.authentication.username, false);
        sendEmbedMessage((CONFIG.discord.mentionRoleOnPrioBanUpdate ? "<@&" + CONFIG.discord.accountOwnerRoleId + ">" : ""), embedCreateSpec.build());
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
                    .subscribe();
        } catch (final Exception e) {
            DISCORD_LOG.error("Failed updating discord profile image", e);
        }
    }

    public void sendAutoReconnectMessage() {
        sendEmbedMessage(EmbedCreateSpec.builder()
                .title("AutoReconnecting in " + CONFIG.client.extra.autoReconnect.delaySeconds + "s")
                .color(Color.CYAN)
                .build());
    }

    public void sendEmbedMessage(EmbedCreateSpec embedCreateSpec) {
        try {
            mainChannelMessageQueue.add(MessageCreateSpec.builder()
                    .addEmbed(embedCreateSpec)
                    .build().asRequest());
        } catch (final Exception e) {
            DISCORD_LOG.error("Failed sending discord message", e);
        }
    }

    private void sendEmbedMessage(String message, EmbedCreateSpec embedCreateSpec) {
        try {
            mainChannelMessageQueue.add(MessageCreateSpec.builder()
                    .content(message)
                    .addEmbed(embedCreateSpec)
                    .build().asRequest());
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
