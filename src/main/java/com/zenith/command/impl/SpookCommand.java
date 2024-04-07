package com.zenith.command.impl;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.command.Command;
import com.zenith.command.CommandUsage;
import com.zenith.command.brigadier.CommandCategory;
import com.zenith.command.brigadier.CommandContext;
import com.zenith.discord.Embed;
import com.zenith.module.impl.Spook;
import com.zenith.util.Config;

import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.zenith.Shared.CONFIG;
import static com.zenith.Shared.MODULE;
import static com.zenith.command.brigadier.ToggleArgumentType.getToggle;
import static com.zenith.command.brigadier.ToggleArgumentType.toggle;
import static java.util.Arrays.asList;

public class SpookCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.args(
            "spook",
            CommandCategory.MODULE,
            "Automatically spooks nearby players",
            asList("on/off", "delay <ticks>", "mode <visualRange/nearest>")
        );
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("spook")
            .then(argument("toggle", toggle()).executes(c -> {
                CONFIG.client.extra.spook.enabled = getToggle(c, "toggle");
                MODULE.get(Spook.class).syncEnabledFromConfig();
                c.getSource().getEmbed()
                    .title("Spook " + toggleStrCaps(CONFIG.client.extra.spook.enabled));
                return OK;
            }))
            .then(literal("delay").then(argument("delayTicks", integer(0, 1000)).executes(c -> {
                final int delay = IntegerArgumentType.getInteger(c, "delayTicks");
                CONFIG.client.extra.spook.tickDelay = (long) delay;
                c.getSource().getEmbed()
                    .title("Spook Delay Updated!");
                return OK;
            })))
            .then(literal("mode")
                      .then(literal("nearest").executes(c -> {
                          CONFIG.client.extra.spook.spookTargetingMode = Config.Client.Extra.Spook.TargetingMode.NEAREST;
                          c.getSource().getEmbed()
                              .title("Spook Mode Updated!");
                      }))
                      .then(literal("visualrange").executes(c -> {
                          CONFIG.client.extra.spook.spookTargetingMode = Config.Client.Extra.Spook.TargetingMode.VISUAL_RANGE;
                          c.getSource().getEmbed()
                              .title("Spook Mode Updated!");
                      })));
    }

    @Override
    public void postPopulate(final Embed builder) {
        builder
            .addField("Spook", toggleStr(CONFIG.client.extra.spook.enabled), false)
            .addField("Delay", CONFIG.client.extra.spook.tickDelay + " tick(s)", false)
            .addField("Mode", CONFIG.client.extra.spook.spookTargetingMode.toString().toLowerCase(), false)
            .primaryColor();
    }
}
