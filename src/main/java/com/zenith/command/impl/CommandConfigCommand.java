package com.zenith.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.command.Command;
import com.zenith.command.CommandCategory;
import com.zenith.command.CommandContext;
import com.zenith.command.CommandUsage;
import com.zenith.discord.Embed;
import discord4j.rest.util.Color;

import static com.zenith.Shared.CONFIG;
import static com.zenith.command.CustomStringArgumentType.wordWithChars;
import static com.zenith.command.ToggleArgumentType.getToggle;
import static com.zenith.command.ToggleArgumentType.toggle;
import static java.util.Arrays.asList;

public class CommandConfigCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.args("commandConfig",
                                 CommandCategory.MANAGE,
                                 "Configures settings related to ZenithProxy commands",
                                 asList(
                                     "discord prefix <string>",
                                     "ingame on/off",
                                     "ingame slashCommands on/off",
                                     "ingame prefix <string>"
                                    // todo: might add command to config these at some point. But I think these should always be on
//                                     "ingame logToDiscord on/off",
//                                     "terminal logToDiscord on/off"
                                 ));
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("commandConfig").requires(Command::validateAccountOwner)
            .then(literal("discord")
                .then(literal("prefix")
                    .then(argument("prefix", wordWithChars())
                        .executes(c -> {
                            final String newPrefix = c.getArgument("prefix", String.class);
                            if (newPrefix.length() > 1) {
                                c.getSource().getEmbed()
                                    .title("Error")
                                    .description("Prefix must be a single character");
                                return -1;
                            } else {
                                CONFIG.discord.prefix = newPrefix;
                                c.getSource().getEmbed()
                                    .title("Command Config")
                                    .description("Set discord prefix to " + CONFIG.discord.prefix);
                                return 1;
                            }
                        }))))
            .then(literal("ingame")
                      .then(argument("toggle", toggle()).executes(c -> {
                          CONFIG.inGameCommands.enable = getToggle(c, "toggle");
                          c.getSource().getEmbed()
                              .title("In Game Commands " + (CONFIG.inGameCommands.enable ? "On!" : "Off!"));
                          return 1;
                      }))
                      .then(literal("slashCommands")
                                .then(argument("toggle", toggle()).executes(c -> {
                                    CONFIG.inGameCommands.slashCommands = getToggle(c, "toggle");
                                    c.getSource().getEmbed()
                                        .title("In Game Slash Commands " + (CONFIG.inGameCommands.slashCommands ? "On!" : "Off!"));
                                    return 1;
                                })))
                      .then(literal("prefix")
                                .then(argument("prefix", wordWithChars())
                                          .executes(c -> {
                                              final String newPrefix = c.getArgument("prefix", String.class);
                                              if (newPrefix.length() > 1) {
                                                  c.getSource().getEmbed()
                                                      .title("Error")
                                                      .description("Prefix must be a single character");
                                                  return -1;
                                              } else {
                                                  CONFIG.inGameCommands.prefix = newPrefix;
                                                  c.getSource().getEmbed()
                                                      .title("Command Config")
                                                      .description("Set ingame prefix to " + CONFIG.inGameCommands.prefix);
                                                  return 1;
                                              }
                                          }))));
    }

    @Override
    public void postPopulate(final Embed builder) {
        builder
            .addField("Discord Prefix", CONFIG.discord.prefix, false)
            .addField("Ingame Commands", toggleStr(CONFIG.inGameCommands.enable), false)
            .addField("Ingame Prefix", CONFIG.inGameCommands.prefix, false)
            .color(Color.CYAN);
    }
}
