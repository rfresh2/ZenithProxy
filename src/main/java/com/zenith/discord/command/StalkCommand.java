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

import static com.zenith.util.Constants.CONFIG;
import static com.zenith.util.Constants.saveConfig;

public class StalkCommand extends Command {
    public StalkCommand(Proxy proxy) {
        super(proxy, "stalk", "Configures the stalk module which sends discord mentions when a player connects "
                + "\nUsage:"
                + "\n " + CONFIG.discord.prefix + "stalk on/off"
                + "\n " + CONFIG.discord.prefix + "stalk list"
                + "\n " + CONFIG.discord.prefix + "stalk add/del <player name>");
    }

    @Override
    public MultipartRequest<MessageCreateRequest> execute(MessageCreateEvent event, RestChannel restChannel) {
        List<String> commandArgs = Arrays.asList(event.getMessage().getContent().split(" "));
        EmbedCreateSpec.Builder embedBuilder = EmbedCreateSpec.builder();
        if (commandArgs.size() < 2) {
            embedBuilder
                    .title("Invalid command usage")
                    .addField("Usage", this.description, false)
                    .color(Color.RUBY);
        } else if (commandArgs.get(1).equalsIgnoreCase("on")) {
            CONFIG.client.extra.stalk.enabled = true;
            embedBuilder
                    .title("Stalk On!")
                    .color(Color.CYAN);
        } else if (commandArgs.get(1).equalsIgnoreCase("off")) {
            CONFIG.client.extra.stalk.enabled = false;
            embedBuilder
                    .title("Stalk Off!")
                    .color(Color.RUBY);
        } else if (commandArgs.get(1).equalsIgnoreCase("list")) {
            embedBuilder
                    .title("Stalk List")
                    .color(Color.CYAN)
                    .addField("Players", ((CONFIG.client.extra.stalk.stalkList.size() > 0)
                                    ? String.join(", ", CONFIG.client.extra.stalk.stalkList)
                                    : "Stalk list is empty"),
                            false);
        } else if (commandArgs.size() < 3) {
            embedBuilder
                    .title("Invalid command usage")
                    .addField("Usage", this.description, false)
                    .color(Color.RUBY);
        } else if (commandArgs.get(1).equalsIgnoreCase("add")) {
            if (!CONFIG.client.extra.stalk.stalkList.contains(commandArgs.get(2))) {
                CONFIG.client.extra.stalk.stalkList.add(commandArgs.get(2));
            }
            embedBuilder
                    .title("Added player: " + commandArgs.get(2) + " To Stalk List")
                    .color(Color.CYAN)
                    .addField("Players", ((CONFIG.client.extra.stalk.stalkList.size() > 0)
                                    ? String.join(", ", CONFIG.client.extra.stalk.stalkList)
                                    : "Stalk list is empty"),
                            false);
        } else if (commandArgs.get(1).equalsIgnoreCase("del")) {
            CONFIG.client.extra.stalk.stalkList.removeIf(s -> s.equalsIgnoreCase(commandArgs.get(2)));
            embedBuilder
                    .title("Removed player: " + commandArgs.get(2) + " From Stalk List")
                    .color(Color.CYAN)
                    .addField("Players", ((CONFIG.client.extra.stalk.stalkList.size() > 0)
                                    ? String.join(", ", CONFIG.client.extra.stalk.stalkList)
                                    : "Stalk list is empty"),
                            false);
        } else {
            embedBuilder
                    .title("Invalid command usage")
                    .addField("Usage", this.description, false)
                    .color(Color.RUBY);
        }

        saveConfig();
        return MessageCreateSpec.builder()
                .addEmbed(embedBuilder
                        .build())
                .build().asRequest();
    }
}
