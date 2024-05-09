package com.zenith.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.command.Command;
import com.zenith.command.CommandUsage;
import com.zenith.command.brigadier.CommandCategory;
import com.zenith.command.brigadier.CommandContext;
import com.zenith.discord.Embed;
import com.zenith.module.impl.AntiKick;

import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.zenith.Shared.CONFIG;
import static com.zenith.Shared.MODULE;
import static com.zenith.command.brigadier.ToggleArgumentType.getToggle;
import static com.zenith.command.brigadier.ToggleArgumentType.toggle;
import static java.util.Arrays.asList;

public class AntiKickCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.args(
            "antiKick",
            CommandCategory.MODULE,
            """
            AntiKick kicks players controlling the proxy client if they are inactive for a set amount of time.
            
            Inactivity is defined as not moving, fishing, or swinging - which are what prevents 2b2t from kicking players.
            """,
            asList(
                "on/off",
                "playerInactivityKickMins <minutes>",
                "minWalkDistance <blocks>"
            )
        );
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("antiKick")
            .then(argument("toggle", toggle()).executes(c -> {
                CONFIG.client.extra.antiKick.enabled = getToggle(c, "toggle");
                MODULE.get(AntiKick.class).syncEnabledFromConfig();
                c.getSource().getEmbed()
                    .title("AntiKick " + toggleStrCaps(CONFIG.client.extra.antiKick.enabled));
                return 1;
            }))
            .then(literal("playerInactivityKickMins")
                      .then(argument("minutes", integer(1, 30)).executes(c -> {
                          CONFIG.client.extra.antiKick.playerInactivityKickMins = getInteger(c, "minutes");
                          c.getSource().getEmbed()
                              .title("Inactivity Time Set!");
                          return 1;
                      })))
            .then(literal("minWalkDistance")
                      .then(argument("blocks", integer(1, 100)).executes(c -> {
                          CONFIG.client.extra.antiKick.minWalkDistance = getInteger(c, "blocks");
                          c.getSource().getEmbed()
                              .title("Min Walk Distance Set!");
                          return 1;
                      })));
    }

    @Override
    public void postPopulate(final Embed builder) {
        builder
            .primaryColor()
            .addField("Enabled", toggleStrCaps(CONFIG.client.extra.antiKick.enabled), false)
            .addField("Player Inactivity Minutes", CONFIG.client.extra.antiKick.playerInactivityKickMins, false)
            .addField("Min Walk Distance", CONFIG.client.extra.antiKick.minWalkDistance, false);
    }
}
