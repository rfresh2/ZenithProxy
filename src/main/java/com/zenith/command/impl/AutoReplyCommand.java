package com.zenith.command.impl;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.command.Command;
import com.zenith.command.CommandCategory;
import com.zenith.command.CommandContext;
import com.zenith.command.CommandUsage;
import com.zenith.discord.DiscordBot;
import com.zenith.discord.Embed;
import com.zenith.module.Module;
import com.zenith.module.impl.AutoReply;
import discord4j.rest.util.Color;

import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.mojang.brigadier.arguments.StringArgumentType.greedyString;
import static com.zenith.Shared.CONFIG;
import static com.zenith.Shared.MODULE_MANAGER;
import static com.zenith.command.ToggleArgumentType.getToggle;
import static com.zenith.command.ToggleArgumentType.toggle;
import static java.util.Arrays.asList;

public class AutoReplyCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.args(
            "autoReply",
            CommandCategory.MODULE,
            "Configure the AutoReply feature",
            asList("on/off", "cooldown <seconds>", "message <message>")
        );
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("autoReply")
            .then(argument("toggle", toggle()).executes(c -> {
                CONFIG.client.extra.autoReply.enabled = getToggle(c, "toggle");
                MODULE_MANAGER.getModule(AutoReply.class).ifPresent(Module::syncEnabledFromConfig);
                c.getSource().getEmbed()
                    .title("AutoReply " + (CONFIG.client.extra.autoReply.enabled ? "On!" : "Off!"));
                return 1;
            }))
            .then(literal("cooldown").then(argument("secs", integer(0, 1000)).executes(c -> {
                int delay = IntegerArgumentType.getInteger(c, "secs");
                MODULE_MANAGER.getModule(AutoReply.class).ifPresent(m -> m.updateCooldown(delay));
                c.getSource().getEmbed()
                    .title("AutoReply Cooldown Updated!");
                return 1;
            })))
            .then(literal("message").then(argument("messageStr", greedyString()).executes(c -> {
                String message = DiscordBot.sanitizeRelayInputMessage(StringArgumentType.getString(c, "messageStr"));
                if (message.length() > 236)
                    message = message.substring(0, 236);
                CONFIG.client.extra.autoReply.message = message;
                c.getSource().getEmbed()
                    .title("AutoReply Message Updated!");
                return 1;
            })));
    }

    @Override
    public void postPopulate(final Embed builder) {
        builder
            .addField("AutoReply", toggleStr(CONFIG.client.extra.autoReply.enabled), false)
            .addField("Cooldown Seconds", CONFIG.client.extra.autoReply.cooldownSeconds, false)
            .addField("Message", CONFIG.client.extra.autoReply.message, false)
            .color(Color.CYAN);
    }
}
