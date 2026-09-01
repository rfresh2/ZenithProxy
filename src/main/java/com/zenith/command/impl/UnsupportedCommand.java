package com.zenith.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.Proxy;
import com.zenith.command.api.*;
import com.zenith.discord.Embed;
import com.zenith.network.client.Authenticator;

import java.util.UUID;

import static com.zenith.Globals.CONFIG;
import static com.zenith.command.brigadier.CustomStringArgumentType.getString;
import static com.zenith.command.brigadier.CustomStringArgumentType.wordWithChars;
import static com.zenith.command.brigadier.ToggleArgumentType.getToggle;
import static com.zenith.command.brigadier.ToggleArgumentType.toggle;
import static com.zenith.discord.DiscordBot.escape;
import static com.zenith.util.config.Config.Authentication.AccountType.OFFLINE;
import static com.zenith.util.config.Config.Authentication.OfflineUUIDMode;

public class UnsupportedCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.builder()
            .name("unsupported")
            .category(CommandCategory.MANAGE)
            .description("""
            Unsupported settings that cause critical security issues.

            Do not use edit these unless you absolutely understand what you are doing.

            No user support will be provided if you modify any of these settings.

            All subcommands are only usable from the terminal.
            """)
            .usageLines(
                "whitelist on/off",
                "spectatorWhitelist on/off",
                "allowOfflinePlayers on/off",
                "auth type offline",
                "auth offlineUsername <username>",
                "auth offlineUUID mode <random/fixed/generated>",
                "auth offlineUUID prefix <prefix>",
                "auth offlineUUID set <uuid>",
                "auth offlineUUID clear"
            )
            .build();
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("unsupported")
            .requires(c -> Command.validateCommandSource(c, CommandSources.TERMINAL))
            .then(literal("whitelist").then(argument("toggle", toggle()).executes(c -> {
                CONFIG.server.extra.whitelist.enable = getToggle(c, "toggle");
                c.getSource().getEmbed()
                    .title("Whitelist " + toggleStrCaps(CONFIG.server.extra.whitelist.enable));
            })))
            .then(literal("spectatorWhitelist").then(argument("toggle", toggle()).executes(c -> {
                CONFIG.server.spectator.whitelistEnabled = getToggle(c, "toggle");
                c.getSource().getEmbed()
                    .title("Spectator Whitelist " + toggleStrCaps(CONFIG.server.spectator.whitelistEnabled));
            })))
            .then(literal("allowOfflinePlayers").then(argument("toggle", toggle()).executes(c -> {
                CONFIG.server.verifyUsers = !getToggle(c, "toggle");
                c.getSource().getEmbed()
                    .title("Allow Offline Players " + toggleStrCaps(!CONFIG.server.verifyUsers));
            })))
            .then(literal("auth")
                .then(literal("type").then(literal("offline").executes(c -> {
                    CONFIG.authentication.accountType = OFFLINE;
                    c.getSource().getEmbed()
                        .title("Authentication Type Set");
                    Proxy.getInstance().cancelLogin();
                    Authenticator.INSTANCE.clearAuthCache();
                })))
                .then(literal("offlineUsername").then(argument("username", wordWithChars()).executes(c -> {
                    CONFIG.authentication.username = getString(c, "username");
                    c.getSource().getEmbed()
                        .title("Offline Username Set");
                    Proxy.getInstance().cancelLogin();
                    Authenticator.INSTANCE.clearAuthCache();
                })))
                .then(literal("offlineUUID")
                    .then(literal("mode").then(argument("mode", enumStrings(OfflineUUIDMode.values())).executes(c -> {
                        CONFIG.authentication.offlineUUIDMode = OfflineUUIDMode.valueOf(getString(c, "mode").toUpperCase());
                        c.getSource().getEmbed()
                            .title("Offline UUID Mode Set");
                        Proxy.getInstance().cancelLogin();
                        Authenticator.INSTANCE.clearAuthCache();
                    })))
                    .then(literal("prefix").then(argument("prefix", wordWithChars()).executes(c -> {
                        CONFIG.authentication.offlineUUIDPrefix = getString(c, "prefix");
                        c.getSource().getEmbed()
                            .title("Offline UUID Prefix Set");
                        Proxy.getInstance().cancelLogin();
                        Authenticator.INSTANCE.clearAuthCache();
                    })))
                    .then(literal("clear").executes(c -> {
                        CONFIG.authentication.offlineUUID = null;
                        CONFIG.authentication.offlineUUIDMode = OfflineUUIDMode.RANDOM;
                        c.getSource().getEmbed()
                            .title("Offline UUID Reset");
                    }))
                    .then(literal("set").then(argument("uuid", wordWithChars()).executes(c -> {
                        try {
                            CONFIG.authentication.offlineUUID = UUID.fromString(getString(c, "uuid"));
                            CONFIG.authentication.offlineUUIDMode = OfflineUUIDMode.FIXED;
                        } catch (Exception e) {
                            c.getSource().getEmbed()
                                .title("Invalid UUID")
                                .errorColor();
                            return ERROR;
                        }
                        c.getSource().getEmbed()
                            .title("Offline UUID Set");
                        Proxy.getInstance().cancelLogin();
                        Authenticator.INSTANCE.clearAuthCache();
                        return OK;
                    })))
                )
            );
    }

    @Override
    public void defaultEmbed(Embed builder) {
        builder
            .addField("Whitelist", toggleStr(CONFIG.server.extra.whitelist.enable))
            .addField("Spectator Whitelist", toggleStr(CONFIG.server.spectator.whitelistEnabled))
            .addField("Allow Offline Players", toggleStr(!CONFIG.server.verifyUsers))
            .addField("Offline Authentication", toggleStr(CONFIG.authentication.accountType == OFFLINE))
            .addField("Offline Username", escape(CONFIG.authentication.username))
            .addField("Offline UUID Mode", CONFIG.authentication.offlineUUIDMode.name().toLowerCase())
            .addField("Offline UUID Prefix", escape(CONFIG.authentication.offlineUUIDPrefix))
            .addField("Offline UUID", CONFIG.authentication.offlineUUID != null
                ? escape(CONFIG.authentication.offlineUUID.toString())
                : "(auto)")
            .primaryColor();
    }
}
