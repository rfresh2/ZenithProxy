package com.zenith.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.command.*;
import discord4j.common.util.Snowflake;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.util.MentionUtil;
import discord4j.rest.util.Color;

import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static com.zenith.Shared.*;
import static com.zenith.command.CustomStringArgumentType.getString;
import static com.zenith.command.CustomStringArgumentType.wordWithChars;
import static com.zenith.command.ToggleArgumentType.getToggle;
import static com.zenith.command.ToggleArgumentType.toggle;
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
                "token <token",
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
                c.getSource().getEmbedBuilder()
                    .title("Discord Bot " + (CONFIG.discord.enable ? "On!" : "Off!"))
                    .color(Color.CYAN);
                if (CONFIG.discord.enable) {
                    c.getSource().getEmbedBuilder()
                        .description("Discord bot will now start");
                }
                // will stop/start depending on if the bot is enabled
                SCHEDULED_EXECUTOR_SERVICE.schedule(this::restartDiscordBot, 3, TimeUnit.SECONDS);
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
                              c.getSource().getEmbedBuilder()
                                  .title("Invalid Channel ID")
                                  .description("The channel ID provided is invalid")
                                  .color(Color.RUBY);
                              return 1;
                          }
                          if (channelId.equals(CONFIG.discord.chatRelay.channelId)) {
                              c.getSource().getEmbedBuilder()
                                  .title("Invalid Channel ID")
                                  .description("Cannot use the same channel ID for both the relay and main channel")
                                  .color(Color.RUBY);
                              return 1;
                          }
                          CONFIG.discord.channelId = channelId;
                          c.getSource().getEmbedBuilder()
                                       .title("Channel set!")
                                       .color(Color.CYAN)
                                       .description("Discord bot will now restart if enabled");
                          if (DISCORD_BOT.isRunning())
                              SCHEDULED_EXECUTOR_SERVICE.schedule(this::restartDiscordBot, 3, TimeUnit.SECONDS);
                          return 1;
                      })))
            .then(literal("relaychannel")
                      .then(argument("channel ID", wordWithChars()).executes(c -> {
                          String channelId = getString(c, "channel ID");
                          if (CHANNEL_ID_PATTERN.matcher(channelId).matches())
                              channelId = channelId.substring(2, channelId.length() - 1);
                          try {
                              Snowflake.of(channelId);
                          } catch (final Exception e) {
                              // invalid id
                              c.getSource().getEmbedBuilder()
                                  .title("Invalid Channel ID")
                                  .description("The channel ID provided is invalid")
                                  .color(Color.RUBY);
                              return 1;
                          }
                          if (channelId.equals(CONFIG.discord.channelId)) {
                              c.getSource().getEmbedBuilder()
                                  .title("Invalid Channel ID")
                                  .description("Cannot use the same channel ID for both the relay and main channel")
                                  .color(Color.RUBY);
                              return 1;
                          }
                          CONFIG.discord.chatRelay.channelId = channelId;
                          c.getSource().getEmbedBuilder()
                                       .title("Relay Channel set!")
                                       .color(Color.CYAN)
                                       .description("Discord bot will now restart if enabled");
                          if (DISCORD_BOT.isRunning())
                              SCHEDULED_EXECUTOR_SERVICE.schedule(this::restartDiscordBot, 3, TimeUnit.SECONDS);
                          return 1;
                      })))
            .then(literal("token").requires(DiscordManageCommand::validateTerminalSource)
                      .then(argument("token", wordWithChars()).executes(c -> {
                          CONFIG.discord.token = getString(c, "token");
                          c.getSource().getEmbedBuilder()
                                       .title("Token set!")
                                       .color(Color.CYAN)
                                       .description("Discord bot will now restart if enabled");
                          if (DISCORD_BOT.isRunning())
                              SCHEDULED_EXECUTOR_SERVICE.schedule(this::restartDiscordBot, 3, TimeUnit.SECONDS);
                          return 1;
                      })))
            .then(literal("role").requires(DiscordManageCommand::validateTerminalSource)
                      .then(argument("roleId", wordWithChars()).executes(c -> {
                          var roleId = getString(c, "roleId");
                          try {
                              Snowflake.of(roleId);
                          } catch (final Exception e) {
                              // invalid id
                              c.getSource().getEmbedBuilder()
                                  .title("Invalid Role ID")
                                  .description("The role ID provided is invalid")
                                  .color(Color.RUBY);
                              return 1;
                          }
                          CONFIG.discord.accountOwnerRoleId = roleId;
                          c.getSource().getEmbedBuilder()
                              .title("Role set!")
                              .color(Color.CYAN);
                          return 1;
                      })))
            .then(literal("manageprofileimage")
                      .then(argument("toggle", toggle()).executes(c -> {
                            CONFIG.discord.manageProfileImage = getToggle(c, "toggle");
                            c.getSource().getEmbedBuilder()
                                         .color(Color.CYAN)
                                         .title("Manage Profile Image " + (CONFIG.discord.manageProfileImage ? "On!" : "Off!"));
                            return 1;
                      })))
            .then(literal("managenickname")
                      .then(argument("toggle", toggle()).executes(c -> {
                            CONFIG.discord.manageNickname = getToggle(c, "toggle");
                            c.getSource().getEmbedBuilder()
                                         .color(Color.CYAN)
                                         .title("Manage Nickname " + (CONFIG.discord.manageNickname ? "On!" : "Off!"));
                            return 1;
                      })))
            .then(literal("managedescription")
                      .then(argument("toggle", toggle()).executes(c -> {
                            CONFIG.discord.manageDescription = getToggle(c, "toggle");
                            c.getSource().getEmbedBuilder()
                                         .color(Color.CYAN)
                                         .title("Manage Description " + (CONFIG.discord.manageDescription ? "On!" : "Off!"));
                            return 1;
                      })))
            .then(literal("shownonwhitelistip")
                      .then(argument("toggle", toggle()).executes(c -> {
                            CONFIG.discord.showNonWhitelistLoginIP = getToggle(c, "toggle");
                            c.getSource().getEmbedBuilder()
                                         .color(Color.CYAN)
                                         .title("Show Non-Whitelist IP " + (CONFIG.discord.showNonWhitelistLoginIP ? "On!" : "Off!"));
                            return 1;
                      })));
    }

    private static boolean validateTerminalSource(CommandContext c) {
        return Command.validateCommandSource(c, CommandSource.TERMINAL);
    }

    @Override
    public void postPopulate(final EmbedCreateSpec.Builder builder) {
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
            DISCORD_BOT.stop(false);
            if (CONFIG.discord.enable) {
                DISCORD_BOT.start();
                DISCORD_BOT.sendEmbedMessage(EmbedCreateSpec.builder()
                                                 .title("Discord Bot Restarted")
                                                 .color(Color.GREEN)
                                                 .build());
            } else {
                DISCORD_LOG.info("Discord bot is disabled, not starting");
            }
        } catch (final Exception e) {
            DISCORD_LOG.error("Failed to restart discord bot", e);
        }
    }
}
