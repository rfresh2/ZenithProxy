package com.zenith.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.command.api.Command;
import com.zenith.command.api.CommandCategory;
import com.zenith.command.api.CommandContext;
import com.zenith.command.api.CommandUsage;

import static com.mojang.brigadier.arguments.DoubleArgumentType.doubleArg;
import static com.mojang.brigadier.arguments.DoubleArgumentType.getDouble;
import static com.zenith.Globals.CONFIG;

public class TickRateCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.builder()
            .name("tickRate")
            .category(CommandCategory.MODULE)
            .description("""
              Modifies the client tick rate, as a multiple of the default rate (20 ticks per second)

              Example:

              * 1.0 would be 20 tps
              * 0.5 would slow down the tickrate to 10 tps
              * 2.0 would speed up the tickrate to 40 tps
              """)
            .usageLines(
                "<rate>"
            )
            .build();
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("tickRate")
            .then(argument("rate", doubleArg(0)).executes(c -> {
                CONFIG.client.tickRate = getDouble(c, "rate");;
                c.getSource().getEmbed()
                    .title("Tick Rate Set");
            }));
    }

    @Override
    public void defaultHandler(final CommandContext c) {
        c.getEmbed()
            .addField("Tick Rate", CONFIG.client.tickRate)
            .primaryColor();
    }
}
