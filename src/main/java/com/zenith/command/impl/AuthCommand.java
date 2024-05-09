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
            """
            Configures the proxy's authentication settings.
            
            To switch accounts, use the `clear` command.
            
            `attempts` configures the number of login attempts before wiping the cache.
            
            `alwaysRefreshOnLogin` will always refresh the token on login instead of trusting the cache. This can cause
            Microsoft to rate limit your account. Auth tokens will always refresh in the background even if this is off.
            
            `deviceCode` is the default and recommended authentication type.
            If authentication fails, try logging into the account on the vanilla MC launcher and joining a server. Then try again in Zenith.
            If this still fails, try one of the alternate auth types.
            
            """,
            asList(
                "clear",
                "attempts <int>",
                "alwaysRefreshOnLogin on/off",
                "type <deviceCode/emailAndPassword/deviceCode2/meteor/prism>",
                "email <email>",
                "password <password>",
                "mention on/off",
                "openBrowser on/off",
                "maxRefreshIntervalMins <minutes>",
                "useClientConnectionProxy on/off"
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
                return OK;
            })))
            .then(literal("alwaysRefreshOnLogin").then(argument("toggle", toggle()).executes(c -> {
                CONFIG.authentication.alwaysRefreshOnLogin = getToggle(c, "toggle");
                c.getSource().getEmbed()
                    .title("Always Refresh On Login " + toggleStrCaps(CONFIG.authentication.alwaysRefreshOnLogin))
                    .primaryColor();
                return 1;
            })))
            .then(literal("type").requires(this::validateDiscordOrTerminalSource)
                      .then(literal("deviceCode").executes(c -> {
                          CONFIG.authentication.accountType = Config.Authentication.AccountType.DEVICE_CODE;
                          c.getSource().getEmbed()
                              .title("Authentication Type Set")
                              .primaryColor();
                          Proxy.getInstance().cancelLogin();
                          Proxy.getInstance().getAuthenticator().clearAuthCache();
                          return 1;
                      }))
                      .then(literal("emailAndPassword").executes(c -> {
                          CONFIG.authentication.accountType = Config.Authentication.AccountType.MSA;
                          c.getSource().getEmbed()
                              .title("Authentication Type Set")
                              .primaryColor();
                          Proxy.getInstance().cancelLogin();
                          Proxy.getInstance().getAuthenticator().clearAuthCache();
                          return 1;
                      }))
                      .then(literal("deviceCode2").executes(c -> {
                          CONFIG.authentication.accountType = Config.Authentication.AccountType.DEVICE_CODE_WITHOUT_DEVICE_TOKEN;
                          c.getSource().getEmbed()
                              .title("Authentication Type Set")
                              .primaryColor();
                          Proxy.getInstance().cancelLogin();
                          Proxy.getInstance().getAuthenticator().clearAuthCache();
                          return 1;
                      }))
                      .then(literal("meteor").executes(c -> {
                          CONFIG.authentication.accountType = Config.Authentication.AccountType.LOCAL_WEBSERVER;
                          c.getSource().getEmbed()
                              .title("Authentication Type Set")
                              .primaryColor();
                          Proxy.getInstance().cancelLogin();
                          Proxy.getInstance().getAuthenticator().clearAuthCache();
                          return 1;
                      }))
                      .then(literal("prism").executes(c -> {
                          CONFIG.authentication.accountType = Config.Authentication.AccountType.PRISM;
                          c.getSource().getEmbed()
                              .title("Authentication Type Set")
                              .primaryColor();
                          Proxy.getInstance().cancelLogin();
                          Proxy.getInstance().getAuthenticator().clearAuthCache();
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
            })))
            .then(literal("maxRefreshInterval").then(argument("minutes", integer(5, 500)).executes(c -> {
                CONFIG.authentication.maxRefreshIntervalMins = c.getArgument("minutes", Integer.class);
                c.getSource().getEmbed()
                    .title("Max Refresh Interval Set")
                    .primaryColor();
                return OK;
            })))
            .then(literal("useClientConnectionProxy").then(argument("toggle", toggle()).executes(c -> {
                CONFIG.authentication.useClientConnectionProxy = getToggle(c, "toggle");
                c.getSource().getEmbed()
                    .title("Use Client Connection Proxy " + toggleStrCaps(CONFIG.authentication.useClientConnectionProxy))
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
            .addField("Account Type", authTypeToString(CONFIG.authentication.accountType), false)
            .addField("Attempts", CONFIG.authentication.msaLoginAttemptsBeforeCacheWipe, false)
            .addField("Always Refresh On Login", toggleStr(CONFIG.authentication.alwaysRefreshOnLogin), false)
            .addField("Mention", toggleStr(CONFIG.discord.mentionRoleOnDeviceCodeAuth), false)
            .addField("Open Browser", toggleStr(CONFIG.authentication.openBrowserOnLogin), false)
            .addField("Max Refresh Interval", CONFIG.authentication.maxRefreshIntervalMins + " minutes", false)
            .addField("Use Client Connection Proxy", toggleStr(CONFIG.authentication.useClientConnectionProxy), false);
    }

    private String authTypeToString(Config.Authentication.AccountType type) {
        return switch (type) {
            case DEVICE_CODE -> "deviceCode";
            case MSA -> "emailAndPassword";
            case DEVICE_CODE_WITHOUT_DEVICE_TOKEN -> "deviceCode2";
            case LOCAL_WEBSERVER -> "meteor";
            case PRISM -> "prism";
        };
    }
}
