package com.zenith.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.command.api.Command;
import com.zenith.command.api.CommandCategory;
import com.zenith.command.api.CommandContext;
import com.zenith.command.api.CommandUsage;
import com.zenith.discord.Embed;
import com.zenith.util.MentionUtil;

import java.util.regex.Pattern;

import static com.zenith.Globals.CONFIG;
import static com.zenith.Globals.DISCORD_LOG;
import static com.zenith.command.brigadier.CustomStringArgumentType.getString;
import static com.zenith.command.brigadier.CustomStringArgumentType.wordWithChars;
import static com.zenith.command.brigadier.ToggleArgumentType.getToggle;
import static com.zenith.command.brigadier.ToggleArgumentType.toggle;

public class DiscordNotificationsCommand extends Command {
    private static final Pattern ROLE_ID_PATTERN = Pattern.compile("<@&\\d+>");

    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.builder()
            .name("discordNotifications")
            .category(CommandCategory.INFO)
            .description("""
            Configures various discord notifications regarding player and proxy connections, deaths, and more.
            """)
            .usageLines(
                "role set <roleId>",
                "role reset",
                "connect mention on/off",
                "online mention on/off",
                "disconnect mention on/off",
                "startQueue mention on/off",
                "death mention on/off",
                "serverRestart mention on/off",
                "loginFailed mention on/off",
                "clientConnect mention on/off",
                "clientDisconnect mention on/off",
                "spectatorConnect mention on/off",
                "spectatorDisconnect mention on/off",
                "nonWhitelistedConnect mention on/off",
                "mcVersionMismatchWarning on/off",
                "prio mention on/off"
            )
            .aliases(
                "alerts",
                "notifications"
            )
            .build();
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("discordNotifications")
            .then(literal("role").requires(Command::validateAccountOwner)
                      .then(literal("set").then(argument("roleId", wordWithChars()).executes(c -> {
                          var roleStr = getString(c, "roleId");
                          if (ROLE_ID_PATTERN.matcher(roleStr).matches()) {
                              roleStr = roleStr.substring(3, roleStr.length() - 1);
                          }
                          try {
                              Long.parseUnsignedLong(roleStr);
                              if (roleStr.length() < 5) throw new NumberFormatException();
                          } catch (final Exception e) {
                              c.getSource().getEmbed()
                                  .title("Invalid role ID");
                              return OK;
                          }
                          CONFIG.discord.notificationMentionRoleId = roleStr;
                          c.getSource().getEmbed()
                              .title("Notification Role Set");
                          return OK;
                      })))
                      .then(literal("reset").executes(c -> {
                            CONFIG.discord.notificationMentionRoleId = "";
                            c.getSource().getEmbed()
                                .title("Notification Role Reset");
                            return OK;
                      })))
            .then(literal("connect").then(literal("mention").then(argument("toggle", toggle()).executes(c -> {
                CONFIG.discord.mentionRoleOnConnect = getToggle(c, "toggle");
                c.getSource().getEmbed()
                    .title("Connect Mention " + toggleStrCaps(CONFIG.discord.mentionRoleOnConnect));
                return OK;
            }))))
            .then(literal("online").then(literal("mention").then(argument("toggle", toggle()).executes(c -> {
                CONFIG.discord.mentionRoleOnPlayerOnline = getToggle(c, "toggle");
                c.getSource().getEmbed()
                    .title("Online Mention " + toggleStrCaps(CONFIG.discord.mentionRoleOnPlayerOnline));
                return OK;
            }))))
            .then(literal("disconnect").then(literal("mention").then(argument("toggle", toggle()).executes(c -> {
                CONFIG.discord.mentionRoleOnDisconnect = getToggle(c, "toggle");
                c.getSource().getEmbed()
                    .title("Disconnect Mention " + toggleStrCaps(CONFIG.discord.mentionRoleOnDisconnect));
                return OK;
            }))))
            .then(literal("startQueue").then(literal("mention").then(argument("toggle", toggle()).executes(c -> {
                CONFIG.discord.mentionRoleOnStartQueue = getToggle(c, "toggle");
                c.getSource().getEmbed()
                    .title("Start Queue Mention " + toggleStrCaps(CONFIG.discord.mentionRoleOnStartQueue));
                return OK;
            }))))
            .then(literal("death").then(literal("mention").then(argument("toggle", toggle()).executes(c -> {
                CONFIG.discord.mentionRoleOnDeath = getToggle(c, "toggle");
                c.getSource().getEmbed()
                    .title("Death Mention " + toggleStrCaps(CONFIG.discord.mentionRoleOnDeath));
                return OK;
            }))))
            .then(literal("serverRestart").then(literal("mention").then(argument("toggle", toggle()).executes(c -> {
                CONFIG.discord.mentionRoleOnServerRestart = getToggle(c, "toggle");
                c.getSource().getEmbed()
                    .title("Server Restart Mention " + toggleStrCaps(CONFIG.discord.mentionRoleOnServerRestart));
                return OK;
            }))))
            .then(literal("loginFailed").then(literal("mention").then(argument("toggle", toggle()).executes(c -> {
                CONFIG.discord.mentionRoleOnLoginFailed = getToggle(c, "toggle");
                c.getSource().getEmbed()
                    .title("Login Failed Mention " + toggleStrCaps(CONFIG.discord.mentionRoleOnLoginFailed));
                return OK;
            }))))
            .then(literal("clientConnect").then(literal("mention").then(argument("toggle", toggle()).executes(c -> {
                CONFIG.discord.mentionOnClientConnected = getToggle(c, "toggle");
                c.getSource().getEmbed()
                    .title("Client Connect Mention " + toggleStrCaps(CONFIG.discord.mentionOnClientConnected));
                return OK;
            }))))
            .then(literal("clientDisconnect").then(literal("mention").then(argument("toggle", toggle()).executes(c -> {
                CONFIG.discord.mentionOnClientDisconnected = getToggle(c, "toggle");
                c.getSource().getEmbed()
                    .title("Client Disconnect Mention " + toggleStrCaps(CONFIG.discord.mentionOnClientDisconnected));
                return OK;
            }))))
            .then(literal("spectatorConnect").then(literal("mention").then(argument("toggle", toggle()).executes(c -> {
                CONFIG.discord.mentionOnSpectatorConnected = getToggle(c, "toggle");
                c.getSource().getEmbed()
                    .title("Spectator Connect Mention " + toggleStrCaps(CONFIG.discord.mentionOnSpectatorConnected));
                return OK;
            }))))
            .then(literal("spectatorDisconnect").then(literal("mention").then(argument("toggle", toggle()).executes(c -> {
                CONFIG.discord.mentionOnSpectatorDisconnected = getToggle(c, "toggle");
                c.getSource().getEmbed()
                    .title("Spectator Disconnect Mention " + toggleStrCaps(CONFIG.discord.mentionOnSpectatorDisconnected));
                return OK;
            }))))
            .then(literal("nonWhitelistedConnect").then(literal("mention").then(argument("toggle", toggle()).executes(c -> {
                CONFIG.discord.mentionOnNonWhitelistedClientConnected = getToggle(c, "toggle");
                c.getSource().getEmbed()
                    .title("Non Whitelisted Connect Mention " + toggleStrCaps(CONFIG.discord.mentionOnNonWhitelistedClientConnected));
                return OK;
            }))))
            .then(literal("prio").then(literal("mention").then(argument("toggle", toggle()).executes(c -> {
                CONFIG.discord.mentionRoleOnPrioUpdate = getToggle(c, "toggle");
                c.getSource().getEmbed()
                    .title("Prio Mention " + toggleStrCaps(CONFIG.discord.mentionRoleOnPrioUpdate));
            }))))
            .then(literal("mcVersionMismatchWarning").then(argument("toggle", toggle()).executes(c -> {
                CONFIG.discord.mcVersionMismatchWarning = getToggle(c, "toggle");
                c.getSource().getEmbed()
                    .title("MC Version Mismatch Warning " + toggleStrCaps(CONFIG.discord.mcVersionMismatchWarning));
                return OK;
            })));
    }

    @Override
    public void defaultEmbed(final Embed builder) {
        builder
            .primaryColor()
            .addField("Notification Role", CONFIG.discord.notificationMentionRoleId.isEmpty()
                ? getRoleMention(CONFIG.discord.accountOwnerRoleId) + " (Manager)"
                : getRoleMention(CONFIG.discord.notificationMentionRoleId))
            .addField("Connect Mention", toggleStr(CONFIG.discord.mentionRoleOnConnect))
            .addField("Online Mention", toggleStr(CONFIG.discord.mentionRoleOnPlayerOnline))
            .addField("Disconnect Mention", toggleStr(CONFIG.discord.mentionRoleOnDisconnect))
            .addField("Start Queue Mention", toggleStr(CONFIG.discord.mentionRoleOnStartQueue))
            .addField("Death Mention", toggleStr(CONFIG.discord.mentionRoleOnDeath))
            .addField("Server Restart Mention", toggleStr(CONFIG.discord.mentionRoleOnServerRestart))
            .addField("Login Failed Mention", toggleStr(CONFIG.discord.mentionRoleOnLoginFailed))
            .addField("Client Connect Mention", toggleStr(CONFIG.discord.mentionOnClientConnected))
            .addField("Client Disconnect Mention", toggleStr(CONFIG.discord.mentionOnClientDisconnected))
            .addField("Spectator Connect Mention", toggleStr(CONFIG.discord.mentionOnSpectatorConnected))
            .addField("Spectator Disconnect Mention", toggleStr(CONFIG.discord.mentionOnSpectatorDisconnected))
            .addField("Non Whitelisted Connect Mention", toggleStr(CONFIG.discord.mentionOnNonWhitelistedClientConnected))
            .addField("Prio Mention", toggleStr(CONFIG.discord.mentionRoleOnPrioUpdate))
            .addField("MC Version Mismatch Warning", toggleStr(CONFIG.discord.mcVersionMismatchWarning));
    }

    private String getRoleMention(final String roleId) {
        try {
            return MentionUtil.forRole(roleId);
        } catch (final NumberFormatException e) {
            DISCORD_LOG.error("Unable to generate mention for role ID: {}", roleId, e);
            return "";
        }
    }
}
