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

public class ChatRelayCommand extends Command {
    public ChatRelayCommand(Proxy proxy) {
        super(proxy, "chatRelay", "Configure the ChatRelay feature"
                + "\nUsage:"
                + "\n  " + CONFIG.discord.prefix + "chatRelay on/off"
                + "\n  " + CONFIG.discord.prefix + "chatRelay connectionMessages on/off"
                + "\n  " + CONFIG.discord.prefix + "chatRelay whisperMentions on/off");
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
            CONFIG.discord.chatRelay.enable = true;
            embedBuilder
                    .title("Chat Relay On!")
                    .color(Color.CYAN);
        } else if (commandArgs.get(1).equalsIgnoreCase("off")) {
            CONFIG.discord.chatRelay.enable = false;
            embedBuilder
                    .title("Chat Relay Off!")
                    .color(Color.CYAN);
        } else if (commandArgs.size() < 3) {
                embedBuilder
                        .title("Invalid command usage")
                        .addField("Usage", this.description, false)
                        .color(Color.RUBY);
        } else if (commandArgs.get(1).equalsIgnoreCase("connectionMessages")) {
            if (commandArgs.get(2).equalsIgnoreCase("on")) {
                CONFIG.discord.chatRelay.connectionMessages = true;
                embedBuilder
                        .title("Connection Messages Relay On!")
                        .color(Color.CYAN);
            } else if (commandArgs.get(2).equalsIgnoreCase("off")) {
                CONFIG.discord.chatRelay.connectionMessages = false;
                embedBuilder
                        .title("Connection Messages Relay Off!")
                        .color(Color.CYAN);
            } else {
                embedBuilder
                        .title("Invalid command usage")
                        .addField("Usage", this.description, false)
                        .color(Color.RUBY);
            }
        } else if (commandArgs.get(1).equalsIgnoreCase("whisperMentions")) {
            if (commandArgs.get(2).equalsIgnoreCase("on")) {
                CONFIG.discord.chatRelay.mentionRoleOnWhisper = true;
                embedBuilder
                        .title("Whisper Mentions Relay On!")
                        .color(Color.CYAN);
            } else if (commandArgs.get(2).equalsIgnoreCase("off")) {
                CONFIG.discord.chatRelay.mentionRoleOnWhisper = false;
                embedBuilder
                        .title("Whisper Mentions Relay Off!")
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
