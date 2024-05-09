package com.zenith.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.command.Command;
import com.zenith.command.CommandUsage;
import com.zenith.command.brigadier.CommandCategory;
import com.zenith.command.brigadier.CommandContext;
import com.zenith.discord.Embed;

import java.util.Arrays;

import static com.zenith.Shared.CONFIG;
import static com.zenith.command.brigadier.ToggleArgumentType.getToggle;
import static com.zenith.command.brigadier.ToggleArgumentType.toggle;

public class ExtraChatCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.args("extraChat",
                                 CommandCategory.MODULE,
                                 """
                                 Configures the ExtraChat module.
                                 
                                 Allows hiding types of messages in-game or in the terminal chat log.
                                 """,
                                 Arrays.asList(
                                     "hideChat on/off",
                                     "hideWhispers on/off",
                                     "hideDeathMessages on/off",
                                     "showConnectionMessages on/off",
                                     "logChatMessages on/off"
                                 ));
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("extraChat")
            .then(literal("hideChat")
                      .then(argument("toggle", toggle()).executes(c -> {
                            CONFIG.client.extra.chat.hideChat = getToggle(c, "toggle");
                            c.getSource().getEmbed()
                                .title("Hide Chat " + toggleStrCaps(CONFIG.client.extra.chat.hideChat));
                            return OK;
                        })))
            .then(literal("hideWhispers")
                      .then(argument("toggle", toggle()).executes(c -> {
                            CONFIG.client.extra.chat.hideWhispers = getToggle(c, "toggle");
                            c.getSource().getEmbed()
                                .title("Hide Whispers " + toggleStrCaps(CONFIG.client.extra.chat.hideWhispers));
                            return OK;
                        })))
            .then(literal("hideDeathMessages")
                      .then(argument("toggle", toggle()).executes(c -> {
                            CONFIG.client.extra.chat.hideDeathMessages = getToggle(c, "toggle");
                            c.getSource().getEmbed()
                                .title("Hide Death Messages " + toggleStrCaps(CONFIG.client.extra.chat.hideDeathMessages));
                            return OK;
                        })))
            .then(literal("showConnectionMessages")
                      .then(argument("toggle", toggle()).executes(c -> {
                            CONFIG.client.extra.chat.showConnectionMessages = getToggle(c, "toggle");
                            c.getSource().getEmbed()
                                .title("Show Connection Messages " + toggleStrCaps(CONFIG.client.extra.chat.showConnectionMessages));
                            return OK;
                        })))
            .then(literal("logChatMessages")
                      .then(argument("toggle", toggle()).executes(c -> {
                            CONFIG.client.extra.logChatMessages = getToggle(c, "toggle");
                            c.getSource().getEmbed()
                                .title("Log Chat Messages " + toggleStrCaps(CONFIG.client.extra.logChatMessages));
                            return OK;
                        })));
    }


    @Override
    public void postPopulate(final Embed builder) {
        builder
            .addField("Hide Chat", toggleStr(CONFIG.client.extra.chat.hideChat), false)
            .addField("Hide Whispers", toggleStr(CONFIG.client.extra.chat.hideWhispers), false)
            .addField("Hide death Messages", toggleStr(CONFIG.client.extra.chat.hideDeathMessages), false)
            .addField("Show Connection Messages", toggleStr(CONFIG.client.extra.chat.showConnectionMessages), false)
            .addField("Log Chat Messages", toggleStr(CONFIG.client.extra.logChatMessages), false)
            .primaryColor();
    }
}
