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

public class PrioCommand extends Command {
    public PrioCommand(Proxy proxy) {
        super(proxy, "prio", "Configure the mentions for 2b2t priority & priority ban updates"
                + "\nUsage:"
                + "\n  " + CONFIG.discord.prefix + "prio mentions on/off"
                + "\n  " + CONFIG.discord.prefix + "prio banMentions on/off");
    }

    @Override
    public MultipartRequest<MessageCreateRequest> execute(MessageCreateEvent event, RestChannel restChannel) {
        List<String> commandArgs = Arrays.asList(event.getMessage().getContent().split(" "));
        EmbedCreateSpec.Builder embedBuilder = EmbedCreateSpec.builder();

        if (commandArgs.size() < 3) {
            embedBuilder
                    .title("Invalid command usage")
                    .addField("Usage", this.description, false)
                    .color(Color.RUBY);
        } else if (commandArgs.get(1).equalsIgnoreCase("mentions")) {
            if (commandArgs.get(2).equalsIgnoreCase("on")) {
                CONFIG.discord.mentionRoleOnPrioUpdate = true;
                embedBuilder
                        .title("Prio Mentions On!")
                        .color(Color.CYAN);
            } else if (commandArgs.get(2).equalsIgnoreCase("off")) {
                CONFIG.discord.mentionRoleOnPrioUpdate = false;
                embedBuilder
                        .title("Prio Mentions Off!")
                        .color(Color.CYAN);
            } else {
                embedBuilder
                        .title("Invalid command usage")
                        .addField("Usage", this.description, false)
                        .color(Color.RUBY);
            }
        } else if (commandArgs.get(1).equalsIgnoreCase("banMentions")) {
            if (commandArgs.get(2).equalsIgnoreCase("on")) {
                CONFIG.discord.mentionRoleOnPrioBanUpdate = true;
                embedBuilder
                        .title("Prio Ban Mentions On!")
                        .color(Color.CYAN);
            } else if (commandArgs.get(2).equalsIgnoreCase("off")) {
                CONFIG.discord.mentionRoleOnPrioBanUpdate = false;
                embedBuilder
                        .title("Prio Ban Mentions Off!")
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
                .build().asRequest();    }
}
