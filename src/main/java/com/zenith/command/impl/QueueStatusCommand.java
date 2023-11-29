package com.zenith.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.Proxy;
import com.zenith.command.Command;
import com.zenith.command.CommandCategory;
import com.zenith.command.CommandContext;
import com.zenith.command.CommandUsage;
import com.zenith.feature.queue.Queue;
import com.zenith.feature.queue.QueueStatus;
import com.zenith.util.math.MathHelper;
import discord4j.rest.util.Color;

import java.time.Duration;
import java.time.Instant;

import static java.util.Arrays.asList;

public class QueueStatusCommand extends Command {

    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.simpleAliases("queueStatus",
                                          CommandCategory.INFO,
                                          "Prints the current 2b2t queue status",
                                          asList("queue", "q")
        );
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("queueStatus").executes(c -> {
            final QueueStatus queueStatus = Queue.getQueueStatus();
            c.getSource().getEmbedBuilder()
                .title("2b2t Queue Status")
                .addField("Regular", ""+queueStatus.regular(), false)
                .addField("Priority", ""+queueStatus.prio(), false)
                .color(Color.CYAN);
            if (Proxy.getInstance().isInQueue()) {
                final int queuePosition = Proxy.getInstance().getQueuePosition();
                final Duration currentWaitDuration = Duration.between(Proxy.getInstance().getConnectTime(), Instant.now());
                c.getSource().getEmbedBuilder()
                    .addField("Position", ""+ queuePosition, false)
                    .addField("ETA", ""+Queue.getQueueEta(queuePosition), false)
                    .addField("Current Wait Duration", MathHelper.formatDuration(currentWaitDuration), false);
            }
        });
    }
}
