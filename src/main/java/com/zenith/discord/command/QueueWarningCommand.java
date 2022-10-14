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

public class QueueWarningCommand extends Command {
    public QueueWarningCommand(Proxy proxy) {
        super(proxy, "queueWarning", "Configure warning messages for when 2b2t queue positions are reached"
                + "\nUsage: "
                + "\n " + CONFIG.discord.prefix + "queueWarning on/off"
                + "\n " + CONFIG.discord.prefix + "queueWarning position <Integer>"
                + "\n " + CONFIG.discord.prefix + "queueWarning mention on/off");
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
            CONFIG.discord.queueWarning.enabled = true;
            embedBuilder
                    .title("QueueWarning On!")
                    .addField("Position", ""+CONFIG.discord.queueWarning.position, false)
                    .addField("mention", (CONFIG.discord.queueWarning.mentionRole ? "on" : "off"), false)
                    .color(Color.CYAN);
        } else if (commandArgs.get(1).equalsIgnoreCase("off")) {
            CONFIG.discord.queueWarning.enabled = false;
            embedBuilder
                    .title("QueueWarning Off!")
                    .addField("Position", ""+CONFIG.discord.queueWarning.position, false)
                    .addField("mention", (CONFIG.discord.queueWarning.mentionRole ? "on" : "off"), false)
                    .color(Color.CYAN);
        } else if (commandArgs.get(1).equalsIgnoreCase("mention")) {
            if (commandArgs.size() < 3) {
                embedBuilder
                        .title("Invalid command usage")
                        .addField("Usage", this.description, false)
                        .color(Color.RUBY);
            } else if (commandArgs.get(2).equalsIgnoreCase("on")) {
                CONFIG.discord.queueWarning.mentionRole = true;
                embedBuilder
                        .title("QueueWarning Mention On!")
                        .addField("Status", (CONFIG.discord.queueWarning.enabled ? "on" : "off"), false)
                        .addField("Position", ""+CONFIG.discord.queueWarning.position, false)
                        .color(Color.CYAN);
            } else if (commandArgs.get(2).equalsIgnoreCase("off")) {
                CONFIG.discord.queueWarning.mentionRole = false;
                embedBuilder
                        .title("QueueWarning Mention Off!")
                        .addField("Status", (CONFIG.discord.queueWarning.enabled ? "on" : "off"), false)
                        .addField("Position", ""+CONFIG.discord.queueWarning.position, false)
                        .color(Color.CYAN);
            } else {
                embedBuilder
                        .title("Invalid command usage")
                        .addField("Usage", this.description, false)
                        .color(Color.RUBY);
            }
        } else if (commandArgs.get(1).equalsIgnoreCase("position")) {
            try {
                int position = Integer.parseInt(commandArgs.get(2));
                CONFIG.discord.queueWarning.position = position;
                embedBuilder
                        .title("QueueWarning Position Updated!")
                        .addField("Status", (CONFIG.discord.queueWarning.enabled ? "on" : "off"), false)
                        .addField("Position", ""+CONFIG.discord.queueWarning.position, false)
                        .addField("mention", (CONFIG.discord.queueWarning.mentionRole ? "on" : "off"), false)
                        .color(Color.CYAN);
            } catch (final Exception e) {
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
}
