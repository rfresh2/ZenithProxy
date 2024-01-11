package com.zenith.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.Proxy;
import com.zenith.command.*;
import com.zenith.util.Config;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;

import java.util.Arrays;

import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.zenith.Shared.CONFIG;
import static com.zenith.command.CustomStringArgumentType.getString;
import static com.zenith.command.CustomStringArgumentType.wordWithChars;
import static java.util.Arrays.asList;

public class AuthCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.args("auth",
                                 CommandCategory.MANAGE,
                                 "Configures the proxy's authentication settings",
                                 asList(
                                     "clear",
                                     "attempts <int>",
                                     "type list",
                                     "type <type>",
                                     "email <email>",
                                     "password <password>"
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
                c.getSource().getEmbedBuilder()
                    .title("Authentication Cleared")
                    .description("Cached tokens and authentication state cleared. Full re-auth will occur on next login.")
                    .color(Color.CYAN);
            }))
            .then(literal("attempts").then(argument("attempts", integer(1, 10)).executes(c -> {
                CONFIG.authentication.msaLoginAttemptsBeforeCacheWipe = c.getArgument("attempts", Integer.class);
                c.getSource().getEmbedBuilder()
                    .title("Authentication Max Attempts Set")
                    .color(Color.CYAN);
                return 1;
            })))
            .then(literal("type").requires(this::validateTerminalSource)
                      .then(literal("list").executes(c -> {
                          c.getSource().getEmbedBuilder()
                              .title("Authentication Types")
                              .color(Color.CYAN);
                          return 1;
                      }))
                      .then(argument("type", wordWithChars()).executes(c -> {
                          String type = getString(c, "type").toUpperCase().trim();
                          try {
                              CONFIG.authentication.accountType = Config.Authentication.AccountType.valueOf(type);
                              Proxy.getInstance().getAuthenticator().clearAuthCache();
                              c.getSource().getEmbedBuilder()
                                  .title("Authentication Type Set")
                                  .color(Color.CYAN);
                          } catch (final Exception e) {
                              c.getSource().getEmbedBuilder()
                                  .title("Invalid Authentication Type")
                                  .description("Valid types: " + Arrays.toString(Config.Authentication.AccountType.values()))
                                  .color(Color.RED);
                          }
                          return 1;
                      })))
            .then(literal("email").requires(this::validateTerminalSource)
                      .then(argument("email", wordWithChars()).executes(c -> {
                          c.getSource().setSensitiveInput(true);
                          var emailStr = getString(c, "email").trim();
                          // validate email str is an email
                          if (!emailStr.contains("@") || emailStr.length() < 3) {
                              c.getSource().getEmbedBuilder()
                                  .title("Invalid Email")
                                  .color(Color.RED);
                              return 1;
                          }
                          CONFIG.authentication.email = emailStr;
                          c.getSource().getEmbedBuilder()
                              .title("Authentication Email Set")
                              .color(Color.CYAN);
                          return 1;
                      })))
            .then(literal("password").requires(this::validateTerminalSource)
                      .then(argument("password", wordWithChars()).executes(c -> {
                          c.getSource().setSensitiveInput(true);
                          var passwordStr = getString(c, "password").trim();
                          // validate password str is a password
                          if (passwordStr.isBlank()) {
                              c.getSource().getEmbedBuilder()
                                  .title("Invalid Password")
                                  .color(Color.RED);
                              return 1;
                          }
                          CONFIG.authentication.password = passwordStr;
                          c.getSource().getEmbedBuilder()
                              .title("Authentication Password Set")
                              .color(Color.CYAN);
                          return 1;
                      })));
    }

    private boolean validateTerminalSource(CommandContext c) {
        return Command.validateCommandSource(c, CommandSource.TERMINAL);
    }

    @Override
    public void postPopulate(final EmbedCreateSpec.Builder builder) {
        builder.title("Authentication")
            .description("Configure authentication settings")
            .color(Color.CYAN)
            .addField("Account Type", CONFIG.authentication.accountType.toString(), true)
            .addField("Available Types", Arrays.toString(Config.Authentication.AccountType.values()), true)
            .addField("Attempts", ""+CONFIG.authentication.msaLoginAttemptsBeforeCacheWipe, true);
    }
}
