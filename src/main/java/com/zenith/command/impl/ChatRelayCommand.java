package com.zenith.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.command.api.*;
import com.zenith.discord.Embed;
import com.zenith.util.MentionUtil;

import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.mojang.brigadier.arguments.StringArgumentType.greedyString;
import static com.zenith.Globals.*;
import static com.zenith.command.brigadier.CustomStringArgumentType.getString;
import static com.zenith.command.brigadier.CustomStringArgumentType.wordWithChars;
import static com.zenith.command.brigadier.ToggleArgumentType.getToggle;
import static com.zenith.command.brigadier.ToggleArgumentType.toggle;
import static java.util.Arrays.asList;

public class ChatRelayCommand extends Command {
    private static final Pattern CHANNEL_ID_PATTERN = Pattern.compile("<#\\d+>");

    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.builder()
            .name("chatRelay")
            .category(CommandCategory.MANAGE)
            .description("""
            Configures the Discord ChatRelay feature.

            The ChatRelay is a live feed of chat messages and/or connection messages from the server to a Discord channel.

            Mentions can be configured when a whisper is received or your name is seen in chat.

            Messages typed in the ChatRelay discord channel will be sent as chat messages in-game
            Discord message replies will be sent as whispers in-game.

            Ignore regex will filter out messages, see here for help writing regex: https://regex101.com/ (Java flavor)
            """)
            .usageLines(
                "on/off",
                "channel <channelId>",
                "connectionMessages on/off",
                "whispers on/off",
                "publicChat on/off",
                "deathMessages on/off",
                "serverMessages on/off",
                "whisperMentions on/off",
                "nameMentions on/off",
                "mentionsWhileConnected on/off",
                "ignoreQueue on/off",
                "sendMessages on/off",
                "ignoreRegex add <regex>",
                "ignoreRegex del <index>",
                "ignoreRegex list",
                "ignoreRegex clear"
            )
            .build();
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("chatRelay")
            .requires(c -> Command.validateCommandSource(c, asList(CommandSources.DISCORD, CommandSources.TERMINAL)))
            .then(argument("toggle", toggle()).executes(c -> {
                CONFIG.discord.chatRelay.enable = getToggle(c, "toggle");
                if (CONFIG.discord.chatRelay.enable && CONFIG.discord.chatRelay.channelId.isEmpty()) {
                    c.getSource().getEmbed()
                        .title("Error")
                        .description("Chat Relay channel must be set: `chatRelay channel <channelId>`")
                        .errorColor();
                    CONFIG.discord.chatRelay.enable = false;
                    return OK;
                }
                EXECUTOR.execute(this::restartDiscordBot);
                c.getSource().getEmbed()
                    .title("Chat Relay " + toggleStrCaps(CONFIG.discord.chatRelay.enable));
                return OK;
            }))
            .then(literal("channel").requires(Command::validateAccountOwner).then(argument("channelId", wordWithChars()).executes(c -> {
                String channelId = getString(c, "channelId");
                if (CHANNEL_ID_PATTERN.matcher(channelId).matches())
                    channelId = channelId.substring(2, channelId.length() - 1);
                try {
                    Long.parseUnsignedLong(channelId);
                } catch (final Exception e) {
                    // invalid id
                    c.getSource().getEmbed()
                        .title("Invalid Channel ID")
                        .description("The channel ID provided is invalid")
                        .errorColor();
                    return OK;
                }
                if (channelId.equals(CONFIG.discord.channelId)) {
                    c.getSource().getEmbed()
                        .title("Invalid Channel ID")
                        .description("Cannot use the same channel ID for both the relay and main channel")
                        .errorColor();
                    return OK;
                }
                CONFIG.discord.chatRelay.channelId = channelId;
                c.getSource().getEmbed()
                    .title("Channel set!")
                    .primaryColor()
                    .description("Discord bot will now restart if enabled");
                if (DISCORD.isRunning())
                    EXECUTOR.schedule(this::restartDiscordBot, 3, TimeUnit.SECONDS);
                return OK;
            })))
            .then(literal("connectionMessages")
                .then(argument("toggle", toggle()).executes(c -> {
                    CONFIG.discord.chatRelay.connectionMessages = getToggle(c, "toggle");
                    c.getSource().getEmbed()
                        .title("Connection Messages " + toggleStrCaps(CONFIG.discord.chatRelay.connectionMessages));
                })))
            .then(literal("whispers")
                .then(argument("toggle", toggle()).executes(c -> {
                    CONFIG.discord.chatRelay.whispers = getToggle(c, "toggle");
                    c.getSource().getEmbed()
                        .title("Whispers " + toggleStrCaps(CONFIG.discord.chatRelay.whispers));
                })))
            .then(literal("publicChat")
                .then(argument("toggle", toggle()).executes(c -> {
                    CONFIG.discord.chatRelay.publicChats = getToggle(c, "toggle");
                    c.getSource().getEmbed()
                        .title("Public Chat " + toggleStrCaps(CONFIG.discord.chatRelay.publicChats));
                })))
            .then(literal("deathMessages")
                .then(argument("toggle", toggle()).executes(c -> {
                    CONFIG.discord.chatRelay.deathMessages = getToggle(c, "toggle");
                    c.getSource().getEmbed()
                        .title("Death Messages " + toggleStrCaps(CONFIG.discord.chatRelay.deathMessages));
                })))
            .then(literal("serverMessages")
                .then(argument("toggle", toggle()).executes(c -> {
                    CONFIG.discord.chatRelay.serverMessages = getToggle(c, "toggle");
                    c.getSource().getEmbed()
                        .title("Server Messages " + toggleStrCaps(CONFIG.discord.chatRelay.serverMessages));
                })))
            .then(literal("whisperMentions")
                .then(argument("toggle", toggle()).executes(c -> {
                    CONFIG.discord.chatRelay.mentionRoleOnWhisper = getToggle(c, "toggle");
                    c.getSource().getEmbed()
                        .title("Whisper Mentions " + toggleStrCaps(CONFIG.discord.chatRelay.mentionRoleOnWhisper));
                })))
            .then(literal("nameMentions")
                .then(argument("toggle", toggle()).executes(c -> {
                    CONFIG.discord.chatRelay.mentionRoleOnNameMention = getToggle(c, "toggle");
                    c.getSource().getEmbed()
                        .title("Name Mentions " + toggleStrCaps(CONFIG.discord.chatRelay.mentionRoleOnNameMention));
                })))
            .then(literal("mentionsWhileConnected")
                .then(argument("toggle", toggle()).executes(c -> {
                    CONFIG.discord.chatRelay.mentionWhileConnected = getToggle(c, "toggle");
                    c.getSource().getEmbed()
                        .title("Mentions While Connected " + toggleStrCaps(CONFIG.discord.chatRelay.mentionWhileConnected));
                })))
            .then(literal("ignoreQueue")
                .then(argument("toggle", toggle()).executes(c -> {
                    CONFIG.discord.chatRelay.ignoreQueue = getToggle(c, "toggle");
                    c.getSource().getEmbed()
                        .title("Ignore Queue " + toggleStrCaps(CONFIG.discord.chatRelay.ignoreQueue));
                })))
            .then(literal("sendMessages")
                .then(argument("toggle", toggle()).executes(c -> {
                    CONFIG.discord.chatRelay.sendMessages = getToggle(c, "toggle");
                    c.getSource().getEmbed()
                        .title("Send Messages " + toggleStrCaps(CONFIG.discord.chatRelay.sendMessages));
                })))
            .then(literal("ignoreRegex")
                .then(literal("add").then(argument("regex", greedyString()).executes(c -> {
                    c.getSource().getData().put("noDefaultEmbed", true);
                    String regexInput = getString(c, "regex");
                    try {
                        Pattern.compile(regexInput);
                    } catch (Exception e) {
                        c.getSource().getEmbed()
                            .title("Invalid Regex")
                            .description(e.toString());
                        return ERROR;
                    }
                    CONFIG.discord.chatRelay.ignoreRegex.add(regexInput);
                    c.getSource().getEmbed()
                        .title("Regex Added");
                    return OK;
                })))
                .then(literal("del").then(argument("index", integer(0)).executes(c -> {
                    c.getSource().getData().put("noDefaultEmbed", true);
                    int index = getInteger(c, "index");
                    if (index < 0 || index >= CONFIG.discord.chatRelay.ignoreRegex.size()) {
                        c.getSource().getEmbed()
                            .title("Invalid Index")
                            .description("Index out of bounds");
                        return ERROR;
                    }
                    CONFIG.discord.chatRelay.ignoreRegex.remove(index);
                    c.getSource().getEmbed()
                        .title("Regex Removed");
                    return OK;
                })))
                .then(literal("clear").executes(c -> {
                    c.getSource().getData().put("noDefaultEmbed", true);
                    CONFIG.discord.chatRelay.ignoreRegex.clear();
                    c.getSource().getEmbed()
                        .title("Ignore Regex Cleared");
                }))
                .then(literal("list").executes(c -> {
                    c.getSource().getData().put("noDefaultEmbed", true);
                    var sb = new StringBuilder();
//                    sb.append("**Ignore Regex List**\n\n");
                    for (int i = 0; i < CONFIG.discord.chatRelay.ignoreRegex.size(); i++) {
                        var regex = CONFIG.discord.chatRelay.ignoreRegex.get(i);
                        sb.append("**")
                            .append(i)
                            .append("**: `")
                            .append(regex)
                            .append("`\n");
                    }
                    c.getSource().getEmbed()
                        .title("Ignore Regex List")
                        .description(sb.toString());
                })));
    }

    @Override
    public void defaultHandler(final CommandContext c) {
        if (!c.getData().containsKey("noDefaultEmbed")) {
            c.getEmbed()
                .addField("Chat Relay", toggleStr(CONFIG.discord.chatRelay.enable))
                .addField("Channel", getChannelMention(CONFIG.discord.chatRelay.channelId))
                .addField("Connection Messages", toggleStr(CONFIG.discord.chatRelay.connectionMessages))
                .addField("Public Chats", toggleStr(CONFIG.discord.chatRelay.publicChats))
                .addField("Whispers", toggleStr(CONFIG.discord.chatRelay.whispers))
                .addField("Death Messages", toggleStr(CONFIG.discord.chatRelay.deathMessages))
                .addField("Server Messages", toggleStr(CONFIG.discord.chatRelay.serverMessages))
                .addField("Whisper Mentions", toggleStr(CONFIG.discord.chatRelay.mentionRoleOnWhisper))
                .addField("Name Mentions", toggleStr(CONFIG.discord.chatRelay.mentionRoleOnNameMention))
                .addField("Mentions While Connected", toggleStr(CONFIG.discord.chatRelay.mentionWhileConnected))
                .addField("Ignore Queue", toggleStr(CONFIG.discord.chatRelay.ignoreQueue))
                .addField("Send Messages", toggleStr(CONFIG.discord.chatRelay.sendMessages));
        }
        c.getEmbed()
            .primaryColor();
    }

    private String getChannelMention(final String channelId) {
        try {
            return MentionUtil.forChannel(channelId);
        } catch (final Exception e) {
            // these channels might be unset on purpose
            DEFAULT_LOG.debug("Invalid channel ID: {}", channelId, e);
            return "";
        }
    }

    private void restartDiscordBot() {
        DISCORD_LOG.info("Restarting discord bot");
        try {
            DISCORD.stop(false);
            if (CONFIG.discord.enable) {
                DISCORD.start();
                DISCORD.sendEmbedMessage(Embed.builder()
                    .title("Discord Bot Restarted")
                    .successColor());
            } else {
                DISCORD_LOG.info("Discord bot is disabled, not starting");
            }
        } catch (final Exception e) {
            DISCORD_LOG.error("Failed to restart discord bot", e);
        }
    }
}
