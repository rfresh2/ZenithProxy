package com.zenith.command.impl;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.command.api.Command;
import com.zenith.command.api.CommandCategory;
import com.zenith.command.api.CommandContext;
import com.zenith.command.api.CommandUsage;
import com.zenith.command.brigadier.CustomStringArgumentType;
import com.zenith.discord.Embed;
import com.zenith.module.impl.ActiveHours;
import com.zenith.module.impl.ActiveHours.ActiveTime;

import java.time.ZoneId;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.zenith.Globals.CONFIG;
import static com.zenith.Globals.MODULE;
import static com.zenith.command.brigadier.CustomStringArgumentType.wordWithChars;
import static com.zenith.command.brigadier.ToggleArgumentType.getToggle;
import static com.zenith.command.brigadier.ToggleArgumentType.toggle;

public class ActiveHoursCommand extends Command {
    private static final Pattern TIME_PATTERN = Pattern.compile("[0-9]{1,2}:[0-9]{2}");

    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.builder()
            .name("activeHours")
            .category(CommandCategory.MODULE)
            .description("""
            Set times for ZenithProxy to automatically connect at.

            By default, 2b2t's queue wait ETA is used to determine when to log in.
            The connect will occur when the current time plus the ETA is equal to a time set.

            If Queue ETA calc is disabled, connects will occur exactly at the set times instead.

             Time zone Ids ("TZ Identifier" column): https://w.wiki/A2fd
             Time format: hh:mm, Examples: 1:42, 14:42, 14:01
            """)
            .usageLines(
                "on/off",
                "timezone <timezone ID>",
                "add/del <time>",
                "status",
                "whilePlayerConnected on/off",
                "queueEtaCalc on/off",
                "fullSessionUntilNextDisconnect on/off"
            )
            .build();
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("activeHours")
            .then(argument("toggle", toggle()).executes(c -> {
                CONFIG.client.extra.utility.actions.activeHours.enabled = getToggle(c, "toggle");
                MODULE.get(ActiveHours.class).syncEnabledFromConfig();
                c.getSource().getEmbed()
                    .title("Active Hours " + toggleStrCaps(CONFIG.client.extra.utility.actions.activeHours.enabled));
                return OK;
            }))
            .then(literal("timezone").then(argument("tz", wordWithChars()).executes(c -> {
                final String timeZoneId = CustomStringArgumentType.getString(c, "tz");
                if (ZoneId.getAvailableZoneIds().stream().noneMatch(id -> id.equals(timeZoneId))) {
                    c.getSource().getEmbed()
                        .title("Invalid Timezone")
                        .addField("Help", "Time zone Ids: https://w.wiki/8Yif", false);
                } else {
                    CONFIG.client.extra.utility.actions.activeHours.timeZoneId = ZoneId.of(timeZoneId).getId();
                    c.getSource().getEmbed()
                        .title("Set timezone: " + timeZoneId);
                }
                return OK;
            })))
            .then(literal("add").then(argument("time", wordWithChars()).executes(c -> {
                final String time = StringArgumentType.getString(c, "time");
                if (!timeMatchesRegex(time)) {
                    c.getSource().getEmbed()
                        .title("Invalid Time Format")
                        .addField("Help", "Time format: XX:XX, e.g.: 1:42, 14:42, 14:01", false);
                    return ERROR;
                } else {
                    final ActiveTime activeTime = ActiveTime.fromString(time);
                    if (!CONFIG.client.extra.utility.actions.activeHours.activeTimes.contains(activeTime)) {
                        CONFIG.client.extra.utility.actions.activeHours.activeTimes.add(activeTime);
                    }
                    c.getSource().getEmbed()
                        .title("Added time: " + time);
                    return OK;
                }
            })))
            .then(literal("del").then(argument("time", wordWithChars()).executes(c -> {
                final String time = StringArgumentType.getString(c, "time");
                if (!timeMatchesRegex(time)) {
                    c.getSource().getEmbed()
                        .title("Invalid Time Format")
                        .addField("Help", "Time format: XX:XX, e.g.: 1:42, 14:42, 14:01", false);
                    return ERROR;
                } else {
                    final ActiveTime activeTime = ActiveTime.fromString(time);
                    CONFIG.client.extra.utility.actions.activeHours.activeTimes.removeIf(s -> s.equals(activeTime));
                    c.getSource().getEmbed()
                        .title("Removed time: " + time);
                    return OK;
                }
            })))
            .then(literal("status").executes(c -> {
                c.getSource().getEmbed()
                    .title("Active Hours Status");
            }))
            .then(literal("whilePlayerConnected")
                .then(argument("toggle", toggle()).executes(c -> {
                    CONFIG.client.extra.utility.actions.activeHours.forceReconnect = getToggle(c, "toggle");
                    c.getSource().getEmbed()
                        .title("Force Reconnect Set!");
                    return OK;
                })))
            .then(literal("queueEtaCalc")
                .then(argument("toggle", toggle()).executes(c -> {
                    CONFIG.client.extra.utility.actions.activeHours.queueEtaCalc = getToggle(c, "toggle");
                    c.getSource().getEmbed()
                        .title("Queue ETA Calc Set!");
                    return OK;
                })))
            .then(literal("fullSessionUntilNextDisconnect").then(argument("toggle", toggle()).executes(c -> {
                CONFIG.client.extra.utility.actions.activeHours.fullSessionUntilNextDisconnect = getToggle(c, "toggle");
                c.getSource().getEmbed()
                    .title("Full Session Until Next Disconnect " + toggleStrCaps(CONFIG.client.extra.utility.actions.activeHours.fullSessionUntilNextDisconnect));
            })));
    }

    private boolean timeMatchesRegex(final String arg) {
        final Matcher matcher = TIME_PATTERN.matcher(arg);
        boolean matchesRegex = matcher.matches();
        if (!matchesRegex) return false;
        final ActiveTime activeTime = ActiveTime.fromString(arg);
        return activeTime.hour() <= 23 && activeTime.minute() <= 59;
    }

    private String activeTimeListToString(final List<ActiveTime> activeTimes) {
        return activeTimes.stream()
            .sorted((a, b) -> {
                if (a.hour() == b.hour()) {
                    return a.minute() - b.minute();
                } else {
                    return a.hour() - b.hour();
                }
            })
            .map(ActiveTime::toString)
            .collect(Collectors.joining(", "));
    }

    @Override
    public void defaultEmbed(Embed builder) {
        builder
            .addField("ActiveHours", toggleStr(CONFIG.client.extra.utility.actions.activeHours.enabled))
            .addField("Time Zone", CONFIG.client.extra.utility.actions.activeHours.timeZoneId)
            .addField("Active Hours", (CONFIG.client.extra.utility.actions.activeHours.activeTimes.isEmpty()
                ? "None set!"
                : activeTimeListToString(CONFIG.client.extra.utility.actions.activeHours.activeTimes)))
            .addField("While Player Connected", toggleStr(CONFIG.client.extra.utility.actions.activeHours.forceReconnect))
            .addField("Queue ETA Calc", toggleStr(CONFIG.client.extra.utility.actions.activeHours.queueEtaCalc))
            .addField("Full Session Until Next Disconnect", toggleStr(CONFIG.client.extra.utility.actions.activeHours.fullSessionUntilNextDisconnect))
            .primaryColor();
    }
}
