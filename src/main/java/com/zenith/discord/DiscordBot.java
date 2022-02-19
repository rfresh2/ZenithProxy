package com.zenith.discord;

import com.collarmc.pounce.Subscribe;
import com.zenith.Proxy;
import com.zenith.discord.command.*;
import com.zenith.event.proxy.*;
import com.zenith.util.Queue;
import discord4j.common.util.Snowflake;
import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.discordjson.json.MessageCreateRequest;
import discord4j.rest.RestClient;
import discord4j.rest.entity.RestChannel;
import discord4j.rest.util.Color;
import discord4j.rest.util.MultipartRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.zenith.discord.command.StatusCommand.getCoordinates;
import static com.zenith.util.Constants.*;

public class DiscordBot {

    private RestClient restClient;
    private Proxy proxy;
    public List<Command> commands = new ArrayList<>();

    public DiscordBot() {

    }

    public void start(Proxy proxy) {
        this.proxy = proxy;
        GatewayDiscordClient client = DiscordClient.create(CONFIG.discord.token)
                .login()
                .block();
        EVENT_BUS.subscribe(this);

        restClient = client.getRestClient();

        commands.add(new ConnectCommand(this.proxy));
        commands.add(new DisconnectCommand(this.proxy));
        commands.add(new StatusCommand(this.proxy));
        commands.add(new HelpCommand(this.proxy, this.commands));
        commands.add(new WhitelistCommand(this.proxy));

        client.getEventDispatcher().on(MessageCreateEvent.class).subscribe(event -> {
            if (!event.getMessage().getChannelId().equals(Snowflake.of(CONFIG.discord.channelId))) {
                return;
            }
            final String message = event.getMessage().getContent();
            if (!message.startsWith(CONFIG.discord.prefix)) {
                return;
            }
            RestChannel restChannel = restClient.getChannelById(event.getMessage().getChannelId());
            commands.stream()
                    .filter(command -> message.startsWith(CONFIG.discord.prefix + command.getName()))
                    .findFirst()
                    .ifPresent(command -> {
                        try {
                            MultipartRequest<MessageCreateRequest> m = command.execute(event, restChannel);
                            if (m != null) {
                                restChannel.createMessage(m).block();
                            }
                        } catch (final Exception e) {
                            DISCORD_LOG.error("Error executing discord command: " + command, e);
                        }
                    });
        });
    }

    @Subscribe
    public void handleConnectEvent(ConnectEvent event) {
       sendEmbedMessage(EmbedCreateSpec.builder()
               .title("ZenithProxy Connected!" + " : " + CONFIG.authentication.username)
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
                .title("ZenithProxy Online!" + " : " + CONFIG.authentication.username)
                .color(Color.CYAN)
                .addField("Server", CONFIG.client.server.address, true)
                .addField("Proxy IP", CONFIG.server.getProxyAddress(), false)
                .build());
    }

    @Subscribe
    public void handleDisconnectEvent(DisconnectEvent event) {
        sendEmbedMessage(EmbedCreateSpec.builder()
                .title("ZenithProxy Disconnected" + " : " + CONFIG.authentication.username)
                .addField("Reason", event.reason, true)
                .color(Color.CYAN)
                .build());
    }

    @Subscribe
    public void handleQueuePositionUpdateEvent(QueuePositionUpdateEvent event) {
        if (event.position == CONFIG.server.queueWarning) {
            sendQueueWarning(event.position);
        } else if (event.position <= 3) {
            sendQueueWarning(event.position);
        }
    }

    private void sendQueueWarning(int position) {
        sendEmbedMessage(EmbedCreateSpec.builder()
                .title("ZenithProxy Queue Warning" + " : " + CONFIG.authentication.username)
                .color(this.proxy.isConnected() ? Color.CYAN : Color.RUBY)
                .addField("Server", CONFIG.client.server.address, true)
                .addField("Queue Position", position + " / " + Queue.getQueueStatus().regular, false)
                .addField("Proxy IP", CONFIG.server.getProxyAddress(), false)
                .build());
    }

    @Subscribe
    public void handleQueueCompleteEvent(QueueCompleteEvent event) {

    }

    @Subscribe
    public void handleStartQueueEvent(StartQueueEvent event) {
        sendEmbedMessage(EmbedCreateSpec.builder()
                .title("ZenithProxy Started Queuing..." + " : " + CONFIG.authentication.username)
                .color(Color.CYAN)
                .addField("Regular Queue", ""+Queue.getQueueStatus().regular, true)
                .addField("Priority Queue", ""+Queue.getQueueStatus().prio, true)
                .build());
    }

    @Subscribe
    public void handleDeathEvent(DeathEvent event) {
        sendEmbedMessage(EmbedCreateSpec.builder()
                .title("Player Death!" + " : " + CONFIG.authentication.username)
                .color(Color.RUBY)
                .addField("Coordinates", getCoordinates(CACHE.getPlayerCache()), false)
                .build());
    }

    @Subscribe
    public void handleNewPlayerInVisualRangeEvent(NewPlayerInVisualRangeEvent event) {
        sendEmbedMessage(EmbedCreateSpec.builder()
                .title("Player In Visual Range")
                .addField("Player Name", Optional.ofNullable(event.playerEntry.getName()).orElse("Unknown"), true)
                .addField("Player UUID", event.playerEntry.getId().toString(), true)
                .image(this.proxy.getAvatarURL(event.playerEntry.getId()).toString())
                .build());
    }

    private void sendEmbedMessage(EmbedCreateSpec embedCreateSpec) {
        try {
            RestChannel restChannel = restClient.getChannelById(Snowflake.of(CONFIG.discord.channelId));
            restChannel.createMessage(MessageCreateSpec.builder()
                    .addEmbed(embedCreateSpec)
                    .build().asRequest()).block();
        } catch (final Exception e) {
            DISCORD_LOG.error("Failed sending discord message", e);
        }

    }
}
