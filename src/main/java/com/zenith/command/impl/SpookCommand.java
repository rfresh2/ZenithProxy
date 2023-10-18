package com.zenith.command.impl;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.command.Command;
import com.zenith.command.CommandContext;
import com.zenith.command.CommandUsage;
import com.zenith.module.Module;
import com.zenith.module.impl.Spook;
import com.zenith.util.Config;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;

import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.zenith.Shared.CONFIG;
import static com.zenith.Shared.MODULE_MANAGER;
import static com.zenith.command.ToggleArgumentType.getToggle;
import static com.zenith.command.ToggleArgumentType.toggle;
import static java.util.Arrays.asList;

public class SpookCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.args(
                "spook",
                "Automatically spooks nearby players",
                asList("on/off", "delay <ticks>", "mode <visualRange/nearest>")
        );
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("spook")
            .then(argument("toggle", toggle()).executes(c -> {
                CONFIG.client.extra.spook.enabled = getToggle(c, "toggle");
                MODULE_MANAGER.getModule(Spook.class).ifPresent(Module::syncEnabledFromConfig);
                c.getSource().getEmbedBuilder()
                    .title("Spook " + (CONFIG.client.extra.spook.enabled ? "On!" : "Off!"));
                return 1;
            }))
            .then(literal("delay").then(argument("delayTicks", integer()).executes(c -> {
                final int delay = IntegerArgumentType.getInteger(c, "delayTicks");
                CONFIG.client.extra.spook.tickDelay = (long) delay;
                c.getSource().getEmbedBuilder()
                    .title("Spook Delay Updated!");
                return 1;
            })))
            .then(literal("mode")
                      .then(literal("nearest").executes(c -> {
                          CONFIG.client.extra.spook.spookTargetingMode = Config.Client.Extra.Spook.TargetingMode.NEAREST;
                          c.getSource().getEmbedBuilder()
                              .title("Spook Mode Updated!");
                      }))
                      .then(literal("visualrange").executes(c -> {
                          CONFIG.client.extra.spook.spookTargetingMode = Config.Client.Extra.Spook.TargetingMode.VISUAL_RANGE;
                          c.getSource().getEmbedBuilder()
                              .title("Spook Mode Updated!");
                      })));
    }

    @Override
    public void postPopulate(final EmbedCreateSpec.Builder builder) {
        builder
            .addField("Spook", toggleStr(CONFIG.client.extra.spook.enabled), false)
            .addField("Delay", "" + CONFIG.client.extra.spook.tickDelay + " tick(s)", false)
            .addField("Mode", CONFIG.client.extra.spook.spookTargetingMode.toString().toLowerCase(), false)
            .color(Color.CYAN);
    }
}
