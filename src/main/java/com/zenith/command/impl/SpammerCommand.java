package com.zenith.command.impl;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.command.Command;
import com.zenith.command.CommandUsage;
import com.zenith.command.brigadier.CommandCategory;
import com.zenith.command.brigadier.CommandContext;
import com.zenith.discord.Embed;
import com.zenith.module.impl.Spammer;

import java.util.ArrayList;
import java.util.List;

import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.mojang.brigadier.arguments.StringArgumentType.greedyString;
import static com.zenith.Shared.CONFIG;
import static com.zenith.Shared.MODULE;
import static com.zenith.command.brigadier.ToggleArgumentType.getToggle;
import static com.zenith.command.brigadier.ToggleArgumentType.toggle;
import static java.util.Arrays.asList;

public class SpammerCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.full("spammer",
                                 CommandCategory.MODULE,
                                 "Spams messages",
                                 asList(
                                     "on/off",
                                     "whisper on/off",
                                     "delayTicks <int>",
                                     "randomOrder on/off",
                                     "appendRandom on/off",
                                     "list",
                                     "clear",
                                     "add <message>",
                                     "addAt <index> <message>",
                                     "del <index>"),
                                 asList("spam")
        );
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("spammer")
            .then(argument("toggle", toggle()).executes(c -> {
                CONFIG.client.extra.spammer.enabled = getToggle(c, "toggle");
                MODULE.get(Spammer.class).syncEnabledFromConfig();
                c.getSource().getEmbed()
                    .title("Spammer " + toggleStrCaps(CONFIG.client.extra.spammer.enabled));
                return 1;
            }))
            .then(literal("whisper")
                      .then(argument("toggle", toggle()).executes(c -> {
                            CONFIG.client.extra.spammer.whisper = getToggle(c, "toggle");
                            c.getSource().getEmbed()
                                .title("Whisper " + toggleStrCaps(CONFIG.client.extra.spammer.whisper));
                            return 1;
                      })))
            .then(literal("delayTicks")
                      .then(argument("delayTicks", integer(1, 10000)).executes(c -> {
                          CONFIG.client.extra.spammer.delayTicks = IntegerArgumentType.getInteger(c, "delayTicks");
                          c.getSource().getEmbed()
                              .title("Delay Updated!");
                          return 1;
                      })))
            .then(literal("randomOrder")
                      .then(argument("toggle", toggle()).executes(c -> {
                            CONFIG.client.extra.spammer.randomOrder = getToggle(c, "toggle");
                            c.getSource().getEmbed()
                                .title("Random Order " + toggleStrCaps(CONFIG.client.extra.spammer.randomOrder));
                            return 1;
                      })))
            .then(literal("appendRandom")
                      .then(argument("toggle", toggle()).executes(c -> {
                            CONFIG.client.extra.spammer.appendRandom = getToggle(c, "toggle");
                            c.getSource().getEmbed()
                                .title("Append Random " + toggleStrCaps(CONFIG.client.extra.spammer.appendRandom));
                            return 1;
                      })))
            .then(literal("list").executes(c -> {
                c.getSource().getEmbed()
                    .title("Status");
                return 1;
            }))
            .then(literal("clear").executes(c -> {
                CONFIG.client.extra.spammer.messages.clear();
                c.getSource().getEmbed()
                    .title("Messages Cleared!");
                return 1;
            }))
            .then(literal("add")
                      .then(argument("message", greedyString()).executes(c -> {
                          final String message = StringArgumentType.getString(c, "message");
                          CONFIG.client.extra.spammer.messages.add(message);
                          c.getSource().getEmbed()
                                                 .primaryColor()
                                                 .title("Message Added!");
                          return 1;
                      })))
            .then(literal("addAt")
                      .then(argument("index", integer(0))
                                .then(argument("message", greedyString()).executes(c -> {
                                    final int index = IntegerArgumentType.getInteger(c, "index");
                                    final String message = StringArgumentType.getString(c, "message");
                                    try {
                                        CONFIG.client.extra.spammer.messages.add(index, message);
                                        c.getSource().getEmbed()
                                            .title("Message Added!");
                                        return 1;
                                    } catch (final Exception e) {
                                        c.getSource().getEmbed()
                                            .title("Invalid Index!");
                                        return -1;
                                    }
                                }))))
            .then(literal("del")
                      .then(argument("index", integer(0)).executes(c -> {
                          final int index = IntegerArgumentType.getInteger(c, "index");
                          try {
                              CONFIG.client.extra.spammer.messages.remove(index);
                              addListDescription(c.getSource().getEmbed()
                                                     .title("Message Removed!"));
                              return 1;
                          } catch (final Exception e) {
                                c.getSource().getEmbed()
                                    .title("Invalid Index!");
                                return -1;
                          }
                      })));
    }

    @Override
    public void postPopulate(final Embed builder) {
        addListDescription(builder)
            .addField("Spammer", toggleStr(CONFIG.client.extra.spammer.enabled), false)
            .addField("Whisper", toggleStr(CONFIG.client.extra.spammer.whisper), false)
            .addField("Delay", CONFIG.client.extra.spammer.delayTicks, false)
            .addField("Random Order", toggleStr(CONFIG.client.extra.spammer.randomOrder), false)
            .addField("Append Random", toggleStr(CONFIG.client.extra.spammer.appendRandom), false)
            .primaryColor();
    }

    private Embed addListDescription(final Embed embedBuilder) {
        final List<String> messages = new ArrayList<>();
        for (int index = 0; index < CONFIG.client.extra.spammer.messages.size(); index++) {
            messages.add("`" + index + ":` " + CONFIG.client.extra.spammer.messages.get(index));
        }
        return embedBuilder.description(String.join("\n", messages));
    }
}
