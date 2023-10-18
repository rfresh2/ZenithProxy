package com.zenith.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.command.Command;
import com.zenith.command.CommandContext;
import com.zenith.command.CommandUsage;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;

import static com.zenith.Shared.CONFIG;
import static com.zenith.command.ToggleArgumentType.getToggle;
import static com.zenith.command.ToggleArgumentType.toggle;
import static java.util.Arrays.asList;

public class ChatRelayCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.args(
                "chatRelay",
                "Configures the ChatRelay feature",
                asList("on/off", "connectionMessages on/off", "whisperMentions on/off",
                        "nameMentions on/off", "mentionsWhileConnected on/off")
        );
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("chatRelay")
            .then(argument("toggle", toggle()).executes(c -> {
                CONFIG.discord.chatRelay.enable = getToggle(c, "toggle");
                c.getSource().getEmbedBuilder()
                    .title("Chat Relay " + (CONFIG.discord.chatRelay.enable ? "On!" : "Off!"));
                return 1;
            }))
            .then(literal("connectionMessages")
                      .then(argument("toggle", toggle()).executes(c -> {
                            CONFIG.discord.chatRelay.connectionMessages = getToggle(c, "toggle");
                            c.getSource().getEmbedBuilder()
                                .title("Connection Messages " + (CONFIG.discord.chatRelay.connectionMessages ? "On!" : "Off!"));
                            return 1;
                      })))
            .then(literal("whisperMentions")
                      .then(argument("toggle", toggle()).executes(c -> {
                            CONFIG.discord.chatRelay.mentionRoleOnWhisper = getToggle(c, "toggle");
                            c.getSource().getEmbedBuilder()
                                .title("Whisper Mentions " + (CONFIG.discord.chatRelay.mentionRoleOnWhisper ? "On!" : "Off!"));
                            return 1;
                      })))
            .then(literal("nameMentions")
                      .then(argument("toggle", toggle()).executes(c -> {
                            CONFIG.discord.chatRelay.mentionRoleOnNameMention = getToggle(c, "toggle");
                            c.getSource().getEmbedBuilder()
                                .title("Name Mentions " + (CONFIG.discord.chatRelay.mentionRoleOnNameMention ? "On!" : "Off!"));
                            return 1;
                      })))
            .then(literal("mentionsWhileConnected")
                      .then(argument("toggle", toggle()).executes(c -> {
                            CONFIG.discord.chatRelay.mentionWhileConnected = getToggle(c, "toggle");
                            c.getSource().getEmbedBuilder()
                                .title("Mentions While Connected " + (CONFIG.discord.chatRelay.mentionWhileConnected ? "On!" : "Off!"));
                            return 1;
                      })));
    }

    @Override
    public void postPopulate(final EmbedCreateSpec.Builder builder) {
        builder
            .addField("Chat Relay", toggleStr(CONFIG.discord.chatRelay.enable), false)
            .addField("Connection Messages", toggleStr(CONFIG.discord.chatRelay.connectionMessages), false)
            .addField("Whisper Mentions", toggleStr(CONFIG.discord.chatRelay.mentionRoleOnWhisper), false)
            .addField("Name Mentions", toggleStr(CONFIG.discord.chatRelay.mentionRoleOnNameMention), false)
            .addField("Mentions While Connected", toggleStr(CONFIG.discord.chatRelay.mentionWhileConnected), false)
            .color(Color.CYAN);
    }
}
