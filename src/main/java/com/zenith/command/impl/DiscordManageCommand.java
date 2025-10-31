package com.zenith.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.command.api.*;
import com.zenith.discord.Embed;
import com.zenith.util.MentionUtil;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ShutdownException;
import net.dv8tion.jda.internal.utils.ShutdownReason;

import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static com.zenith.Globals.*;
import static com.zenith.command.brigadier.CustomStringArgumentType.getString;
import static com.zenith.command.brigadier.CustomStringArgumentType.wordWithChars;
import static com.zenith.command.brigadier.ToggleArgumentType.getToggle;
import static com.zenith.command.brigadier.ToggleArgumentType.toggle;
import static com.zenith.discord.DiscordBot.escape;
import static java.util.Arrays.asList;

public class DiscordManageCommand extends Command {
    private static final Pattern CHANNEL_ID_PATTERN = Pattern.compile("<#\\d+>");

    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.builder()
            .name("discord")
            .category(CommandCategory.MANAGE)
            .description("""
            Manages the Discord bot's configuration.

            The relay is configured using the `chatRelay` command
            """)
            .usageLines(
                "on/off",
                "channel <channel ID>",
                "token <token>",
                "role <role ID>",
                "relayChannel <channelId>",
                "manageProfileImage on/off",
                "manageNickname on/off",
                "manageDescription on/off",
                "managePresence on/off",
                "showNonWhitelistIP on/off",
                "ignoreOtherBots on/off"
            )
            .build();
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("discord")
            .requires(Command::validateAccountOwner)
            .requires(c -> Command.validateCommandSource(c, asList(CommandSources.DISCORD, CommandSources.TERMINAL)))
            .then(argument("toggle", toggle()).executes(c -> {
                CONFIG.discord.enable = getToggle(c, "toggle");
                c.getSource().getEmbed()
                    .title("Discord Bot " + toggleStrCaps(CONFIG.discord.enable));
                if (CONFIG.discord.enable) {
                    c.getSource().getEmbed()
                        .description("Discord bot will now start");
                }
                // will stop/start depending on if the bot is enabled
                EXECUTOR.schedule(this::restartDiscordBot, 3, TimeUnit.SECONDS);
            }))
            .then(literal("channel").then(argument("channel ID", wordWithChars()).executes(c -> {
                String channelId = getString(c, "channel ID");
                if (CHANNEL_ID_PATTERN.matcher(channelId).matches())
                    channelId = channelId.substring(2, channelId.length() - 1);
                try {
                    Long.parseUnsignedLong(channelId);
                } catch (final Exception e) {
                    // invalid id
                    c.getSource().getEmbed()
                        .title("Invalid Channel ID")
                        .description("The channel ID provided is invalid");
                    return OK;
                }
                if (channelId.equals(CONFIG.discord.chatRelay.channelId)) {
                    c.getSource().getEmbed()
                        .title("Invalid Channel ID")
                        .description("Cannot use the same channel ID for both the relay and main channel");
                    return OK;
                }
                CONFIG.discord.channelId = channelId;
                c.getSource().getEmbed()
                    .title("Channel set!")
                    .description("Discord bot will now restart if enabled");
                if (DISCORD.isRunning())
                    EXECUTOR.schedule(this::restartDiscordBot, 3, TimeUnit.SECONDS);
                return OK;
            })))
            .then(literal("relayChannel").then(argument("channelId", wordWithChars()).executes(c -> {
                String channelId = getString(c, "channelId");
                if (CHANNEL_ID_PATTERN.matcher(channelId).matches())
                    channelId = channelId.substring(2, channelId.length() - 1);
                try {
                    Long.parseUnsignedLong(channelId);
                } catch (final Exception e) {
                    // invalid id
                    c.getSource().getEmbed()
                        .title("Invalid Channel ID")
                        .description("The channel ID provided is invalid");
                    return OK;
                }
                if (channelId.equals(CONFIG.discord.channelId)) {
                    c.getSource().getEmbed()
                        .title("Invalid Channel ID")
                        .description("Cannot use the same channel ID for both the relay and main channel");
                    return OK;
                }
                CONFIG.discord.chatRelay.channelId = channelId;
                c.getSource().getEmbed()
                    .title("Relay Channel set!")
                    .description("Discord bot will now restart if enabled");
                if (DISCORD.isRunning())
                    EXECUTOR.schedule(this::restartDiscordBot, 3, TimeUnit.SECONDS);
                return OK;
            })))
            .then(literal("token").requires(DiscordManageCommand::validateTerminalSource).then(argument("token", wordWithChars()).executes(c -> {
                c.getSource().setSensitiveInput(true);
                var token = getString(c, "token");
                var result = validateToken(token);
                if (!result.success()) {
                    c.getSource().getEmbed()
                        .title("Invalid Token")
                        .description("Discord API returned an error during test login\n\n" + escape(result.error()));
                    return ERROR;
                }
                CONFIG.discord.token = token;
                c.getSource().getEmbed()
                    .title("Token set!")
                    .description("Discord bot will now restart if enabled");
                if (DISCORD.isRunning())
                    EXECUTOR.schedule(this::restartDiscordBot, 3, TimeUnit.SECONDS);
                return OK;
            })))
            .then(literal("role").requires(DiscordManageCommand::validateTerminalSource).then(argument("roleId", wordWithChars()).executes(c -> {
                c.getSource().setSensitiveInput(true);
                var roleId = getString(c, "roleId");
                try {
                    Long.parseUnsignedLong(roleId);
                } catch (final Exception e) {
                    // invalid id
                    c.getSource().getEmbed()
                        .title("Invalid Role ID")
                        .description("The role ID provided is invalid");
                    return OK;
                }
                CONFIG.discord.accountOwnerRoleId = roleId;
                c.getSource().getEmbed()
                    .title("Role set!");
                return OK;
            })))
            .then(literal("manageProfileImage").then(argument("toggle", toggle()).executes(c -> {
                CONFIG.discord.manageProfileImage = getToggle(c, "toggle");
                c.getSource().getEmbed()
                    .title("Manage Profile Image " + toggleStrCaps(CONFIG.discord.manageProfileImage));
            })))
            .then(literal("manageNickname").then(argument("toggle", toggle()).executes(c -> {
                CONFIG.discord.manageNickname = getToggle(c, "toggle");
                c.getSource().getEmbed()
                    .title("Manage Nickname " + toggleStrCaps(CONFIG.discord.manageNickname));
            })))
            .then(literal("manageDescription").then(argument("toggle", toggle()).executes(c -> {
                CONFIG.discord.manageDescription = getToggle(c, "toggle");
                c.getSource().getEmbed()
                    .title("Manage Description " + toggleStrCaps(CONFIG.discord.manageDescription));
            })))
            .then(literal("managePresence").then(argument("toggle", toggle()).executes(c -> {
                CONFIG.discord.managePresence = getToggle(c, "toggle");
                c.getSource().getEmbed()
                    .title("Manage Presence " + toggleStrCaps(CONFIG.discord.managePresence));
            })))
            .then(literal("showNonWhitelistIP").then(argument("toggle", toggle()).executes(c -> {
                CONFIG.discord.showNonWhitelistLoginIP = getToggle(c, "toggle");
                c.getSource().getEmbed()
                    .title("Show Non-Whitelist IP " + toggleStrCaps(CONFIG.discord.showNonWhitelistLoginIP));
            })))
            .then(literal("ignoreOtherBots").then(argument("toggle", toggle()).executes(c -> {
                CONFIG.discord.ignoreOtherBots = getToggle(c, "toggle");
                c.getSource().getEmbed()
                    .title("Ignore Other Bots " + toggleStrCaps(CONFIG.discord.ignoreOtherBots));
            })));
    }

    private static boolean validateTerminalSource(CommandContext c) {
        return Command.validateCommandSource(c, CommandSources.TERMINAL);
    }

    @Override
    public void defaultEmbed(final Embed builder) {
        builder
            .addField("Discord Bot", toggleStr(CONFIG.discord.enable) + " (" + DISCORD.getJdaStatus() + ")")
            .addField("Relay", toggleStr(CONFIG.discord.chatRelay.enable))
            .addField("Channel ID", getChannelMention(CONFIG.discord.channelId))
            .addField("Relay Channel ID", getChannelMention(CONFIG.discord.chatRelay.channelId))
            .addField("Manager Role ID", getRoleMention(CONFIG.discord.accountOwnerRoleId))
            .addField("Manage Profile Image", toggleStr(CONFIG.discord.manageProfileImage))
            .addField("Manage Nickname", toggleStr(CONFIG.discord.manageNickname))
            .addField("Manage Description", toggleStr(CONFIG.discord.manageDescription))
            .addField("Manage Presence", toggleStr(CONFIG.discord.managePresence))
            .addField("Show Non-Whitelist IP", toggleStr(CONFIG.discord.showNonWhitelistLoginIP))
            .addField("Ignore Other Bots", toggleStr(CONFIG.discord.ignoreOtherBots))
            .primaryColor();
    }

    private String getChannelMention(final String channelId) {
        if (channelId == null || channelId.isEmpty()) {
            return "";
        }
        try {
            return MentionUtil.forChannel(channelId);
        } catch (final Exception e) {
            // these channels might be unset on purpose
            DEFAULT_LOG.debug("Invalid channel ID: {}", channelId, e);
            return "";
        }
    }

    private String getRoleMention(final String roleId) {
        if (roleId == null || roleId.isEmpty()) {
            return "";
        }
        try {
            return MentionUtil.forRole(roleId);
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

    private LoginResult validateToken(final String token) {
        try {
            JDABuilder
                .createLight(token)
                .setEnabledIntents(GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_MESSAGES)
                .build()
                .awaitReady()
                .shutdownNow();
            return new LoginResult(true, null);
        } catch (ShutdownException e) {
            DISCORD_LOG.error("Failed to validate token", e);
            String errorMsg = e.getMessage();
            var reason = e.getShutdownReason();
            if (reason == ShutdownReason.DISALLOWED_INTENTS) {
                errorMsg = "You must enable MESSAGE CONTENT INTENT on the Discord developer website: https://wiki.2b2t.vc/_assets/img/discord-setup/DiscordSetup2.png";
            }
            return new LoginResult(false, errorMsg);
        } catch (final Throwable e) {
            DISCORD_LOG.error("Failed validating discord token", e);
            return new LoginResult(false, e.getMessage());
        }
    }

    record LoginResult(boolean success, String error) {}
}
