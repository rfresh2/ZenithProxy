package com.zenith.command.impl;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.command.Command;
import com.zenith.command.CommandContext;
import com.zenith.command.CommandUsage;
import com.zenith.discord.DiscordBot;
import com.zenith.module.impl.AutoReply;
import discord4j.rest.util.Color;

import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.mojang.brigadier.arguments.StringArgumentType.greedyString;
import static com.zenith.util.Constants.CONFIG;
import static com.zenith.util.Constants.MODULE_MANAGER;
import static java.util.Arrays.asList;

public class AutoReplyCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.args(
                "autoReply",
                "Configure the AutoReply feature",
                asList("on/off", "cooldown <seconds>", "message <message>")
        );
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("autoReply")
                .then(literal("on").executes(c -> {
                    CONFIG.client.extra.autoReply.enabled = true;
                    c.getSource().getEmbedBuilder()
                            .title("AutoReply On!")
                            .addField("Cooldown Seconds", "" + CONFIG.client.extra.autoReply.cooldownSeconds, false)
                            .addField("Message", CONFIG.client.extra.autoReply.message, false)
                            .color(Color.CYAN);
                }))
                .then(literal("off").executes(c -> {
                    CONFIG.client.extra.autoReply.enabled = false;
                    c.getSource().getEmbedBuilder()
                            .title("AutoReply Off!")
                            .addField("Cooldown Seconds", "" + CONFIG.client.extra.autoReply.cooldownSeconds, false)
                            .addField("Message", CONFIG.client.extra.autoReply.message, false)
                            .color(Color.CYAN);
                }))
                .then(literal("cooldown").then(argument("secs", integer()).executes(c -> {
                    int delay = IntegerArgumentType.getInteger(c, "secs");
                    MODULE_MANAGER.getModule(AutoReply.class).ifPresent(m -> m.updateCooldown(delay));
                    c.getSource().getEmbedBuilder()
                            .title("AutoReply Cooldown Updated!")
                            .addField("Status", (CONFIG.client.extra.autoReply.enabled ? "on" : "off"), false)
                            .addField("Cooldown Seconds", "" + CONFIG.client.extra.autoReply.cooldownSeconds, false)
                            .addField("Message", CONFIG.client.extra.autoReply.message, false)
                            .color(Color.CYAN);
                    return 1;
                })))
                .then(literal("message").then(argument("messageStr", greedyString()).executes(c -> {
                    String message = DiscordBot.sanitizeRelayInputMessage(StringArgumentType.getString(c, "messageStr"));
                    if (message.length() > 236) {
                        message = message.substring(0, 236);
                    }
                    CONFIG.client.extra.autoReply.message = message;
                    c.getSource().getEmbedBuilder()
                            .title("AutoReply Message Updated!")
                            .addField("Status", (CONFIG.client.extra.autoReply.enabled ? "on" : "off"), false)
                            .addField("Cooldown Seconds", "" + CONFIG.client.extra.autoReply.cooldownSeconds, false)
                            .addField("Message", CONFIG.client.extra.autoReply.message, false)
                            .color(Color.CYAN);
                    return 1;
                })));
    }
}
