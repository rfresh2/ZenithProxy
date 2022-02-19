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

public class AutoReconnectCommand extends Command {
    public AutoReconnectCommand(Proxy proxy) {
        super(proxy, "autoReconnect", "Configure the AutoReconnect feature"
                + "\nUsage:"
                + "\n  " + CONFIG.discord.prefix + "autoReconnect on/off"
                + "\n  " + CONFIG.discord.prefix + "autoReconnect delay <Seconds>");
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
            CONFIG.client.extra.autoReconnect.enabled = true;
            embedBuilder
                    .title("AutoReconnect Enabled!")
                    .addField("Delay", ""+CONFIG.client.extra.autoReconnect.enabled, false)
                    .color(Color.CYAN);
        } else if (commandArgs.get(1).equalsIgnoreCase("on")) {
            CONFIG.client.extra.autoReconnect.enabled = false;
            embedBuilder
                    .title("AutoReconnect Disabled!")
                    .addField("Delay", ""+CONFIG.client.extra.autoReconnect.enabled, false)
                    .color(Color.CYAN);
        } else if (commandArgs.size() < 2) {
            embedBuilder
                    .title("Invalid command usage")
                    .addField("Usage", this.description, false)
                    .color(Color.RUBY);
        } else if (commandArgs.get(1).equalsIgnoreCase("delay")) {
            try {
                int delay = Integer.parseInt(commandArgs.get(2));
                CONFIG.client.extra.autoReconnect.delaySeconds = delay;
                embedBuilder
                        .title("AutoReconnect Delay Updated!")
                        .addField("Enabled", String.valueOf(CONFIG.client.extra.autoReconnect.enabled), false)
                        .addField("Delay", ""+CONFIG.client.extra.autoReconnect.delaySeconds, false)
                        .color(Color.CYAN);
            } catch (final Exception e) {
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
