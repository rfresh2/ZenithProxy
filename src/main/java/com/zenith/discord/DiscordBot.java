package com.zenith.discord;

import com.zenith.Proxy;
import com.zenith.discord.command.*;
import com.zenith.util.Queue;
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

import static com.zenith.util.Constants.CONFIG;

public class DiscordBot {

    private RestClient restClient;
    private Proxy proxy;
    private List<Command> commands = new ArrayList<>();

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
                    .ifPresent(command -> restChannel.createMessage(
                            command.execute(event, restChannel))
                            .block());
        });
    }

    public void sendQueueWarning(final int queuePosition) {
        RestChannel restChannel = restClient.getChannelById(Snowflake.of(CONFIG.discord.channelId));
        restChannel.createMessage(MessageCreateSpec.builder()
                .addEmbed(EmbedCreateSpec.builder()
                        .title("ZenithProxy Queue Warning" + " : " + CONFIG.authentication.username)
                        .color(this.proxy.isConnected() ? Color.CYAN : Color.RUBY)
                        .addField("Server", CONFIG.client.server.address, true)
                        .addField("Queue Position", queuePosition + " / " + Queue.getQueueStatus().regular, false)
                        .addField("Proxy IP", "todo:" + CONFIG.server.bind.port, false)
                        .build())
                .build().asRequest()).block();
    }

    public void sendDoneQueueing() {
        RestChannel restChannel = restClient.getChannelById(Snowflake.of(CONFIG.discord.channelId));
        restChannel.createMessage(MessageCreateSpec.builder()
                .addEmbed(EmbedCreateSpec.builder()
                        .title("ZenithProxy Queue Complete" + " : " + CONFIG.authentication.username)
                        .color(Color.CYAN)
                        .addField("Server", CONFIG.client.server.address, true)
                        .addField("Proxy IP", "todo:" + CONFIG.server.bind.port, false)
                        .build())
                .build().asRequest()).block();
    }

    public void sendStartQueueing() {
        RestChannel restChannel = restClient.getChannelById(Snowflake.of(CONFIG.discord.channelId));
        restChannel.createMessage(MessageCreateSpec.builder()
                .addEmbed(EmbedCreateSpec.builder()
                        .title("ZenithProxy Started Queuing..." + " : " + CONFIG.authentication.username)
                        .color(Color.CYAN)
                        .addField("Queue Position", Queue.getQueueStatus().regular + " / " + Queue.getQueueStatus().regular, false)
                        .addField("Proxy IP", "todo:" + CONFIG.server.bind.port, false)
                        .build())
                .build().asRequest()).block();
    }
}
