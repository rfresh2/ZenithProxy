package com.zenith.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.Proxy;
import com.zenith.command.api.Command;
import com.zenith.command.api.CommandCategory;
import com.zenith.command.api.CommandContext;
import com.zenith.command.api.CommandUsage;
import com.zenith.discord.Embed;
import com.zenith.feature.queue.Queue;
import com.zenith.feature.queue.QueueStatus;
import com.zenith.util.math.MathHelper;

import java.time.Duration;

import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.zenith.Globals.CONFIG;
import static com.zenith.command.brigadier.ToggleArgumentType.getToggle;
import static com.zenith.command.brigadier.ToggleArgumentType.toggle;

public class QueueStatusCommand extends Command {

    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.builder()
            .name("queueStatus")
            .category(CommandCategory.INFO)
            .description("Gets the current 2b2t queue length and wait ETA")
            .usageLines(
                "",
                "refresh",
                "refresh interval <minutes>",
                "refresh eta on/off",
                "refresh whileNotOn2b2t on/off"
            )
            .aliases(
                "queue",
                "q"
            )
            .build();
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("queueStatus").executes(c -> {
                final boolean inQueue = Proxy.getInstance().isInQueue();
                final QueueStatus queueStatus = Queue.getQueueStatus();
                c.getSource().getEmbed()
                    .title("2b2t Queue Status")
                    .addField("Regular", queueStatus.regular() + (inQueue ? "" : " [ETA: " + Queue.getQueueEta(queueStatus.regular()) + "]"), false)
                    .addField("Priority", queueStatus.prio(), false)
                    .primaryColor();
                if (inQueue) {
                    final int queuePosition = Proxy.getInstance().getQueuePosition();
                    final Duration currentWaitDuration = Duration.ofSeconds(Proxy.getInstance().getOnlineTimeSeconds());
                    c.getSource().getEmbed()
                        .addField("Position", queuePosition + " [ETA: " + Queue.getQueueEta(queuePosition) + "]", false)
                        .addField("Current Wait Duration", MathHelper.formatDuration(currentWaitDuration), false);
                }})
            .then(literal("refresh")
                .executes(c -> {
                    try {
                        Queue.updateQueueStatusNow();
                        Queue.updateQueueEtaEquation();
                    } catch (final Throwable e) {
                        c.getSource().getEmbed()
                            .title("Error")
                            .description("Failed to refresh queue status\n" + e.getMessage())
                            .errorColor();
                        return;
                    }
                    c.getSource().getEmbed()
                        .title("Success")
                        .description("Queue status refreshed")
                        .successColor();
                })
                .then(literal("interval").then(argument("mins", integer(1)).executes(c -> {
                    CONFIG.server.queueStatusRefreshMinutes = getInteger(c, "mins");
                    settingsEmbed(c.getSource().getEmbed())
                        .title("Refresh Interval Set");
                })))
                .then(literal("eta").then(argument("toggle", toggle()).executes(c -> {
                    CONFIG.server.dynamicQueueEtaEquation = getToggle(c, "toggle");
                    settingsEmbed(c.getSource().getEmbed())
                        .title("Refresh ETA Set");
                })))
                .then(literal("whileNotOn2b2t").then(argument("toggle", toggle()).executes(c -> {
                    CONFIG.server.queueStatusRefreshWhileNotOn2b2t = getToggle(c, "toggle");
                    settingsEmbed(c.getSource().getEmbed())
                        .title("Refresh While Not On 2b2t Set");
                }))));
    }

    private Embed settingsEmbed(Embed embed) {
        return embed
            .addField("Refresh Interval", CONFIG.server.queueStatusRefreshMinutes + " mins")
            .addField("Dynamic ETA Equation", toggleStr(CONFIG.server.dynamicQueueEtaEquation))
            .addField("Refresh While Not On 2b2t", toggleStr(CONFIG.server.queueStatusRefreshWhileNotOn2b2t));
    }
}
