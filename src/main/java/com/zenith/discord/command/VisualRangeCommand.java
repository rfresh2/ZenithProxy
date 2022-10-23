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
import java.util.stream.Collectors;

import static com.zenith.discord.DiscordBot.escape;
import static com.zenith.util.Constants.CONFIG;
import static com.zenith.util.Constants.saveConfig;

public class VisualRangeCommand extends Command {

    public VisualRangeCommand(Proxy proxy) {
        super(proxy, "visualRange", "Configure the VisualRange notification feature"
                + "\nUsage:"
                + "\n  " + CONFIG.discord.prefix + "visualRange on/off"
                + "\n  " + CONFIG.discord.prefix + "visualRange mention on/off"
                + "\n  " + CONFIG.discord.prefix + "visualRange friend add/del <username>"
                + "\n  " + CONFIG.discord.prefix + "visualRange friend list"
                + "\n  " + CONFIG.discord.prefix + "visualRange friend clear");
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
            CONFIG.client.extra.visualRangeAlert = true;
            embedBuilder
                    .title("VisualRange On!")
                    .color(Color.CYAN);
        } else if (commandArgs.get(1).equalsIgnoreCase("off")) {
            CONFIG.client.extra.visualRangeAlert = false;
            embedBuilder
                    .title("VisualRange Off!")
                    .color(Color.CYAN);
        } else if (commandArgs.get(1).equalsIgnoreCase("friend")) {
            if (commandArgs.size() < 3) {
                embedBuilder
                        .title("Invalid command usage")
                        .addField("Usage", this.description, false)
                        .color(Color.RUBY);
            } else if (commandArgs.get(2).equalsIgnoreCase("list")) {
                embedBuilder
                        .title("Friend list")
                        .addField("Friend List", friendListString(), false)
                        .color(Color.CYAN);
            } else if (commandArgs.get(2).equalsIgnoreCase("clear")) {
                CONFIG.client.extra.friendList.clear();
                embedBuilder
                        .title("Friend list cleared!")
                        .color(Color.CYAN);
            } if (commandArgs.size() < 4) {
                embedBuilder
                        .title("Invalid command usage")
                        .addField("Usage", this.description, false)
                        .color(Color.RUBY);
            } else if (commandArgs.get(2).equalsIgnoreCase("add")) {
                if (!CONFIG.client.extra.friendList.contains(commandArgs.get(3))) {
                    CONFIG.client.extra.friendList.add(commandArgs.get(3));
                }
                embedBuilder
                        .title("Friend added")
                        .addField("Friend List", friendListString(), false)
                        .color(Color.CYAN);
            } else if (commandArgs.get(2).equalsIgnoreCase("del")) {
                CONFIG.client.extra.friendList.removeIf(friend -> friend.equalsIgnoreCase(commandArgs.get(3)));
                embedBuilder
                        .title("Friend deleted")
                        .addField("Friend List", friendListString(), false)
                        .color(Color.CYAN);
            } else {
                embedBuilder
                        .title("Invalid command usage")
                        .addField("Usage", this.description, false)
                        .color(Color.RUBY);
            }
        } else if (commandArgs.get(1).equalsIgnoreCase("mention")) {
            if (commandArgs.size() < 3) {
                embedBuilder
                        .title("Invalid command usage")
                        .addField("Usage", this.description, false)
                        .color(Color.RUBY);
            } else if (commandArgs.get(2).equalsIgnoreCase("on")) {
                CONFIG.client.extra.visualRangeAlertMention = true;
                embedBuilder
                        .title("VisualRange Mentions On!")
                        .addField("Friend List", friendListString(), false)
                        .color(Color.CYAN);
            } else if (commandArgs.get(2).equalsIgnoreCase("off")) {
                CONFIG.client.extra.visualRangeAlertMention = false;
                embedBuilder
                        .title("VisualRange Mentions Off!")
                        .color(Color.CYAN);
            } else {
                embedBuilder
                        .title("Invalid command usage")
                        .addField("Usage", this.description, false)
                        .color(Color.RUBY);
            }
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

    private String friendListString() {
        return escape((CONFIG.client.extra.friendList.size() > 0 ? String.join(", ", CONFIG.client.extra.friendList) : "Friend List is empty"));
    }
}
