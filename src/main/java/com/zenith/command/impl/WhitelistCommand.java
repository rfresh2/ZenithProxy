package com.zenith.command.impl;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.Proxy;
import com.zenith.command.Command;
import com.zenith.command.CommandUsage;
import com.zenith.command.brigadier.CommandCategory;
import com.zenith.command.brigadier.CommandContext;
import com.zenith.discord.Embed;

import static com.mojang.brigadier.arguments.StringArgumentType.string;
import static com.zenith.Shared.CONFIG;
import static com.zenith.Shared.PLAYER_LISTS;
import static com.zenith.command.brigadier.ToggleArgumentType.getToggle;
import static com.zenith.command.brigadier.ToggleArgumentType.toggle;
import static com.zenith.command.util.CommandOutputHelper.playerListToString;
import static com.zenith.discord.DiscordBot.escape;
import static java.util.Arrays.asList;

public class WhitelistCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.full(
            "whitelist",
            CommandCategory.CORE,
            """
            Manages the list of players allowed to login.
            
            Whitelisted players are allowed to both control the account in-game and spectate.
            """,
            asList(
                "add/del <player>",
                "list",
                "clear",
                "autoAddClient on/off"
            ),
            asList("wl")
        );
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("whitelist")
                .then(literal("add").requires(Command::validateAccountOwner).then(argument("player", string()).executes(c -> {
                    final String player = StringArgumentType.getString(c, "player");
                    PLAYER_LISTS.getWhitelist().add(player).ifPresentOrElse(e ->
                                    c.getSource().getEmbed()
                                            .title("Added user: " + escape(e.getUsername()) + " To Whitelist"),
                                                                            () -> c.getSource().getEmbed()
                                    .title("Failed to add user: " + escape(player) + " to whitelist. Unable to lookup profile."));
                    return OK;
                })))
                .then(literal("del").requires(Command::validateAccountOwner).then(argument("player", string()).executes(c -> {
                    final String player = StringArgumentType.getString(c, "player");
                    PLAYER_LISTS.getWhitelist().remove(player);
                    c.getSource().getEmbed()
                            .title("Removed user: " + escape(player) + " From Whitelist");
                    Proxy.getInstance().kickNonWhitelistedPlayers();
                    return 1;
                })))
                .then(literal("list").executes(c -> {
                    c.getSource().getEmbed()
                            .title("Whitelist List");
                }))
                .then(literal("clear").requires(Command::validateAccountOwner).executes(c -> {
                    PLAYER_LISTS.getWhitelist().clear();
                    c.getSource().getEmbed()
                            .title("Whitelist Cleared");
                    Proxy.getInstance().kickNonWhitelistedPlayers();
                    return 1;
                }))
            .then(literal("autoAddClient").requires(Command::validateAccountOwner)
                      .then(argument("toggle", toggle()).executes(c -> {
                          CONFIG.server.extra.whitelist.autoAddClient = getToggle(c, "toggle");
                          c.getSource().getEmbed()
                              .title("Auto Add Client " + toggleStrCaps(CONFIG.server.extra.whitelist.autoAddClient));
                          return 1;
                      })));
    }

    @Override
    public void postPopulate(final Embed builder) {
        builder
            .description(playerListToString(PLAYER_LISTS.getWhitelist()))
            .addField("Auto Add Client", toggleStr(CONFIG.server.extra.whitelist.autoAddClient), false)
            .primaryColor();
    }
}
