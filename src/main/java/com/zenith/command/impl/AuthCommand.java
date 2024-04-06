package com.zenith.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.Proxy;
import com.zenith.command.Command;
import com.zenith.command.CommandUsage;
import com.zenith.command.brigadier.CommandCategory;
import com.zenith.command.brigadier.CommandContext;
import com.zenith.command.brigadier.CommandSource;
import com.zenith.discord.Embed;
import com.zenith.util.Config;

import java.util.Arrays;

import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.zenith.Shared.CONFIG;
import static com.zenith.command.brigadier.CustomStringArgumentType.getString;
import static com.zenith.command.brigadier.CustomStringArgumentType.wordWithChars;
import static com.zenith.command.brigadier.ToggleArgumentType.getToggle;
import static com.zenith.command.brigadier.ToggleArgumentType.toggle;
import static java.util.Arrays.asList;

public class AuthCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.args(
            "auth",
            CommandCategory.MANAGE,
            "Configures the proxy's authentication settings",
            asList(
                "clear",
                "attempts <int>",
                "alwaysRefreshOnLogin on/off",
                "type list",
                "type <type>",
                "email <email>",
                "password <password>",
                "mention on/off",
                "openBrowser on/off"
            )
        );
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("auth").requires(Command::validateAccountOwner)
            /**
             * Lets us reset the current authentication state
             * Can be used to switch accounts if using device code auth
             */
            .then(literal("clear").executes(c -> {
                Proxy.getInstance().cancelLogin();
                Proxy.getInstance().getAuthenticator().clearAuthCache();
                c.getSource().getEmbed()
                    .title("Authentication Cleared")
                    .description("Cached tokens and authentication state cleared. Full re-auth will occur on next login.")
                    .primaryColor();
            }))
            .then(literal("attempts").then(argument("attempts", integer(1, 10)).executes(c -> {
                CONFIG.authentication.msaLoginAttemptsBeforeCacheWipe = c.getArgument("attempts", Integer.class);
                c.getSource().getEmbed()
                    .title("Authentication Max Attempts Set")
                    .primaryColor();
                return 1;
            })))
            .then(literal("alwaysRefreshOnLogin").then(argument("toggle", toggle()).executes(c -> {
                CONFIG.authentication.alwaysRefreshOnLogin = getToggle(c, "toggle");
                c.getSource().getEmbed()
                    .title("Always Refresh On Login " + toggleStrCaps(CONFIG.authentication.alwaysRefreshOnLogin))
                    .primaryColor();
                return 1;
            })))
            .then(literal("type").requires(this::validateDiscordOrTerminalSource)
                      .then(literal("list").executes(c -> {
                          c.getSource().getEmbed()
                              .title("Authentication Types")
                              .primaryColor();
                          return 1;
                      }))
                      .then(argument("type", wordWithChars()).executes(c -> {
                          String type = getString(c, "type").toUpperCase().trim();
                          try {
                              CONFIG.authentication.accountType = Config.Authentication.AccountType.valueOf(type);
                              Proxy.getInstance().getAuthenticator().clearAuthCache();
                              c.getSource().getEmbed()
                                  .title("Authentication Type Set")
                                  .primaryColor();
                          } catch (final Exception e) {
                              c.getSource().getEmbed()
                                  .title("Invalid Authentication Type")
                                  .description("Valid types: " + Arrays.toString(Config.Authentication.AccountType.values()))
                                  .errorColor();
                          }
                          return 1;
                      })))
            .then(literal("email").requires(this::validateTerminalSource)
                      .then(argument("email", wordWithChars()).executes(c -> {
                          c.getSource().setSensitiveInput(true);
                          var emailStr = getString(c, "email").trim();
                          // validate email str is an email
                          if (!emailStr.contains("@") || emailStr.length() < 3) {
                              c.getSource().getEmbed()
                                  .title("Invalid Email")
                                  .errorColor();
                              return 1;
                          }
                          CONFIG.authentication.email = emailStr;
                          c.getSource().getEmbed()
                              .title("Authentication Email Set")
                              .primaryColor();
                          return 1;
                      })))
            .then(literal("password").requires(this::validateTerminalSource)
                      .then(argument("password", wordWithChars()).executes(c -> {
                          c.getSource().setSensitiveInput(true);
                          var passwordStr = getString(c, "password").trim();
                          // validate password str is a password
                          if (passwordStr.isBlank()) {
                              c.getSource().getEmbed()
                                  .title("Invalid Password")
                                  .errorColor();
                              return 1;
                          }
                          CONFIG.authentication.password = passwordStr;
                          c.getSource().getEmbed()
                              .title("Authentication Password Set")
                              .primaryColor();
                          return 1;
                      })))
            .then(literal("mention")
                      .then(argument("toggle", toggle()).executes(c -> {
                          CONFIG.discord.mentionRoleOnDeviceCodeAuth = getToggle(c, "toggle");
                            c.getSource().getEmbed()
                                .title("Mention Role " + toggleStrCaps(CONFIG.discord.mentionRoleOnDeviceCodeAuth))
                                .primaryColor();
                            return 1;
                      })))
            .then(literal("openBrowser").then(argument("toggle", toggle()).executes(c -> {
                CONFIG.authentication.openBrowserOnLogin = getToggle(c, "toggle");
                c.getSource().getEmbed()
                    .title("Open Browser On Login " + toggleStrCaps(CONFIG.authentication.openBrowserOnLogin))
                    .primaryColor();
                return 1;
            })));
    }

    private boolean validateTerminalSource(CommandContext c) {
        return Command.validateCommandSource(c, CommandSource.TERMINAL);
    }

    private boolean validateDiscordOrTerminalSource(CommandContext c) {
        return Command.validateCommandSource(c, asList(CommandSource.TERMINAL, CommandSource.DISCORD));
    }

    @Override
    public void postPopulate(final Embed builder) {
        builder
            .addField("Account Type", CONFIG.authentication.accountType.toString(), false)
            .addField("Available Types", Arrays.toString(Config.Authentication.AccountType.values()), false)
            .addField("Attempts", CONFIG.authentication.msaLoginAttemptsBeforeCacheWipe, false)
            .addField("Always Refresh On Login", toggleStr(CONFIG.authentication.alwaysRefreshOnLogin), false)
            .addField("Mention", toggleStr(CONFIG.discord.mentionRoleOnDeviceCodeAuth), false)
            .addField("Open Browser", toggleStr(CONFIG.authentication.openBrowserOnLogin), false);
    }
}
