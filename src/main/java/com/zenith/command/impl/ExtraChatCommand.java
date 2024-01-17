package com.zenith.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.command.Command;
import com.zenith.command.CommandCategory;
import com.zenith.command.CommandContext;
import com.zenith.command.CommandUsage;
import com.zenith.discord.Embed;
import discord4j.rest.util.Color;

import java.util.Arrays;

import static com.zenith.Shared.CONFIG;
import static com.zenith.command.ToggleArgumentType.getToggle;
import static com.zenith.command.ToggleArgumentType.toggle;

public class ExtraChatCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.args("extraChat",
                                 CommandCategory.MODULE,
                                 "Extra chat commands",
                                 Arrays.asList(
                                     "hideChat on/off",
                                     "hideWhispers on/off",
                                     "hideDeathMessages on/off",
                                     "showConnectionMessages on/off"
                                 ));
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("extrachat")
            .then(literal("hidechat")
                      .then(argument("toggle", toggle()).executes(c -> {
                            CONFIG.client.extra.chat.hideChat = getToggle(c, "toggle");
                            c.getSource().getEmbed()
                                .title("Chat " + (CONFIG.client.extra.chat.hideChat ? "hidden!" : "shown!"));
                            return 1;
                        })))
            .then(literal("hidewhispers")
                      .then(argument("toggle", toggle()).executes(c -> {
                            CONFIG.client.extra.chat.hideWhispers = getToggle(c, "toggle");
                            c.getSource().getEmbed()
                                .title("Whispers " + (CONFIG.client.extra.chat.hideWhispers ? "hidden!" : "shown!"));
                            return 1;
                        })))
            .then(literal("hidedeathmessages")
                      .then(argument("toggle", toggle()).executes(c -> {
                            CONFIG.client.extra.chat.hideDeathMessages = getToggle(c, "toggle");
                            c.getSource().getEmbed()
                                .title("Death messages " + (CONFIG.client.extra.chat.hideDeathMessages ? "hidden!" : "shown!"));
                            return 1;
                        })))
            .then(literal("showconnectionmessages")
                      .then(argument("toggle", toggle()).executes(c -> {
                            CONFIG.client.extra.chat.showConnectionMessages = getToggle(c, "toggle");
                            c.getSource().getEmbed()
                                .title("Connection messages " + (CONFIG.client.extra.chat.showConnectionMessages ? "shown!" : "hidden!"));
                            return 1;
                        })));
    }


    @Override
    public void postPopulate(final Embed builder) {
        builder
            .addField("Hide chat", CONFIG.client.extra.chat.hideChat ? "on" : "off", true)
            .addField("Hide whispers", CONFIG.client.extra.chat.hideWhispers ? "on" : "off", true)
            .addField("Hide death messages", CONFIG.client.extra.chat.hideDeathMessages ? "on" : "off", true)
            .addField("Connection messages", CONFIG.client.extra.chat.showConnectionMessages ? "on" : "off", true)
            .color(Color.CYAN);
    }
}
