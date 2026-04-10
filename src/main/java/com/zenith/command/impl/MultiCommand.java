package com.zenith.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.command.api.*;
import com.zenith.discord.Embed;

import java.util.stream.Collectors;

import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.greedyString;
import static com.zenith.Globals.COMMAND;
import static java.util.Arrays.asList;

public class MultiCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.builder()
            .name("multi")
            .category(CommandCategory.MODULE)
            .description("""
              Execute multiple commands one after another

              Each command is separated by a double comma: `,,`

              The contained commands do not have a prefix.

              Example: `multi friend add rfresh2,,say hello,,pearlLoader load rfresh2`
              """)
            .usageLines(
                "<command1>,,<command2>..."
            )
            .aliases(
                "x"
            )
            .build();
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("multi")
            .then(argument("commands", greedyString()).executes(c -> {
                var commandsList = asList(getString(c, "commands").split(",,"));
                for (int i = 0; i < commandsList.size(); i++) {
                    final var command = commandsList.get(i);
                    CommandContext ctx;
                    if (c.getSource() instanceof DiscordCommandContext dc) {
                        ctx = DiscordCommandContext.create(command, dc.getMessageReceivedEvent());
                    } else {
                        ctx = CommandContext.create(command, c.getSource().getSource());
                        if (c.getSource().getInGamePlayerInfo() != null) {
                            ctx.setInGamePlayerInfo(c.getSource().getInGamePlayerInfo());
                        }
                    }
                    c.getSource().getSource().logEmbed(c.getSource(), Embed.builder()
                        .title("Executing Command " + (i+1))
                        .addField("Command", "`" + command + "`")
                    );
                    COMMAND.execute(ctx);
                    if (!ctx.isNoOutput() && !ctx.getEmbed().isTitlePresent() && ctx.getMultiLineOutput().isEmpty()) {
                        c.getSource().getSource().logEmbed(c.getSource(), Embed.builder()
                            .title("MultiCommand Error")
                            .addField("Error", "Unknown Command")
                            .addField("Command", "`" + command + "`"));
                    } else {
                        c.getSource().getSource().logEmbed(c.getSource(), ctx.getEmbed());
                        c.getSource().getSource().logMultiLine(ctx.getMultiLineOutput());
                    }
                }
                c.getSource().getEmbed()
                    .title("Commands Executed")
                    .description(
                        commandsList.stream().map(s -> "`" + s + "`\n").collect(Collectors.joining())
                    )
                    .primaryColor();
            }));
    }
}
