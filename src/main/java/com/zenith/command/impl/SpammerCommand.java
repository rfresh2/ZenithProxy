package com.zenith.command.impl;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.command.Command;
import com.zenith.command.CommandContext;
import com.zenith.command.CommandUsage;
import com.zenith.module.Module;
import com.zenith.module.impl.Spammer;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;

import java.util.ArrayList;
import java.util.List;

import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.mojang.brigadier.arguments.StringArgumentType.greedyString;
import static com.zenith.Shared.CONFIG;
import static com.zenith.Shared.MODULE_MANAGER;
import static com.zenith.command.ToggleArgumentType.getToggle;
import static com.zenith.command.ToggleArgumentType.toggle;
import static java.util.Arrays.asList;

public class SpammerCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.full("spammer", "Spams messages", asList(
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
                aliases());
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("spammer")
            .then(argument("toggle", toggle()).executes(c -> {
                CONFIG.client.extra.spammer.enabled = getToggle(c, "toggle");
                MODULE_MANAGER.getModule(Spammer.class).ifPresent(Module::syncEnabledFromConfig);
                c.getSource().getEmbedBuilder()
                    .title("Spammer " + (CONFIG.client.extra.spammer.enabled ? "On!" : "Off!"));
                return 1;
            }))
            .then(literal("whisper")
                      .then(argument("toggle", toggle()).executes(c -> {
                            CONFIG.client.extra.spammer.whisper = getToggle(c, "toggle");
                            c.getSource().getEmbedBuilder()
                                .title("Whisper " + (CONFIG.client.extra.spammer.whisper ? "On!" : "Off!"));
                            return 1;
                      })))
            .then(literal("delayticks")
                      .then(argument("delayTicks", integer(1, 10000)).executes(c -> {
                          CONFIG.client.extra.spammer.delayTicks = IntegerArgumentType.getInteger(c, "delayTicks");
                          c.getSource().getEmbedBuilder()
                              .title("Delay Updated!");
                          return 1;
                      })))
            .then(literal("randomorder")
                      .then(argument("toggle", toggle()).executes(c -> {
                            CONFIG.client.extra.spammer.randomOrder = getToggle(c, "toggle");
                            c.getSource().getEmbedBuilder()
                                .title("Random Order " + (CONFIG.client.extra.spammer.randomOrder ? "On!" : "Off!"));
                            return 1;
                      })))
            .then(literal("appendrandom")
                      .then(argument("toggle", toggle()).executes(c -> {
                            CONFIG.client.extra.spammer.appendRandom = getToggle(c, "toggle");
                            c.getSource().getEmbedBuilder()
                                .title("Append Random " + (CONFIG.client.extra.spammer.appendRandom ? "On!" : "Off!"));
                            return 1;
                      })))
            .then(literal("list").executes(c -> {
                c.getSource().getEmbedBuilder()
                    .title("Status");
                return 1;
            }))
            .then(literal("clear").executes(c -> {
                CONFIG.client.extra.spammer.messages.clear();
                c.getSource().getEmbedBuilder()
                    .title("Messages Cleared!");
                return 1;
            }))
            .then(literal("add")
                      .then(argument("message", greedyString()).executes(c -> {
                          final String message = StringArgumentType.getString(c, "message");
                          CONFIG.client.extra.spammer.messages.add(message);
                          c.getSource().getEmbedBuilder()
                                                 .color(Color.CYAN)
                                                 .title("Message Added!");
                          return 1;
                      })))
            .then(literal("addAt")
                      .then(argument("index", integer())
                                .then(argument("message", greedyString()).executes(c -> {
                                    final int index = IntegerArgumentType.getInteger(c, "index");
                                    final String message = StringArgumentType.getString(c, "message");
                                    try {
                                        CONFIG.client.extra.spammer.messages.add(index, message);
                                        c.getSource().getEmbedBuilder()
                                            .title("Message Added!");
                                        return 1;
                                    } catch (final Exception e) {
                                        c.getSource().getEmbedBuilder()
                                            .title("Invalid Index!");
                                        return -1;
                                    }
                                }))))
            .then(literal("del")
                      .then(argument("index", integer()).executes(c -> {
                          final int index = IntegerArgumentType.getInteger(c, "index");
                          try {
                              CONFIG.client.extra.spammer.messages.remove(index);
                              addListDescription(c.getSource().getEmbedBuilder()
                                                     .title("Message Removed!"));
                              return 1;
                          } catch (final Exception e) {
                                c.getSource().getEmbedBuilder()
                                    .title("Invalid Index!");
                                return -1;
                          }
                      })));
    }

    @Override
    public List<String> aliases() {
        return asList("spam");
    }

    @Override
    public void postPopulate(final EmbedCreateSpec.Builder builder) {
        addListDescription(builder)
            .addField("Spammer", toggleStr(CONFIG.client.extra.spammer.enabled), false)
            .addField("Whisper", toggleStr(CONFIG.client.extra.spammer.whisper), false)
            .addField("Delay", "" + CONFIG.client.extra.spammer.delayTicks, false)
            .addField("Random Order", toggleStr(CONFIG.client.extra.spammer.randomOrder), false)
            .addField("Append Random", toggleStr(CONFIG.client.extra.spammer.appendRandom), false)
            .color(Color.CYAN);
    }

    private EmbedCreateSpec.Builder addListDescription(final EmbedCreateSpec.Builder embedBuilder) {
        final List<String> messages = new ArrayList<>();
        for (int index = 0; index < CONFIG.client.extra.spammer.messages.size(); index++) {
            messages.add("`" + index + ":` " + CONFIG.client.extra.spammer.messages.get(index));
        }
        return embedBuilder.description(String.join("\n", messages));
    }
}
