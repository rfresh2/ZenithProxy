package com.zenith.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.Proxy;
import com.zenith.command.Command;
import com.zenith.command.CommandUsage;
import com.zenith.command.brigadier.CommandCategory;
import com.zenith.command.brigadier.CommandContext;
import com.zenith.discord.Embed;

import static com.zenith.Shared.CACHE;
import static com.zenith.Shared.CONFIG;
import static com.zenith.command.brigadier.CustomStringArgumentType.wordWithChars;
import static com.zenith.command.brigadier.ToggleArgumentType.getToggle;
import static com.zenith.command.brigadier.ToggleArgumentType.toggle;
import static java.util.Arrays.asList;

public class CommandConfigCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.args(
            "commandConfig",
            CommandCategory.MANAGE,
            """
            Configures ZenithProxy command prefixes and settings.
            """,
            asList(
                "discord prefix <string>",
                "ingame on/off",
                "ingame slashCommands on/off",
                "ingame slashCommands replaceServerCommands on/off",
                "ingame prefix <string>"
                // todo: might add command to config these at some point. But I think these should always be on
//                "ingame logToDiscord on/off",
//                "terminal logToDiscord on/off"
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
                                return ERROR;
                            } else {
                                CONFIG.discord.prefix = newPrefix;
                                c.getSource().getEmbed()
                                    .title("Command Config")
                                    .description("Set discord prefix to " + CONFIG.discord.prefix);
                                return OK;
                            }
                        }))))
            .then(literal("ingame")
                      .then(argument("toggle", toggle()).executes(c -> {
                          CONFIG.inGameCommands.enable = getToggle(c, "toggle");
                          c.getSource().getEmbed()
                              .title("In Game Commands " + toggleStrCaps(CONFIG.inGameCommands.enable));
                          return OK;
                      }))
                      .then(literal("slashCommands")
                                .then(argument("toggle", toggle()).executes(c -> {
                                    CONFIG.inGameCommands.slashCommands = getToggle(c, "toggle");
                                    c.getSource().getEmbed()
                                        .title("In Game Slash Commands " + toggleStrCaps(CONFIG.inGameCommands.slashCommands));
                                    syncSlashCommandsToCurrentPlayer();
                                    return OK;
                                }))
                                .then(literal("replaceServerCommands")
                                          .then(argument("toggle", toggle()).executes(c -> {
                                              CONFIG.inGameCommands.slashCommandsReplacesServerCommands = getToggle(c, "toggle");
                                              c.getSource().getEmbed()
                                                  .title("Replace Server Commands " + toggleStrCaps(CONFIG.inGameCommands.slashCommandsReplacesServerCommands));
                                              syncSlashCommandsToCurrentPlayer();
                                              return OK;
                                          }))))
                      .then(literal("prefix")
                                .then(argument("prefix", wordWithChars())
                                          .executes(c -> {
                                              final String newPrefix = c.getArgument("prefix", String.class);
                                              if (newPrefix.length() > 1) {
                                                  c.getSource().getEmbed()
                                                      .title("Error")
                                                      .description("Prefix must be a single character");
                                                  return ERROR;
                                              } else {
                                                  CONFIG.inGameCommands.prefix = newPrefix;
                                                  c.getSource().getEmbed()
                                                      .title("Command Config")
                                                      .description("Set ingame prefix to " + CONFIG.inGameCommands.prefix);
                                                  return OK;
                                              }
                                          }))));
    }

    private static void syncSlashCommandsToCurrentPlayer() {
        var session = Proxy.getInstance().getCurrentPlayer().get();
        if (session != null) {
            CACHE.getChatCache().getPackets(session::sendAsync);
        }
    }

    @Override
    public void postPopulate(final Embed builder) {
        builder
            .addField("Discord Prefix", CONFIG.discord.prefix, false)
            .addField("Ingame Commands", toggleStr(CONFIG.inGameCommands.enable), false)
            .addField("Ingame Slash Commands", toggleStr(CONFIG.inGameCommands.slashCommands), false)
            .addField("Ingame Slash Commands Replace Server Commands", toggleStr(CONFIG.inGameCommands.slashCommandsReplacesServerCommands), false)
            .addField("Ingame Prefix", CONFIG.inGameCommands.prefix, false)
            .primaryColor();
    }
}
