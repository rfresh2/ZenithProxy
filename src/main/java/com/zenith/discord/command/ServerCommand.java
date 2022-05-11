package com.zenith.discord.command;

import com.zenith.Proxy;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.discordjson.json.MessageCreateRequest;
import discord4j.rest.entity.RestChannel;
import discord4j.rest.util.Color;
import discord4j.rest.util.MultipartRequest;

import java.util.Arrays;
import java.util.List;

import static com.zenith.util.Constants.*;

public class ServerCommand extends Command {
    public ServerCommand(Proxy proxy) {
        super(proxy, "server", "Change the server the proxy connects to."
                + "\nUsage:"
                + "\n " + CONFIG.discord.prefix + "server <ip>"
                + "\n " + CONFIG.discord.prefix + "server <ip> <port>");
    }

    @Override
    public MultipartRequest<MessageCreateRequest> execute(MessageCreateEvent event, RestChannel restChannel) {
        List<String> commandArgs = Arrays.asList(event.getMessage().getContent().split(" "));
        EmbedCreateSpec.Builder embedBuilder = EmbedCreateSpec.builder();

        if (commandArgs.size() < 2 || commandArgs.size() > 3) {
            embedBuilder
                    .title("Invalid command usage")
                    .addField("Usage", this.description, false)
                    .color(Color.RUBY);
        } else {
            try {
                if (commandArgs.size() == 3) {
                    CONFIG.client.server.port = Integer.parseInt(commandArgs.get(2));
                } else {
                    CONFIG.client.server.port = 25565;
                }
                CONFIG.client.server.address = commandArgs.get(1);
                embedBuilder
                        .title("Server Updated!")
                        .addField("IP", CONFIG.client.server.address, false)
                        .addField("Port", "" + CONFIG.client.server.port, true)
                        .color(Color.CYAN);
            } catch (final Exception e) {
                DISCORD_LOG.error(e);
                embedBuilder
                        .title("Invalid command usage")
                        .addField("Usage", this.description, false)
                        .color(Color.RUBY);
            }
        }

        saveConfig();
        return MessageCreateSpec.builder()
                .addEmbed(embedBuilder
                        .build())
                .build().asRequest();
    }
}
