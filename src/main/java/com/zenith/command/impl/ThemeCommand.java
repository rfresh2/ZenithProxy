package com.zenith.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.command.Command;
import com.zenith.command.CommandUsage;
import com.zenith.command.brigadier.CommandCategory;
import com.zenith.command.brigadier.CommandContext;
import com.zenith.discord.Embed;
import com.zenith.util.ConfigColor;

import java.util.Arrays;

import static com.zenith.Shared.CONFIG;
import static com.zenith.command.brigadier.CustomStringArgumentType.getString;
import static com.zenith.command.brigadier.CustomStringArgumentType.wordWithChars;
import static java.util.Arrays.asList;

public class ThemeCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.full(
            "theme",
            CommandCategory.MANAGE,
            """
            Changes the color theme of alerts and messages.
            
            Use `theme list` to see available colors.
            
            Where Colors Are Used:
              * Primary: Most embeds and command responses if not an error.
              * Success: General "this worked" responses, server join, and friends
              * Error: Error responses, server leave, and enemies
              * In Queue: The proxy is in queue, reconnecting, or is in a transitional state
            """,
            asList(
               "list",
               "primary <color>",
               "success <color>",
               "error <color>",
               "inQueue <color>"
            ),
            asList("color")
        );
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("theme")
            .then(literal("list").executes(c -> {
                var allColors = Arrays.stream(ConfigColor.values())
                    .map(color -> color.name().toLowerCase())
                    .toList();
                c.getSource().getEmbed()
                    .title("Available Colors")
                    .description(String.join("\n", allColors));
            }))
            .then(literal("primary").then(argument("color", wordWithChars()).executes(c -> {
                var colorStr = getString(c, "color").toUpperCase();
                try {
                    CONFIG.theme.primary = ConfigColor.valueOf(colorStr);
                    c.getSource().getEmbed()
                        .title("Primary Color Set!");
                    return 1;
                } catch (final Throwable e) {
                    c.getSource().getEmbed()
                        .title("Invalid Color!")
                        .addField("Help", "Use `theme list` to see available colors", false);
                    return ERROR;
                }
            })))
            .then(literal("success").then(argument("color", wordWithChars()).executes(c -> {
                var colorStr = getString(c, "color").toUpperCase();
                try {
                    CONFIG.theme.success = ConfigColor.valueOf(colorStr);
                    c.getSource().getEmbed()
                        .title("Success Color Set!");
                    return 1;
                } catch (final Throwable e) {
                    c.getSource().getEmbed()
                        .title("Invalid Color!")
                        .addField("Help", "Use `theme list` to see available colors", false);
                    return ERROR;
                }
            })))
            .then(literal("error").then(argument("color", wordWithChars()).executes(c -> {
                var colorStr = getString(c, "color").toUpperCase();
                try {
                    CONFIG.theme.error = ConfigColor.valueOf(colorStr);
                    c.getSource().getEmbed()
                        .title("Error Color Set!");
                    return 1;
                } catch (final Throwable e) {
                    c.getSource().getEmbed()
                        .title("Invalid Color!")
                        .addField("Help", "Use `theme list` to see available colors", false);
                    return ERROR;
                }
            })))
            .then(literal("inQueue").then(argument("color", wordWithChars()).executes(c -> {
                var colorStr = getString(c, "color").toUpperCase();
                try {
                    CONFIG.theme.inQueue = ConfigColor.valueOf(colorStr);
                    c.getSource().getEmbed()
                        .title("In Queue Color Set!");
                    return 1;
                } catch (final Throwable e) {
                    c.getSource().getEmbed()
                        .title("Invalid Color!")
                        .addField("Help", "Use `theme list` to see available colors", false);
                    return ERROR;
                }
            })));
    }

    @Override
    public void postPopulate(Embed embed) {
        embed
            .primaryColor()
            .addField("Primary", CONFIG.theme.primary.name().toLowerCase(), false)
            .addField("Success", CONFIG.theme.success.name().toLowerCase(), false)
            .addField("Error", CONFIG.theme.error.name().toLowerCase(), false)
            .addField("In Queue", CONFIG.theme.inQueue.name().toLowerCase(), false);
    }
}
