package com.zenith.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.Proxy;
import com.zenith.command.Command;
import com.zenith.command.CommandUsage;
import com.zenith.command.brigadier.CommandCategory;
import com.zenith.command.brigadier.CommandContext;

import java.time.Duration;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static com.zenith.Shared.EXECUTOR;
import static com.zenith.command.brigadier.CustomStringArgumentType.getString;
import static com.zenith.command.brigadier.CustomStringArgumentType.wordWithChars;

public class ScheduleConnectCommand extends Command {

    ScheduledFuture<?> task;

    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.args(
                "schedule",
                CommandCategory.CORE,
                "Schedules to connect the player to the server",
                List.of("<time until proxy connects> ")
        );
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("schedule")
                .then(argument("delay", wordWithChars()).executes(c -> {
                    Duration delay = null;
                    try {
                        delay = Duration.parse("PT" + getString(c, "delay"));
                    } catch (Exception ignored) {}

                    if (delay == null) {
                        c.getSource().getEmbed()
                                .title("invalid delay")
                                .errorColor();
                        return ERROR;
                    }
                    Runnable newTask = () -> EXECUTOR.execute(Proxy.getInstance()::connectAndCatchExceptions);

                    if (this.task != null) task.cancel(true);
                    this.task = EXECUTOR.schedule(newTask, delay.getSeconds(), TimeUnit.SECONDS);

                    LocalTime connectTime = LocalTime.now().plusSeconds(delay.getSeconds());
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss z");
                    ZoneId zoneId = ZoneId.systemDefault();
                    String formattedTime = connectTime.atDate(java.time.LocalDate.now(zoneId)).atZone(zoneId).format(formatter);
                    c.getSource().getEmbed()
                            .title("Scheduled to Connect")
                            .addField("at", formattedTime, true);

                    return 1;
                }));
    }
}
