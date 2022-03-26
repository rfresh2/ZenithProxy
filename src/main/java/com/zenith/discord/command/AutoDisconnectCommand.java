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

public class AutoDisconnectCommand extends Command {
    public AutoDisconnectCommand(Proxy proxy) {
        super(proxy, "autoDisconnect", "Auto disconnect on health reaching a certain level"
                + "\nUsage: "
                + "\n " + CONFIG.discord.prefix + "autoDisconnect on/off"
                + "\n " + CONFIG.discord.prefix + "autoDisconnect health <Integer>");
    }

    @Override
    public MultipartRequest<MessageCreateRequest> execute(MessageCreateEvent event, RestChannel restChannel) {
        List<String> commandArgs = Arrays.asList(event.getMessage().getContent().split(" "));
        EmbedCreateSpec.Builder embedBuilder = EmbedCreateSpec.builder();

        if (commandArgs.size() < 1) {
            embedBuilder
                    .title("Invalid command usage")
                    .addField("Usage", this.description, false)
                    .color(Color.RUBY);
        } else if (commandArgs.get(1).equalsIgnoreCase("on")) {
            CONFIG.client.extra.utility.actions.autoDisconnect.enabled = true;
            embedBuilder
                    .title("AutoDisconnect On!")
                    .addField("Health", ""+CONFIG.client.extra.utility.actions.autoDisconnect.health, false)
                    .color(Color.CYAN);
        } else if (commandArgs.get(1).equalsIgnoreCase("off")) {
            CONFIG.client.extra.utility.actions.autoDisconnect.enabled = false;
            embedBuilder
                    .title("AutoDisconnect Off!")
                    .addField("Health", ""+CONFIG.client.extra.utility.actions.autoDisconnect.health, false)
                    .color(Color.CYAN);
        } else if (commandArgs.size() < 2) {
            embedBuilder
                    .title("Invalid command usage")
                    .addField("Usage", this.description, false)
                    .color(Color.RUBY);
        } else if (commandArgs.get(1).equalsIgnoreCase("health")) {
           try {
               int health = Integer.parseInt(commandArgs.get(2));
               CONFIG.client.extra.utility.actions.autoDisconnect.health = health;
               embedBuilder
                       .title("AutoDisconnect Health Updated!")
                       .addField("Status", (CONFIG.client.extra.utility.actions.autoDisconnect.enabled ? "on" : "off"), false)
                       .addField("Health", ""+CONFIG.client.extra.utility.actions.autoDisconnect.health, false)
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
