package com.zenith.discord.command;

import com.zenith.Proxy;
import com.zenith.discord.DiscordBot;
import com.zenith.module.AutoReply;
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

public class AutoReplyCommand extends Command {
    public AutoReplyCommand(Proxy proxy) {
        super(proxy, "autoReply", "Configure the AutoReply feature"
                + "\nUsage:"
                + "\n  " + CONFIG.discord.prefix + "autoReply on/off"
                + "\n  " + CONFIG.discord.prefix + "autoReply cooldown <Seconds>"
                + "\n  " + CONFIG.discord.prefix + "autoReply message <Message>");
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
            CONFIG.client.extra.autoReply.enabled = true;
            embedBuilder
                    .title("AutoReply On!")
                    .addField("Cooldown Seconds", ""+CONFIG.client.extra.autoReply.cooldownSeconds, false)
                    .addField("Message", CONFIG.client.extra.autoReply.message, false)
                    .color(Color.CYAN);
        } else if (commandArgs.get(1).equalsIgnoreCase("off")) {
            CONFIG.client.extra.autoReply.enabled = false;
            embedBuilder
                    .title("AutoReply Off!")
                    .addField("Cooldown Seconds", ""+CONFIG.client.extra.autoReply.cooldownSeconds, false)
                    .addField("Message", CONFIG.client.extra.autoReply.message, false)
                    .color(Color.CYAN);
        } else if (commandArgs.size() < 3) {
            embedBuilder
                    .title("Invalid command usage")
                    .addField("Usage", this.description, false)
                    .color(Color.RUBY);
        } else if (commandArgs.get(1).equalsIgnoreCase("cooldown")) {
            try {
                int delay = Integer.parseInt(commandArgs.get(2));
                this.proxy.getModules().stream().filter(m -> m instanceof AutoReply).findFirst().ifPresent(m -> ((AutoReply) m).updateCooldown(delay));
                embedBuilder
                        .title("AutoReply Cooldown Updated!")
                        .addField("Status", (CONFIG.client.extra.autoReply.enabled ? "on" : "off"), false)
                        .addField("Cooldown Seconds", "" + CONFIG.client.extra.autoReply.cooldownSeconds, false)
                        .addField("Message", CONFIG.client.extra.autoReply.message, false)
                        .color(Color.CYAN);
            } catch (final Exception e) {
                embedBuilder
                        .title("Invalid command usage")
                        .addField("Usage", this.description, false)
                        .color(Color.RUBY);
            }
        } else if (commandArgs.get(1).equalsIgnoreCase("message")) {
            try {
                String message = DiscordBot.sanitizeRelayInputMessage(String.join(" ", commandArgs.subList(2, commandArgs.size())));
                if (message.length() > 236) { message = message.substring(0, 236); }
                CONFIG.client.extra.autoReply.message = message;
                embedBuilder
                        .title("AutoReply Message Updated!")
                        .addField("Status", (CONFIG.client.extra.autoReply.enabled ? "on" : "off"), false)
                        .addField("Cooldown Seconds", ""+CONFIG.client.extra.autoReply.cooldownSeconds, false)
                        .addField("Message", CONFIG.client.extra.autoReply.message, false)
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
