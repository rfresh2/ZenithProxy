package com.zenith.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.command.Command;
import com.zenith.command.CommandUsage;
import com.zenith.command.brigadier.CommandCategory;
import com.zenith.command.brigadier.CommandContext;
import com.zenith.discord.Embed;
import com.zenith.module.impl.ChatHistory;
import discord4j.rest.util.Color;

import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.zenith.Shared.CONFIG;
import static com.zenith.Shared.MODULE;
import static com.zenith.command.brigadier.ToggleArgumentType.getToggle;
import static com.zenith.command.brigadier.ToggleArgumentType.toggle;
import static java.util.Arrays.asList;

public class ChatHistoryCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.args(
            "chatHistory",
            CommandCategory.MODULE,
            """
            Caches and sends recent chat history to players and spectators who connect to the proxy.
            Includes whispers, chat, and system messages.
            """,
            asList(
                "on/off",
                "seconds <seconds>",
                "maxCount <maxCount>",
                "spectators on/off"
            )
        );
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("chatHistory")
            .then(argument("toggle", toggle()).executes(c -> {
                CONFIG.server.extra.chatHistory.enable = getToggle(c, "toggle");
                MODULE.get(ChatHistory.class).syncEnabledFromConfig();
                c.getSource().getEmbed()
                    .title("Chat History " + toggleStrCaps(CONFIG.server.extra.chatHistory.enable));
                return 1;
            }))
            .then(literal("seconds")
                      .then(argument("seconds", integer(0, 120)).executes(c -> {
                          CONFIG.server.extra.chatHistory.seconds = getInteger(c, "seconds");
                          c.getSource().getEmbed()
                              .title("Chat History Seconds Set");
                          return 1;
                      })))
            .then(literal("maxCount")
                      .then(argument("maxCount", integer(0, 100)).executes(c -> {
                          CONFIG.server.extra.chatHistory.maxCount = getInteger(c, "maxCount");
                          MODULE.get(ChatHistory.class).syncMaxCountFromConfig();
                          c.getSource().getEmbed()
                              .title("Chat History Max Count Set");
                          return 1;
                      })))
            .then(literal("spectators")
                      .then(argument("toggle", toggle()).executes(c -> {
                          CONFIG.server.extra.chatHistory.spectators = getToggle(c, "toggle");
                          c.getSource().getEmbed()
                              .title("Chat History Spectators " + toggleStrCaps(CONFIG.server.extra.chatHistory.spectators));
                          return 1;
                      })));
    }

    @Override
    public void postPopulate(final Embed embed) {
        embed
            .addField("Chat History", toggleStr(CONFIG.server.extra.chatHistory.enable), false)
            .addField("Seconds", CONFIG.server.extra.chatHistory.seconds, false)
            .addField("Max Count", CONFIG.server.extra.chatHistory.maxCount, false)
            .addField("Spectators", toggleStr(CONFIG.server.extra.chatHistory.spectators), false)
            .color(Color.CYAN);
    }
}
