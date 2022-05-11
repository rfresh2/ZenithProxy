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

public class AutoRespawnCommand extends Command {
    public AutoRespawnCommand(Proxy proxy) {
        super(proxy, "autoRespawn", "Configure the AutoRespawn feature"
                + "\nUsage:"
                + "\n  " + CONFIG.discord.prefix + "autoRespawn on/off"
                + "\n  " + CONFIG.discord.prefix + "autoRespawn delay <Milliseconds>");
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
            CONFIG.client.extra.autoRespawn.enabled = true;
            embedBuilder
                    .title("AutoRespawn On!")
                    .addField("Delay", ""+CONFIG.client.extra.autoRespawn.enabled, false)
                    .color(Color.CYAN);
        } else if (commandArgs.get(1).equalsIgnoreCase("off")) {
            CONFIG.client.extra.autoRespawn.enabled = false;
            embedBuilder
                    .title("AutoRespawn Off!")
                    .addField("Delay", ""+CONFIG.client.extra.autoRespawn.enabled, false)
                    .color(Color.CYAN);
        } else if (commandArgs.size() < 3) {
            embedBuilder
                    .title("Invalid command usage")
                    .addField("Usage", this.description, false)
                    .color(Color.RUBY);
        } else if (commandArgs.get(1).equalsIgnoreCase("delay")) {
            try {
                int delay = Integer.parseInt(commandArgs.get(2));
                CONFIG.client.extra.autoRespawn.delayMillis = delay;
                embedBuilder
                        .title("AutoRespawn Delay Updated!")
                        .addField("Status", (CONFIG.client.extra.autoRespawn.enabled ? "on" : "off"), false)
                        .addField("Delay", ""+CONFIG.client.extra.autoRespawn.delayMillis, false)
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
