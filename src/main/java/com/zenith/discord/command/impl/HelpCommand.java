package com.zenith.discord.command.impl;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.discord.command.Command;
import com.zenith.discord.command.CommandContext;
import com.zenith.discord.command.CommandUsage;
import discord4j.rest.util.Color;

import java.util.Optional;
import java.util.stream.Collectors;

import static com.mojang.brigadier.arguments.StringArgumentType.string;
import static com.zenith.util.Constants.CONFIG;

public class HelpCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.simple(
                "help",
                "Proxy command list");
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("help").executes(c -> {
                    c.getSource().getEmbedBuilder()
                            .title("Proxy Commands")
                            .color(Color.CYAN);
                    final String brigadierCommands = c.getSource().getCommandManager().getCommands().stream()
                            .sorted((c1, c2) -> c1.commandUsage().getName().compareToIgnoreCase(c2.commandUsage().getName()))
                            .map(command -> command.commandUsage().shortSerialize())
                            .collect(Collectors.joining("\n"));
                    c.getSource().getEmbedBuilder()
                            .description("**More info:** \n  " + CONFIG.discord.prefix + "help <command>\n\n**Command List**\n" + brigadierCommands);
                })
                .then(argument("commandName", string()).executes(c -> {
                    c.getSource().getEmbedBuilder()
                            .title("Command Usage")
                            .color(Color.CYAN);
                    final String commandName = StringArgumentType.getString(c, "commandName");
                    final Optional<Command> foundCommand = c.getSource().getCommandManager().getCommands().stream()
                            .filter(command -> command.commandUsage().getName().equalsIgnoreCase(commandName)
                                    || command.commandUsage().getAliases().stream().anyMatch(a -> a.equalsIgnoreCase(commandName)))
                            .findFirst();
                    if (foundCommand.isPresent()) {
                        c.getSource().getEmbedBuilder().description(foundCommand.get().commandUsage().serialize());
                    } else {
                        c.getSource().getEmbedBuilder().description("Unknown command");
                    }
                    return 1;
                }));
    }


}
