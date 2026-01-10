package com.zenith.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.command.api.Command;
import com.zenith.command.api.CommandCategory;
import com.zenith.command.api.CommandContext;
import com.zenith.command.api.CommandUsage;
import com.zenith.discord.Embed;
import com.zenith.module.impl.ExtraChat;

import static com.zenith.Globals.CONFIG;
import static com.zenith.Globals.MODULE;
import static com.zenith.command.brigadier.CustomStringArgumentType.getString;
import static com.zenith.command.brigadier.CustomStringArgumentType.wordWithChars;
import static com.zenith.command.brigadier.ToggleArgumentType.getToggle;
import static com.zenith.command.brigadier.ToggleArgumentType.toggle;

public class ExtraChatCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.builder()
            .name("extraChat")
            .category(CommandCategory.MODULE)
            .description("""
                 Extra chat features and modifications.
                 """)
            .usageLines(
                "on/off",
                "hideChat on/off",
                "hideWhispers on/off",
                "hideDeathMessages on/off",
                "showConnectionMessages on/off",
                "insertClickableLinks on/off",
                "hide2b2tActionBarText on/off",
                "whisperCommand <command>",
                "replace2b2tChatCommands on/off",
                "prefix on/off",
                "prefix set <prefix>",
                "suffix on/off",
                "suffix set <suffix>",
                "suffix random on/off"
            )
            .build();
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("extraChat")
            .then(argument("toggle", toggle()).executes(c -> {
                CONFIG.client.extra.chat.enabled = getToggle(c, "toggle");
                MODULE.get(ExtraChat.class).syncEnabledFromConfig();
                c.getSource().getEmbed().title("ExtraChat " + toggleStrCaps(CONFIG.client.extra.chat.enabled));
            }))
            .then(literal("hideChat").then(argument("toggle", toggle()).executes(c -> {
                CONFIG.client.extra.chat.hideChat = getToggle(c, "toggle");
                c.getSource().getEmbed()
                    .title("Hide Chat " + toggleStrCaps(CONFIG.client.extra.chat.hideChat));
            })))
            .then(literal("hideWhispers").then(argument("toggle", toggle()).executes(c -> {
                CONFIG.client.extra.chat.hideWhispers = getToggle(c, "toggle");
                c.getSource().getEmbed()
                    .title("Hide Whispers " + toggleStrCaps(CONFIG.client.extra.chat.hideWhispers));
            })))
            .then(literal("hideDeathMessages").then(argument("toggle", toggle()).executes(c -> {
                CONFIG.client.extra.chat.hideDeathMessages = getToggle(c, "toggle");
                c.getSource().getEmbed()
                    .title("Hide Death Messages " + toggleStrCaps(CONFIG.client.extra.chat.hideDeathMessages));
            })))
            .then(literal("showConnectionMessages").then(argument("toggle", toggle()).executes(c -> {
                CONFIG.client.extra.chat.showConnectionMessages = getToggle(c, "toggle");
                c.getSource().getEmbed()
                    .title("Show Connection Messages " + toggleStrCaps(CONFIG.client.extra.chat.showConnectionMessages));
            })))
            .then(literal("insertClickableLinks").then(argument("toggle", toggle()).executes(c -> {
                CONFIG.client.extra.chat.insertClickableLinks = getToggle(c, "toggle");
                c.getSource().getEmbed()
                    .title("Insert Clickable Links " + toggleStrCaps(CONFIG.client.extra.chat.insertClickableLinks));
            })))
            .then(literal("hide2b2tActionBarText").then(argument("toggle", toggle()).executes(c -> {
                CONFIG.client.extra.chat.hide2b2tActionBarText = getToggle(c, "toggle");
                c.getSource().getEmbed()
                    .title("Hide 2b2t Action Bar Text " + toggleStrCaps(CONFIG.client.extra.chat.hide2b2tActionBarText));
            })))
            .then(literal("logChatMessages").then(argument("toggle", toggle()).executes(c -> {
                CONFIG.client.extra.logChatMessages = getToggle(c, "toggle");
                c.getSource().getEmbed()
                    .title("Log Chat Messages " + toggleStrCaps(CONFIG.client.extra.logChatMessages));
            })))
            .then(literal("logOnlyQueuePositionUpdates").then(argument("toggle", toggle()).executes(c -> {
                CONFIG.client.extra.logOnlyQueuePositionUpdates = getToggle(c, "toggle");
                c.getSource().getEmbed()
                    .title("Log Only Queue Pos Updates " + toggleStrCaps(CONFIG.client.extra.logOnlyQueuePositionUpdates));
            })))
            .then(literal("whisperCommand").then(argument("cmd", wordWithChars()).executes(c -> {
                String cmd = getString(c, "cmd");
                if (cmd.isBlank()) {
                    c.getSource().getEmbed()
                        .title("Invalid Whisper Command");
                    return ERROR;
                }
                CONFIG.client.extra.whisperCommand = cmd;
                c.getSource().getEmbed()
                    .title("Whisper Command Set");
                return OK;
            })))
            .then(literal("replace2b2tChatCommands").then(argument("toggle", toggle()).executes(c -> {
                CONFIG.client.extra.chat.replace2b2tChatCommands = getToggle(c, "toggle");
                c.getSource().getEmbed()
                    .title("Replace 2b2t Chat Commands " + toggleStrCaps(CONFIG.client.extra.chat.replace2b2tChatCommands));
            })))
            .then(literal("ignoreReplace2b2tChatCommandWhileDatabaseOn").then(argument("toggle", toggle()).executes(c -> {
                CONFIG.client.extra.chat.ignoreReplace2b2tChatCommandWhileDatabaseOn = getToggle(c, "toggle");
                c.getSource().getEmbed()
                    .title("Ignore Replace 2b2t Chat Commands While Database On " + toggleStrCaps(CONFIG.client.extra.chat.ignoreReplace2b2tChatCommandWhileDatabaseOn));
            })))
            .then(literal("prefix")
                .then(argument("toggle", toggle()).executes(c -> {
                    CONFIG.client.extra.chat.prefixChats = getToggle(c, "toggle");
                    c.getSource().getEmbed()
                        .title("Prefix Chats " + toggleStrCaps(CONFIG.client.extra.chat.prefixChats));
                }))
                .then(literal("set").then(argument("prefix", wordWithChars()).executes(c -> {
                    CONFIG.client.extra.chat.prefix = getString(c, "prefix");
                    c.getSource().getEmbed()
                        .title("Prefix Set");
                }))))
            .then(literal("suffix")
                .then(argument("toggle", toggle()).executes(c -> {
                    CONFIG.client.extra.chat.suffixChats = getToggle(c, "toggle");
                    c.getSource().getEmbed()
                        .title("Suffix Chats " + toggleStrCaps(CONFIG.client.extra.chat.suffixChats));
                }))
                .then(literal("set").then(argument("suffix", wordWithChars()).executes(c -> {
                    CONFIG.client.extra.chat.suffix = getString(c, "suffix");
                    c.getSource().getEmbed()
                        .title("Suffix Set");
                })))
                .then(literal("random").then(argument("toggle", toggle()).executes(c -> {
                    CONFIG.client.extra.chat.randomSuffix = getToggle(c, "toggle");
                    c.getSource().getEmbed()
                        .title("Random Suffix " + toggleStrCaps(CONFIG.client.extra.chat.randomSuffix));
                }))));
    }


    @Override
    public void defaultEmbed(final Embed builder) {
        builder
            .addField("ExtraChat", toggleStr(CONFIG.client.extra.chat.enabled))
            .addField("Hide Chat", toggleStr(CONFIG.client.extra.chat.hideChat))
            .addField("Hide Whispers", toggleStr(CONFIG.client.extra.chat.hideWhispers))
            .addField("Hide death Messages", toggleStr(CONFIG.client.extra.chat.hideDeathMessages))
            .addField("Show Connection Messages", toggleStr(CONFIG.client.extra.chat.showConnectionMessages))
            .addField("Insert Clickable Links", toggleStr(CONFIG.client.extra.chat.insertClickableLinks))
            .addField("Hide 2b2t Action Bar Text", toggleStr(CONFIG.client.extra.chat.hide2b2tActionBarText))
            .addField("Whisper Command", CONFIG.client.extra.whisperCommand)
            .addField("Replace 2b2t Chat Commands", toggleStr(CONFIG.client.extra.chat.replace2b2tChatCommands))
            .addField("Prefix", toggleStr(CONFIG.client.extra.chat.prefixChats) + (CONFIG.client.extra.chat.prefixChats ? " - `" + CONFIG.client.extra.chat.prefix + "`" : ""))
            .addField("Suffix", toggleStr(CONFIG.client.extra.chat.suffixChats) + (CONFIG.client.extra.chat.suffixChats ? CONFIG.client.extra.chat.randomSuffix ? " - random" : " - `" + CONFIG.client.extra.chat.suffix + "`" : ""))
            .primaryColor();
    }
}
