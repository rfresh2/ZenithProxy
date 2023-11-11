package com.zenith.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.Proxy;
import com.zenith.command.Command;
import com.zenith.command.CommandCategory;
import com.zenith.command.CommandContext;
import com.zenith.command.CommandUsage;
import discord4j.rest.util.Color;

import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.zenith.Shared.CONFIG;
import static java.util.Arrays.asList;

public class AuthCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.args("auth",
                                 CommandCategory.MANAGE,
                                 "Configures the proxy's authentication settings",
                                 asList(
                                     "clear",
                                     "attempts <int>"
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
                Proxy.getInstance().getAuthenticator().reset();
                c.getSource().getEmbedBuilder()
                    .title("Authentication Cleared")
                    .description("Cached tokens and authentication state cleared. Full re-auth will occur on next login.")
                    .color(Color.CYAN);
            }))
            .then(literal("attempts").then(argument("attempts", integer(1, 10)).executes(c -> {
                CONFIG.authentication.msaLoginAttemptsBeforeCacheWipe = c.getArgument("attempts", Integer.class);
                c.getSource().getEmbedBuilder()
                    .title("Authentication Max Attempts Set")
                    .color(Color.CYAN)
                    .addField("Attempts", ""+CONFIG.authentication.msaLoginAttemptsBeforeCacheWipe, true);
                return 1;
            })));
                  // todo: add ability to change account type and enter user/pass?
                  //    ideally we'd delete the messages containing credentials
                  //    or we configure this through DM's
                  //    or its a terminal only command

    }
}
