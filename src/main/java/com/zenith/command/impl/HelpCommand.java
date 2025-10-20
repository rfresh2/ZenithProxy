package com.zenith.command.impl;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.command.api.*;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.mojang.brigadier.arguments.StringArgumentType.string;
import static com.zenith.Globals.COMMAND;

public class HelpCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.builder()
            .name("help")
            .category(CommandCategory.CORE)
            .description("ZenithProxy command list")
            .usageLines(
                "",
                "<category>",
                "<command>"
            )
            .aliases("h")
            .build();
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("help").executes(c -> {
                c.getSource().getEmbed()
                    .title("Commands")
                    .primaryColor();
                final String commandUsages = getCommandUsages(c.getSource().getSource(), CommandCategory.CORE);
                final String prefix = c.getSource().getSource().commandPrefix();
                c.getSource().getEmbed()
                    .description("[Commands Wiki](https://wiki.2b2t.vc/Commands)\n\n"
                                     + "**More Info:** "
                                     + "\n  `" + prefix + "help <command>` or `" + prefix + "help <category>`"
                                     + "\n\n**Categories**\n"
                                     + Arrays.stream(CommandCategory.values())
                                            .map(CommandCategory::getName)
                                            .collect(Collectors.joining(", "))
                                     + "\n"
                                     + "\n**Core Commands**"
                                     + "\n" + commandUsages
                    );
            })
            .then(argument("commandName", string()).executes(c -> {
                final String commandName = StringArgumentType.getString(c, "commandName");
                c.getSource().getEmbed()
                    .title("Command Usage")
                    .primaryColor();
                Arrays.stream(CommandCategory.values())
                    .filter(category -> category.getName().equalsIgnoreCase(commandName))
                    .findFirst()
                    .ifPresentOrElse(
                        category -> populateCategory(c.getSource(), category),
                        () -> populateCommand(c.getSource(), commandName));
                return OK;
            }));
    }

    private String getCommandUsages(final CommandSource src, final CommandCategory category) {
        return COMMAND.getCommands(category).stream()
            .sorted((c1, c2) -> c1.commandUsage().getName().compareToIgnoreCase(c2.commandUsage().getName()))
            .map(command -> command.commandUsage().shortSerializeButNoWikiFooter(src))
            .collect(Collectors.joining("\n"));
    }

    private void populateCategory(final CommandContext c, final CommandCategory category) {
        final String commandUsages = getCommandUsages(c.getSource(), category);
        final String prefix = c.getSource().commandPrefix();
        c.getEmbed()
            .description("[Commands Wiki](https://wiki.2b2t.vc/Commands)\n\n"
                             + "**More Info:** "
                             + "\n  `" + prefix + "help <command>` or `" + prefix + "help <category>`"
                             + "\n"
                             + "\n**" + category.getName() + " Commands**"
                             + "\n" + commandUsages
            );
    }

    private void populateCommand(final CommandContext c, final String commandName) {
        final Optional<Command> foundCommand = COMMAND.getCommands().stream()
            .filter(command -> command.commandUsage().getName().equalsIgnoreCase(commandName)
                || command.commandUsage().getAliases().stream().anyMatch(a -> a.equalsIgnoreCase(commandName)))
            .findFirst();
        if (foundCommand.isPresent()) {
            c.getEmbed().description(foundCommand.get().commandUsage().serialize(c.getSource()));
        } else {
            c.getEmbed().description("Unknown command or category");
        }
    }
}
