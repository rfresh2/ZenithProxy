package com.zenith.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.command.Command;
import com.zenith.command.CommandUsage;
import com.zenith.command.brigadier.CommandCategory;
import com.zenith.command.brigadier.CommandContext;
import com.zenith.command.brigadier.CommandSource;
import com.zenith.discord.Embed;
import discord4j.common.util.Snowflake;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.util.MentionUtil;
import discord4j.gateway.intent.Intent;
import discord4j.gateway.intent.IntentSet;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static com.zenith.Shared.*;
import static com.zenith.command.brigadier.CustomStringArgumentType.getString;
import static com.zenith.command.brigadier.CustomStringArgumentType.wordWithChars;
import static com.zenith.command.brigadier.ToggleArgumentType.getToggle;
import static com.zenith.command.brigadier.ToggleArgumentType.toggle;
import static java.util.Arrays.asList;

public class DiscordManageCommand extends Command {
    private static final Pattern CHANNEL_ID_PATTERN = Pattern.compile("<#\\d+>");

    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.args(
            "discord",
            CommandCategory.MANAGE,
            "Manages the discord bot configuration",
            asList(
                "on/off",
                "channel <channel ID>",
                "relayChannel <channel ID>",
                "token <token>",
                "role <role ID>",
                "manageProfileImage on/off",
                "manageNickname on/off",
                "manageDescription on/off",
                "showNonWhitelistIP on/off"
            )
        );
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("discord")
            .requires(Command::validateAccountOwner)
            .requires(c -> Command.validateCommandSource(c, asList(CommandSource.DISCORD, CommandSource.TERMINAL)))
            .then(argument("toggle", toggle()).executes(c -> {
                CONFIG.discord.enable = getToggle(c, "toggle");
                c.getSource().getEmbed()
                    .title("Discord Bot " + toggleStrCaps(CONFIG.discord.enable))
                    .primaryColor();
                if (CONFIG.discord.enable) {
                    c.getSource().getEmbed()
                        .description("Discord bot will now start");
                }
                // will stop/start depending on if the bot is enabled
                EXECUTOR.schedule(this::restartDiscordBot, 3, TimeUnit.SECONDS);
                return 1;
            }))
            .then(literal("channel")
                      .then(argument("channel ID", wordWithChars()).executes(c -> {
                          String channelId = getString(c, "channel ID");
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
                          return 1;
                      })))
            .then(literal("relayChannel")
                      .then(argument("channel ID", wordWithChars()).executes(c -> {
                          String channelId = getString(c, "channel ID");
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
                          if (channelId.equals(CONFIG.discord.channelId)) {
                              c.getSource().getEmbed()
                                  .title("Invalid Channel ID")
                                  .description("Cannot use the same channel ID for both the relay and main channel")
                                  .errorColor();
                              return 1;
                          }
                          CONFIG.discord.chatRelay.channelId = channelId;
                          c.getSource().getEmbed()
                                       .title("Relay Channel set!")
                                       .primaryColor()
                                       .description("Discord bot will now restart if enabled");
                          if (DISCORD.isRunning())
                              EXECUTOR.schedule(this::restartDiscordBot, 3, TimeUnit.SECONDS);
                          return 1;
                      })))
            .then(literal("token").requires(DiscordManageCommand::validateTerminalSource)
                      .then(argument("token", wordWithChars()).executes(c -> {
                          c.getSource().setSensitiveInput(true);
                          var token = getString(c, "token");
                          if (!validateToken(token)) {
                              c.getSource().getEmbed()
                                  .title("Invalid Token")
                                  .description("Discord API returned an error during test login")
                                  .errorColor();
                              return ERROR;
                          }
                          CONFIG.discord.token = token;
                          c.getSource().getEmbed()
                              .title("Token set!")
                              .primaryColor()
                              .description("Discord bot will now restart if enabled");
                          if (DISCORD.isRunning())
                              EXECUTOR.schedule(this::restartDiscordBot, 3, TimeUnit.SECONDS);
                          return 1;
                      })))
            .then(literal("role").requires(DiscordManageCommand::validateTerminalSource)
                      .then(argument("roleId", wordWithChars()).executes(c -> {
                          c.getSource().setSensitiveInput(true);
                          var roleId = getString(c, "roleId");
                          try {
                              Snowflake.of(roleId);
                          } catch (final Exception e) {
                              // invalid id
                              c.getSource().getEmbed()
                                  .title("Invalid Role ID")
                                  .description("The role ID provided is invalid")
                                  .errorColor();
                              return 1;
                          }
                          CONFIG.discord.accountOwnerRoleId = roleId;
                          c.getSource().getEmbed()
                              .title("Role set!")
                              .primaryColor();
                          return 1;
                      })))
            .then(literal("manageProfileImage")
                      .then(argument("toggle", toggle()).executes(c -> {
                            CONFIG.discord.manageProfileImage = getToggle(c, "toggle");
                            c.getSource().getEmbed()
                                         .primaryColor()
                                         .title("Manage Profile Image " + toggleStrCaps(CONFIG.discord.manageProfileImage));
                            return 1;
                      })))
            .then(literal("manageNickname")
                      .then(argument("toggle", toggle()).executes(c -> {
                            CONFIG.discord.manageNickname = getToggle(c, "toggle");
                            c.getSource().getEmbed()
                                         .primaryColor()
                                         .title("Manage Nickname " + toggleStrCaps(CONFIG.discord.manageNickname));
                            return 1;
                      })))
            .then(literal("manageDescription")
                      .then(argument("toggle", toggle()).executes(c -> {
                            CONFIG.discord.manageDescription = getToggle(c, "toggle");
                            c.getSource().getEmbed()
                                         .primaryColor()
                                         .title("Manage Description " + toggleStrCaps(CONFIG.discord.manageDescription));
                            return 1;
                      })))
            .then(literal("showNonWhitelistIP")
                      .then(argument("toggle", toggle()).executes(c -> {
                            CONFIG.discord.showNonWhitelistLoginIP = getToggle(c, "toggle");
                            c.getSource().getEmbed()
                                         .primaryColor()
                                         .title("Show Non-Whitelist IP " + toggleStrCaps(CONFIG.discord.showNonWhitelistLoginIP));
                            return 1;
                      })));
    }

    private static boolean validateTerminalSource(CommandContext c) {
        return Command.validateCommandSource(c, CommandSource.TERMINAL);
    }

    @Override
    public void postPopulate(final Embed builder) {
        builder
            .addField("Channel ID", getChannelMention(CONFIG.discord.channelId), false)
            .addField("Relay Channel ID", getChannelMention(CONFIG.discord.chatRelay.channelId), false)
            .addField("Manager Role ID", getRoleMention(CONFIG.discord.accountOwnerRoleId), false)
            .addField("Manage Profile Image", toggleStr(CONFIG.discord.manageProfileImage), false)
            .addField("Manage Nickname", toggleStr(CONFIG.discord.manageNickname), false)
            .addField("Manage Description", toggleStr(CONFIG.discord.manageDescription), false)
            .addField("Show Non-Whitelist IP", toggleStr(CONFIG.discord.showNonWhitelistLoginIP), false);
    }

    private String getChannelMention(final String channelId) {
        try {
            return MentionUtil.forChannel(Snowflake.of(channelId));
        } catch (final Exception e) {
            // these channels might be unset on purpose
            DEFAULT_LOG.debug("Invalid channel ID: " + channelId, e);
            return "";
        }
    }

    private String getRoleMention(final String roleId) {
        try {
            return MentionUtil.forRole(Snowflake.of(roleId));
        } catch (final NumberFormatException e) {
            DISCORD_LOG.error("Unable to generate mention for role ID: {}", roleId, e);
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

    private boolean validateToken(final String token) {
        try {
            var builder = DiscordClientBuilder.create(token)
                .build();
            builder.gateway()
                .setEnabledIntents((IntentSet.of(Intent.MESSAGE_CONTENT, Intent.GUILD_MESSAGES)));
            builder
                .login()
                .block(Duration.ofSeconds(20))
                .logout()
                .block(Duration.ofSeconds(20));
            return true;
        } catch (final Throwable e) {
            DISCORD_LOG.error("Failed validating discord token", e);
            return false;
        }
    }
}
