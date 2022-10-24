package com.zenith.discord.command.brigadier.impl;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.zenith.discord.command.brigadier.BrigadierCommand;
import com.zenith.discord.command.brigadier.CommandContext;
import com.zenith.discord.command.brigadier.CommandUsage;
import com.zenith.util.Config.Client.Extra.Utility.ActiveHours.ActiveTime;
import discord4j.rest.util.Color;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.mojang.brigadier.arguments.StringArgumentType.string;
import static com.zenith.util.Constants.CONFIG;
import static java.util.Arrays.asList;

public class ActiveHoursBrigadierCommand extends BrigadierCommand {
    private static final Pattern TIME_PATTERN = Pattern.compile("[0-9]{1,2}:[0-9]{2}");

    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.of(
                "activeHours",
                "Set active hours for the proxy to automatically be logged in at."
                        + "\n Time zone Ids (\"TZ database name\" column): https://en.wikipedia.org/wiki/List_of_tz_database_time_zones"
                        + "\n Time format: XX:XX, e.g.: 1:42, 14:42, 14:01",
                asList("on/off",
                        "timezone <timezone ID>",
                        "add/del <time>",
                        "status",
                        "forceReconnect on/off")
        );
    }

    @Override
    public void register(CommandDispatcher<CommandContext> dispatcher) {
        dispatcher.register(
                command("activeHours")
                        .then(literal("on").executes(c -> {
                            CONFIG.client.extra.utility.actions.activeHours.enabled = true;
                            c.getSource().getEmbedBuilder()
                                    .title("Active Hours On!")
                                    .color(Color.CYAN);
                        }))
                        .then(literal("off").executes(c -> {
                            CONFIG.client.extra.utility.actions.activeHours.enabled = false;
                            c.getSource().getEmbedBuilder()
                                    .title("Active Hours Off!")
                                    .color(Color.CYAN);
                        }))
                        .then(literal("add").then(argument("time", string()).executes(c -> {
                            final String time = StringArgumentType.getString(c, "time");
                            if (!timeMatchesRegex(time)) {
                                return -1;
                            } else {
                                final ActiveTime activeTime = ActiveTime.fromString(time);
                                if (!CONFIG.client.extra.utility.actions.activeHours.activeTimes.contains(activeTime)) {
                                    CONFIG.client.extra.utility.actions.activeHours.activeTimes.add(activeTime);
                                }
                                c.getSource().getEmbedBuilder()
                                        .title("Added time: " + time)
                                        .color(Color.CYAN)
                                        .addField("Time Zone", CONFIG.client.extra.utility.actions.activeHours.timeZoneId, false)
                                        .addField("Active Hours", (CONFIG.client.extra.utility.actions.activeHours.activeTimes.isEmpty() ? "None set!" : activeTimeListToString(CONFIG.client.extra.utility.actions.activeHours.activeTimes)), false)
                                        .addField("Force Reconnect", (CONFIG.client.extra.utility.actions.activeHours.forceReconnect ? "on" : "off"), false);
                                return 1;
                            }
                        })))
                        .then(literal("del").then(argument("del", string()).executes(c -> {
                            final String time = StringArgumentType.getString(c, "time");
                            if (!timeMatchesRegex(time)) {
                                return -1;
                            } else {
                                final ActiveTime activeTime = ActiveTime.fromString(time);
                                CONFIG.client.extra.utility.actions.activeHours.activeTimes.removeIf(s -> s.equals(activeTime));
                                c.getSource().getEmbedBuilder()
                                        .title("Removed time: " + time)
                                        .color(Color.CYAN)
                                        .addField("Time Zone", CONFIG.client.extra.utility.actions.activeHours.timeZoneId, false)
                                        .addField("Active Hours", (CONFIG.client.extra.utility.actions.activeHours.activeTimes.isEmpty() ? "None set!" : activeTimeListToString(CONFIG.client.extra.utility.actions.activeHours.activeTimes)), false)
                                        .addField("Force Reconnect", (CONFIG.client.extra.utility.actions.activeHours.forceReconnect ? "on" : "off"), false);
                                return 1;
                            }
                        })))
                        .then(literal("status").executes(c -> {
                            c.getSource().getEmbedBuilder()
                                    .title("Active Hours Status")
                                    .color(Color.CYAN)
                                    .addField("Time Zone", CONFIG.client.extra.utility.actions.activeHours.timeZoneId, false)
                                    .addField("Active Hours", (CONFIG.client.extra.utility.actions.activeHours.activeTimes.isEmpty() ? "None set!" : activeTimeListToString(CONFIG.client.extra.utility.actions.activeHours.activeTimes)), false)
                                    .addField("Force Reconnect", (CONFIG.client.extra.utility.actions.activeHours.forceReconnect ? "on" : "off"), false);
                        }))
                        .then(literal("forceReconnect")
                                .then(literal("on").executes(c -> {
                                    CONFIG.client.extra.utility.actions.activeHours.forceReconnect = true;
                                    c.getSource().getEmbedBuilder()
                                            .title("Force Reconnect On!")
                                            .color(Color.CYAN);
                                }))
                                .then(literal("off").executes(c -> {
                                    CONFIG.client.extra.utility.actions.activeHours.forceReconnect = false;
                                    c.getSource().getEmbedBuilder()
                                            .title("Force Reconnect Off!")
                                            .color(Color.CYAN);
                                })))
        );
    }

    private boolean timeMatchesRegex(final String arg) {
        final Matcher matcher = TIME_PATTERN.matcher(arg);
        boolean matchesRegex = matcher.matches();
        if (!matchesRegex) return false;
        final ActiveTime activeTime = ActiveTime.fromString(arg);
        return activeTime.hour <= 24 && activeTime.minute <= 59;
    }

    private String activeTimeListToString(final List<ActiveTime> activeTimes) {
        return activeTimes.stream()
                .sorted((a, b) -> {
                    if (a.hour == b.hour) {
                        return a.minute - b.minute;
                    } else {
                        return a.hour - b.hour;
                    }
                })
                .map(ActiveTime::toString)
                .collect(Collectors.joining(", "));
    }
}
