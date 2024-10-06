package com.zenith.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.command.Command;
import com.zenith.command.CommandUsage;
import com.zenith.command.brigadier.CommandCategory;
import com.zenith.command.brigadier.CommandContext;
import com.zenith.command.brigadier.CommandSource;
import com.zenith.discord.Embed;
import discord4j.common.util.Snowflake;
import discord4j.core.util.MentionUtil;

import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static com.zenith.Shared.*;
import static com.zenith.command.brigadier.CustomStringArgumentType.getString;
import static com.zenith.command.brigadier.CustomStringArgumentType.wordWithChars;
import static com.zenith.command.brigadier.ToggleArgumentType.getToggle;
import static com.zenith.command.brigadier.ToggleArgumentType.toggle;
import static java.util.Arrays.asList;

public class ChatRelayCommand extends Command {
    private static final Pattern CHANNEL_ID_PATTERN = Pattern.compile("<#\\d+>");

    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.args(
            "chatRelay",
            CommandCategory.MANAGE,
            """
            Configures the Discord ChatRelay feature.
            
            The ChatRelay is a live feed of chat messages and/or connection messages from the server to a Discord channel.
            
            Mentions can be configured when a whisper is received or your name is seen in chat.
            
            Messages typed in the ChatRelay discord channel will be sent as chat messages in-game
            Discord message replies will be sent as whispers in-game
            """,
            asList(
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
                "sendMessages on/off"
            )
        );
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("chatRelay")
            .requires(c -> Command.validateCommandSource(c, asList(CommandSource.DISCORD, CommandSource.TERMINAL)))
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
                    Snowflake.of(channelId);
                } catch (final Exception e) {
                    // invalid id
                    c.getSource().getEmbed()
                        .title("Invalid Channel ID")
                        .description("The channel ID provided is invalid")
                        .errorColor();
                    return 1;
                }
                if (channelId.equals(CONFIG.discord.chatRelay.channelId)) {
                    c.getSource().getEmbed()
                        .title("Invalid Channel ID")
                        .description("Cannot use the same channel ID for both the relay and main channel")
                        .errorColor();
                    return 1;
                }
                CONFIG.discord.channelId = channelId;
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
                            return 1;
                      })))
            .then(literal("whispers")
                      .then(argument("toggle", toggle()).executes(c -> {
                            CONFIG.discord.chatRelay.whispers = getToggle(c, "toggle");
                            c.getSource().getEmbed()
                                .title("Whispers " + toggleStrCaps(CONFIG.discord.chatRelay.whispers));
                            return 1;
                      })))
            .then(literal("publicChat")
                      .then(argument("toggle", toggle()).executes(c -> {
                            CONFIG.discord.chatRelay.publicChats = getToggle(c, "toggle");
                            c.getSource().getEmbed()
                                .title("Public Chat " + toggleStrCaps(CONFIG.discord.chatRelay.publicChats));
                            return 1;
                      })))
            .then(literal("deathMessages")
                      .then(argument("toggle", toggle()).executes(c -> {
                          CONFIG.discord.chatRelay.deathMessages = getToggle(c, "toggle");
                          c.getSource().getEmbed()
                              .title("Death Messages " + toggleStrCaps(CONFIG.discord.chatRelay.deathMessages));
                          return 1;
                      })))
            .then(literal("serverMessages")
                      .then(argument("toggle", toggle()).executes(c -> {
                          CONFIG.discord.chatRelay.serverMessages = getToggle(c, "toggle");
                          c.getSource().getEmbed()
                              .title("Server Messages " + toggleStrCaps(CONFIG.discord.chatRelay.serverMessages));
                          return 1;
                      })))
            .then(literal("whisperMentions")
                      .then(argument("toggle", toggle()).executes(c -> {
                            CONFIG.discord.chatRelay.mentionRoleOnWhisper = getToggle(c, "toggle");
                            c.getSource().getEmbed()
                                .title("Whisper Mentions " + toggleStrCaps(CONFIG.discord.chatRelay.mentionRoleOnWhisper));
                            return 1;
                      })))
            .then(literal("nameMentions")
                      .then(argument("toggle", toggle()).executes(c -> {
                            CONFIG.discord.chatRelay.mentionRoleOnNameMention = getToggle(c, "toggle");
                            c.getSource().getEmbed()
                                .title("Name Mentions " + toggleStrCaps(CONFIG.discord.chatRelay.mentionRoleOnNameMention));
                            return 1;
                      })))
            .then(literal("mentionsWhileConnected")
                      .then(argument("toggle", toggle()).executes(c -> {
                            CONFIG.discord.chatRelay.mentionWhileConnected = getToggle(c, "toggle");
                            c.getSource().getEmbed()
                                .title("Mentions While Connected " + toggleStrCaps(CONFIG.discord.chatRelay.mentionWhileConnected));
                            return 1;
                      })))
            .then(literal("ignoreQueue")
                      .then(argument("toggle", toggle()).executes(c -> {
                            CONFIG.discord.chatRelay.ignoreQueue = getToggle(c, "toggle");
                            c.getSource().getEmbed()
                                .title("Ignore Queue " + toggleStrCaps(CONFIG.discord.chatRelay.ignoreQueue));
                            return 1;
                      })))
            .then(literal("sendMessages")
                      .then(argument("toggle", toggle()).executes(c -> {
                            CONFIG.discord.chatRelay.sendMessages = getToggle(c, "toggle");
                            c.getSource().getEmbed()
                                .title("Send Messages " + toggleStrCaps(CONFIG.discord.chatRelay.sendMessages));
                            return 1;
                      })));
    }

    @Override
    public void postPopulate(final Embed builder) {
        builder
            .addField("Chat Relay", toggleStr(CONFIG.discord.chatRelay.enable), false)
            .addField("Channel", getChannelMention(CONFIG.discord.chatRelay.channelId), false)
            .addField("Connection Messages", toggleStr(CONFIG.discord.chatRelay.connectionMessages), false)
            .addField("Public Chats", toggleStr(CONFIG.discord.chatRelay.publicChats), false)
            .addField("Whispers", toggleStr(CONFIG.discord.chatRelay.whispers), false)
            .addField("Death Messages", toggleStr(CONFIG.discord.chatRelay.deathMessages), false)
            .addField("Server Messages", toggleStr(CONFIG.discord.chatRelay.serverMessages), false)
            .addField("Whisper Mentions", toggleStr(CONFIG.discord.chatRelay.mentionRoleOnWhisper), false)
            .addField("Name Mentions", toggleStr(CONFIG.discord.chatRelay.mentionRoleOnNameMention), false)
            .addField("Mentions While Connected", toggleStr(CONFIG.discord.chatRelay.mentionWhileConnected), false)
            .addField("Ignore Queue", toggleStr(CONFIG.discord.chatRelay.ignoreQueue), false)
            .addField("Send Messages", toggleStr(CONFIG.discord.chatRelay.sendMessages), false)
            .primaryColor();
    }

    private String getChannelMention(final String channelId) {
        try {
            return MentionUtil.forChannel(Snowflake.of(channelId));
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
