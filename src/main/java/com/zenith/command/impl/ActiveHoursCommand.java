package com.zenith.command.impl;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.command.Command;
import com.zenith.command.CommandUsage;
import com.zenith.command.brigadier.CommandCategory;
import com.zenith.command.brigadier.CommandContext;
import com.zenith.command.brigadier.CustomStringArgumentType;
import com.zenith.discord.Embed;
import com.zenith.module.impl.ActiveHours;
import com.zenith.module.impl.ActiveHours.ActiveTime;

import java.time.ZoneId;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.zenith.Shared.CONFIG;
import static com.zenith.Shared.MODULE;
import static com.zenith.command.brigadier.CustomStringArgumentType.wordWithChars;
import static com.zenith.command.brigadier.ToggleArgumentType.getToggle;
import static com.zenith.command.brigadier.ToggleArgumentType.toggle;
import static java.util.Arrays.asList;

public class ActiveHoursCommand extends Command {
    private static final Pattern TIME_PATTERN = Pattern.compile("[0-9]{1,2}:[0-9]{2}");

    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.full(
            "activeHours",
            CommandCategory.MODULE,
            """
            Set active hours for the proxy to automatically be logged in at.
            
            By default, 2b2t's queue wait ETA is used to determine when to log in.
            The connect will occur when the current time plus the ETA is equal to a time set.
            
            If Queue ETA calc is disabled, connects will occur exactly at the set times instead.
            
             Time zone Ids ("TZ Identifier" column): https://w.wiki/A2fd
             Time format: XX:XX, e.g.: 1:42, 14:42, 14:01
            """,
            asList(
                "on/off",
                "timezone <timezone ID>",
                "add/del <time>",
                "status",
                "forceReconnect on/off",
                "queueEtaCalc on/off"
            ),
            asList(
                "schedule"
            )
        );
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
            .then(literal("forceReconnect")
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
    public void postPopulate(Embed builder) {
        builder
            .addField("ActiveHours", toggleStr(CONFIG.client.extra.utility.actions.activeHours.enabled), false)
            .addField("Time Zone", CONFIG.client.extra.utility.actions.activeHours.timeZoneId, false)
            .addField("Active Hours", (CONFIG.client.extra.utility.actions.activeHours.activeTimes.isEmpty()
                ? "None set!"
                : activeTimeListToString(CONFIG.client.extra.utility.actions.activeHours.activeTimes)), false)
            .addField("Force Reconnect", toggleStr(CONFIG.client.extra.utility.actions.activeHours.forceReconnect), false)
            .addField("Queue ETA Calc", toggleStr(CONFIG.client.extra.utility.actions.activeHours.queueEtaCalc), false)
            .primaryColor();
    }
}
