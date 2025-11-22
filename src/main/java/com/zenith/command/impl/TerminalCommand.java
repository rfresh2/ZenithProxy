package com.zenith.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.command.api.*;

import static com.zenith.Globals.CONFIG;
import static com.zenith.command.brigadier.ToggleArgumentType.getToggle;
import static com.zenith.command.brigadier.ToggleArgumentType.toggle;

public class TerminalCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.builder()
            .name("terminal")
            .category(CommandCategory.MANAGE)
            .description("""
                Configures the ZenithProxy interactive terminal.

                All subcommands only usable from the terminal.
                """)
            .usageLines(
                "autoCompletions",
                "logToDiscord on/off"
            )
            .build();
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("terminal").requires(ctx -> Command.validateCommandSource(ctx, CommandSources.TERMINAL))
            .then(literal("autoCompletions").then(argument("toggle", toggle()).executes(c -> {
                CONFIG.interactiveTerminal.alwaysOnCompletions = getToggle(c, "toggle");
                c.getSource().getEmbed()
                    .title("AutoCompletions " + toggleStrCaps(CONFIG.interactiveTerminal.alwaysOnCompletions))
                    .addField("Info", "Changes will take effect on next `restart`");
            })))
            .then(literal("logToDiscord").then(argument("toggle", toggle()).executes(c -> {
                CONFIG.interactiveTerminal.logToDiscord = getToggle(c, "toggle");
                c.getSource().getEmbed()
                    .title("Log To Discord " + toggleStrCaps(CONFIG.interactiveTerminal.logToDiscord));
            })));
    }


    @Override
    public void defaultHandler(final CommandContext ctx) {
        ctx.getEmbed()
            .addField("AutoCompletions", CONFIG.interactiveTerminal.alwaysOnCompletions)
            .addField("Log To Discord", CONFIG.interactiveTerminal.logToDiscord)
            .primaryColor();
    }
}
