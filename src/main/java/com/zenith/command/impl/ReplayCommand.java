package com.zenith.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.command.Command;
import com.zenith.command.CommandCategory;
import com.zenith.command.CommandContext;
import com.zenith.command.CommandUsage;
import com.zenith.module.impl.ReplayMod;
import discord4j.rest.util.Color;

import static com.zenith.Shared.MODULE_MANAGER;
import static java.util.Arrays.asList;

public class ReplayCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.args(
            "replay",
            CommandCategory.MODULE,
            "Captures a ReplayMod recording",
            asList(
                "start",
                "stop"
            )
        );
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("replay")
            .then(literal("start").executes(c -> {
                // start recording
                var module = MODULE_MANAGER.get(ReplayMod.class);
                if (module.isEnabled()) {
                    c.getSource().getEmbed()
                        .title("Error")
                        .color(Color.RUBY)
                        .description("ReplayMod is already recording");
                    return 1;
                }
                module.enable();
                c.getSource().getEmbed()
                    .title("Recording Started")
                    .color(Color.CYAN);
                return 1;
            }))
            .then(literal("stop").executes(c -> {
                var module = MODULE_MANAGER.get(ReplayMod.class);
                if (!module.isEnabled()) {
                    c.getSource().getEmbed()
                        .title("Error")
                        .color(Color.RUBY)
                        .description("ReplayMod is not recording");
                    return 1;
                }
                module.disable();
                c.getSource().getEmbed()
                    .title("Recording Stopped")
                    .color(Color.CYAN);
                return 1;
            }));
    }
}
