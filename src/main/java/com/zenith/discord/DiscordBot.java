package com.zenith.discord;

import com.zenith.Proxy;
import com.zenith.discord.command.*;
import com.zenith.util.Queue;
import com.zenith.util.cache.data.entity.Entity;
import com.zenith.util.cache.data.tab.PlayerEntry;
import discord4j.common.util.Snowflake;
import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.rest.RestClient;
import discord4j.rest.entity.RestChannel;
import discord4j.rest.util.Color;

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
                            restChannel.createMessage(
                                    command.execute(event, restChannel))
                                    .block();
                        } catch (final Exception e) {
                            DISCORD_LOG.error("Error executing discord command: " + command, e);
                        }
                    });
        });
    }

    public void sendQueueWarning(final int queuePosition) {
        sendEmbedMessage(EmbedCreateSpec.builder()
                .title("ZenithProxy Queue Warning" + " : " + CONFIG.authentication.username)
                .color(this.proxy.isConnected() ? Color.CYAN : Color.RUBY)
                .addField("Server", CONFIG.client.server.address, true)
                .addField("Queue Position", queuePosition + " / " + Queue.getQueueStatus().regular, false)
                .addField("Proxy IP", CONFIG.server.getProxyAddress(), false)
                .build());
    }

    public void sendOnline() {
        sendEmbedMessage(EmbedCreateSpec.builder()
                .title("ZenithProxy Online!" + " : " + CONFIG.authentication.username)
                .color(Color.CYAN)
                .addField("Server", CONFIG.client.server.address, true)
                .addField("Proxy IP", CONFIG.server.getProxyAddress(), false)
                .build());
    }

    public void sendStartQueueing() {
        sendEmbedMessage(EmbedCreateSpec.builder()
                .title("ZenithProxy Started Queuing..." + " : " + CONFIG.authentication.username)
                .color(Color.CYAN)
                .addField("Queue Position", Queue.getQueueStatus().regular + " / " + Queue.getQueueStatus().regular, false)
                .addField("Proxy IP", CONFIG.server.getProxyAddress(), false)
                .build());
    }

    public void sendDeath() {
        sendEmbedMessage(EmbedCreateSpec.builder()
                .title("Player Death!" + " : " + CONFIG.authentication.username)
                .color(Color.RUBY)
                .addField("Coordinates", getCoordinates(CACHE.getPlayerCache()), false)
                .build());
    }

    public void sendNewPlayerInVisualRange(Entity entity) {
        Optional<PlayerEntry> playerEntry = CACHE.getTabListCache().getTabList().getEntries().stream()
                .filter(e -> e.getId().equals(entity.getUuid()))
                .findFirst();
        sendEmbedMessage(EmbedCreateSpec.builder()
                .title("Player In Visual Range")
                .addField("Player Name", playerEntry.map(pe -> pe.getDisplayName()).orElse("Unknown"), true)
                .addField("Player UUID", playerEntry.map(pe -> pe.getId().toString()).orElse("Unknown"), true)
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
