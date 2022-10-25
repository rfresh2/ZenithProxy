package com.zenith.discord.command.impl;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.zenith.discord.command.Command;
import com.zenith.discord.command.CommandContext;
import com.zenith.discord.command.CommandUsage;
import discord4j.rest.util.Color;

import static com.mojang.brigadier.arguments.StringArgumentType.string;
import static com.zenith.discord.DiscordBot.escape;
import static com.zenith.util.Constants.CONFIG;
import static java.util.Arrays.asList;

public class WhitelistCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.of(
                "whitelist",
                "Manage the proxy's whitelist. Only usable by users with the account owner role.",
                asList("add/del <player>", "list", "clear")
        );
    }

    @Override
    public void register(CommandDispatcher<CommandContext> dispatcher) {
        dispatcher.register(
                command("whitelist")
                        .then(literal("add").requires(this::validateAccountOwner).then(argument("player", string()).executes(c -> {
                            final String player = StringArgumentType.getString(c, "player");
                            if (!CONFIG.server.extra.whitelist.allowedUsers.contains(player)) {
                                CONFIG.server.extra.whitelist.allowedUsers.add(player);
                            }
                            c.getSource().getEmbedBuilder()
                                    .title("Added user: " + escape(player) + " To Whitelist")
                                    .color(Color.CYAN)
                                    .addField("Whitelisted", escape(((CONFIG.server.extra.whitelist.allowedUsers.size() > 0) ? String.join(", ", CONFIG.server.extra.whitelist.allowedUsers) : "Whitelist is empty")),
                                            false);
                            return 1;
                        })))
                        .then(literal("del").requires(this::validateAccountOwner).then(argument("player", string()).executes(c -> {
                            final String player = StringArgumentType.getString(c, "player");
                            CONFIG.server.extra.whitelist.allowedUsers.removeIf(s -> s.equalsIgnoreCase(player));
                            c.getSource().getEmbedBuilder()
                                    .title("Removed user: " + escape(player) + " From Whitelist")
                                    .color(Color.CYAN)
                                    .addField("Whitelisted", escape(((CONFIG.server.extra.whitelist.allowedUsers.size() > 0) ? String.join(", ", CONFIG.server.extra.whitelist.allowedUsers) : "Whitelist is empty")),
                                            false);
                            return 1;
                        })))
                        .then(literal("list").executes(c -> {
                            c.getSource().getEmbedBuilder()
                                    .title("Whitelist List")
                                    .color(Color.CYAN)
                                    .addField("Whitelisted", escape(((CONFIG.server.extra.whitelist.allowedUsers.size() > 0) ? String.join(", ", CONFIG.server.extra.whitelist.allowedUsers) : "Whitelist is empty")),
                                            false);
                        }))
                        .then(literal("clear").requires(this::validateAccountOwner).executes(c -> {
                            CONFIG.server.extra.whitelist.allowedUsers.clear();
                            c.getSource().getEmbedBuilder()
                                    .title("Whitelist Cleared")
                                    .color(Color.RUBY)
                                    .addField("Whitelisted", escape(((CONFIG.server.extra.whitelist.allowedUsers.size() > 0) ? String.join(", ", CONFIG.server.extra.whitelist.allowedUsers) : "Whitelist is empty")),
                                            false);
                            return 1;
                        }))
        );
    }
}
