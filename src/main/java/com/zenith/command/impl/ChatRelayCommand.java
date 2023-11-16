package com.zenith.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.command.Command;
import com.zenith.command.CommandCategory;
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
            CommandCategory.MANAGE,
            "Configures the ChatRelay feature",
            asList("on/off",
                   "connectionMessages on/off",
                   "whispers on/off",
                   "publicChat on/off",
                   "deathMessages on/off",
                   "serverMessages on/off",
                   "whisperMentions on/off",
                   "nameMentions on/off",
                   "mentionsWhileConnected on/off",
                   "sendMessages on/off"
            )
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
            .then(literal("whispers")
                      .then(argument("toggle", toggle()).executes(c -> {
                            CONFIG.discord.chatRelay.whispers = getToggle(c, "toggle");
                            c.getSource().getEmbedBuilder()
                                .title("Whispers " + (CONFIG.discord.chatRelay.whispers ? "On!" : "Off!"));
                            return 1;
                      })))
            .then(literal("publicChat")
                      .then(argument("toggle", toggle()).executes(c -> {
                            CONFIG.discord.chatRelay.publicChats = getToggle(c, "toggle");
                            c.getSource().getEmbedBuilder()
                                .title("Public Chat " + (CONFIG.discord.chatRelay.publicChats ? "On!" : "Off!"));
                            return 1;
                      })))
            .then(literal("deathMessages")
                      .then(argument("toggle", toggle()).executes(c -> {
                          CONFIG.discord.chatRelay.deathMessages = getToggle(c, "toggle");
                          c.getSource().getEmbedBuilder()
                              .title("Death Messages " + (CONFIG.discord.chatRelay.deathMessages ? "On!" : "Off!"));
                          return 1;
                      })))
            .then(literal("serverMessages")
                      .then(argument("toggle", toggle()).executes(c -> {
                          CONFIG.discord.chatRelay.serverMessages = getToggle(c, "toggle");
                          c.getSource().getEmbedBuilder()
                              .title("Server Messages " + (CONFIG.discord.chatRelay.serverMessages ? "On!" : "Off!"));
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
                      })))
            .then(literal("sendMessages")
                      .then(argument("toggle", toggle()).executes(c -> {
                            CONFIG.discord.chatRelay.sendMessages = getToggle(c, "toggle");
                            c.getSource().getEmbedBuilder()
                                .title("Send Messages " + (CONFIG.discord.chatRelay.sendMessages ? "On!" : "Off!"));
                            return 1;
                      })));
    }

    @Override
    public void postPopulate(final EmbedCreateSpec.Builder builder) {
        builder
            .addField("Chat Relay", toggleStr(CONFIG.discord.chatRelay.enable), false)
            .addField("Connection Messages", toggleStr(CONFIG.discord.chatRelay.connectionMessages), false)
            .addField("Public Chats", toggleStr(CONFIG.discord.chatRelay.publicChats), false)
            .addField("Whispers", toggleStr(CONFIG.discord.chatRelay.whispers), false)
            .addField("Death Messages", toggleStr(CONFIG.discord.chatRelay.deathMessages), false)
            .addField("Server Messages", toggleStr(CONFIG.discord.chatRelay.serverMessages), false)
            .addField("Whisper Mentions", toggleStr(CONFIG.discord.chatRelay.mentionRoleOnWhisper), false)
            .addField("Name Mentions", toggleStr(CONFIG.discord.chatRelay.mentionRoleOnNameMention), false)
            .addField("Mentions While Connected", toggleStr(CONFIG.discord.chatRelay.mentionWhileConnected), false)
            .addField("Send Messages", toggleStr(CONFIG.discord.chatRelay.sendMessages), false)
            .color(Color.CYAN);
    }
}
