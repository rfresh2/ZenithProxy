package com.zenith.discord.command;

import com.zenith.Proxy;
import com.zenith.util.Config.Client.Extra.Utility.ActiveHours;
import com.zenith.util.Config.Client.Extra.Utility.ActiveHours.ActiveTime;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.discordjson.json.MessageCreateRequest;
import discord4j.rest.entity.RestChannel;
import discord4j.rest.util.Color;
import discord4j.rest.util.MultipartRequest;

import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.zenith.util.Constants.CONFIG;
import static com.zenith.util.Constants.saveConfig;

public class ActiveHoursCommand extends Command {
    private static final Pattern TIME_PATTERN = Pattern.compile("[0-9]{1,2}:[0-9]{2}");

    public ActiveHoursCommand(final Proxy proxy) {
        super(proxy, "activeHours", "Set active hours for the proxy to automatically be logged in at"
                + "\nUsage:"
                + "\n  " + CONFIG.discord.prefix + "activeHours on/off"
                + "\n  " + CONFIG.discord.prefix + "activeHours timezone <timezone ID>"
                + "\n  " + CONFIG.discord.prefix + "activeHours <add/del> <time>"
                + "\n  " + CONFIG.discord.prefix + "activeHours status"
                + "\n  " + CONFIG.discord.prefix + "activeHours forceReconnect on/off"
                + "\n " + "If forceReconnect is on, the proxy will connect even if a client is already connected to the proxy"
                + "\n Time zone Ids (\"TZ database name\" column): https://en.wikipedia.org/wiki/List_of_tz_database_time_zones"
                + "\n Time format: XX:XX, e.g.: 1:42, 14:42, 14:01");
    }

    @Override
    public MultipartRequest<MessageCreateRequest> execute(MessageCreateEvent event, RestChannel restChannel) {
        List<String> commandArgs = Arrays.asList(event.getMessage().getContent().split(" "));
        EmbedCreateSpec.Builder embedBuilder = EmbedCreateSpec.builder();
        ActiveHours activeHoursConfig = CONFIG.client.extra.utility.actions.activeHours;
        if (commandArgs.size() < 2) {
            embedBuilder
                    .title("Invalid command usage")
                    .addField("Usage", this.description, false)
                    .color(Color.RUBY);
        } else if (commandArgs.get(1).equalsIgnoreCase("status")) {
            embedBuilder
                    .title("Active Hours Status")
                    .color(Color.CYAN)
                    .addField("Time Zone", activeHoursConfig.timeZoneId, false)
                    .addField("Active Hours", (activeHoursConfig.activeTimes.isEmpty() ? "None set!" : activeTimeListToString(activeHoursConfig.activeTimes)), false)
                    .addField("Force Reconnect", (activeHoursConfig.forceReconnect ? "on" : "off"), false);
        } else if (commandArgs.get(1).equalsIgnoreCase("on")) {
            activeHoursConfig.enabled = true;
            embedBuilder
                    .title("Active Hours On!")
                    .color(Color.CYAN);
        } else if (commandArgs.get(1).equalsIgnoreCase("off")) {
            activeHoursConfig.enabled = false;
            embedBuilder
                    .title("Active Hours Off!")
                    .color(Color.CYAN);
        } else if (commandArgs.size() < 3) {
            embedBuilder
                    .title("Invalid command usage")
                    .addField("Usage", this.description, false)
                    .color(Color.RUBY);
        } else if (commandArgs.get(1).equalsIgnoreCase("add")) {
            if (!timeMatchesRegex(commandArgs.get(2))) {
                embedBuilder
                        .title("Invalid command usage")
                        .addField("Usage", this.description, false)
                        .color(Color.RUBY);
            } else {
                ActiveTime activeTime = ActiveTime.fromString(commandArgs.get(2));
                if (!activeHoursConfig.activeTimes.contains(activeTime)) {
                    activeHoursConfig.activeTimes.add(activeTime);
                }
                embedBuilder
                        .title("Added time: " + commandArgs.get(2))
                        .color(Color.CYAN)
                        .addField("Time Zone", activeHoursConfig.timeZoneId, false)
                        .addField("Active Hours", (activeHoursConfig.activeTimes.isEmpty() ? "None set!" : activeTimeListToString(activeHoursConfig.activeTimes)), false)
                        .addField("Force Reconnect", (activeHoursConfig.forceReconnect ? "on" : "off"), false);
            }
        } else if (commandArgs.get(1).equalsIgnoreCase("del")) {
            if (!timeMatchesRegex(commandArgs.get(2))) {
                embedBuilder
                        .title("Invalid command usage")
                        .addField("Usage", this.description, false)
                        .color(Color.RUBY);
            } else {
                ActiveTime activeTime = ActiveTime.fromString(commandArgs.get(2));
                activeHoursConfig.activeTimes.removeIf(s -> s.equals(activeTime));
                embedBuilder
                        .title("Removed time: " + commandArgs.get(2))
                        .color(Color.CYAN)
                        .addField("Time Zone", activeHoursConfig.timeZoneId, false)
                        .addField("Active Hours", (activeHoursConfig.activeTimes.isEmpty() ? "None set!" : activeTimeListToString(activeHoursConfig.activeTimes)), false)
                        .addField("Force Reconnect", (activeHoursConfig.forceReconnect ? "on" : "off"), false);
            }
        } else if (commandArgs.get(1).equalsIgnoreCase("timezone")) {
            if (ZoneId.getAvailableZoneIds().stream().noneMatch(id -> id.equals(commandArgs.get(2)))) {
                embedBuilder
                        .title("Invalid command usage")
                        .addField("Usage", this.description, false)
                        .color(Color.RUBY);
            } else {
                activeHoursConfig.timeZoneId = ZoneId.of(commandArgs.get(2)).getId();
                embedBuilder
                        .title("Set timezone: " + commandArgs.get(2))
                        .color(Color.CYAN)
                        .addField("Time Zone", activeHoursConfig.timeZoneId, false)
                        .addField("Active Hours", (activeHoursConfig.activeTimes.isEmpty() ? "None set!" : activeTimeListToString(activeHoursConfig.activeTimes)), false)
                        .addField("Force Reconnect", (activeHoursConfig.forceReconnect ? "on" : "off"), false);
            }
        }  else if (commandArgs.get(1).equalsIgnoreCase("forceReconnect")) {
            if (commandArgs.get(2).equalsIgnoreCase("on")) {
                activeHoursConfig.forceReconnect = true;
                embedBuilder
                        .title("Force Reconnect On!")
                        .color(Color.CYAN);
            } else if (commandArgs.get(2).equalsIgnoreCase("off")) {
                activeHoursConfig.forceReconnect = false;
                embedBuilder
                        .title("Force Reconnect Off!")
                        .color(Color.CYAN);
            } else {
                embedBuilder
                        .title("Invalid command usage")
                        .addField("Usage", this.description, false)
                        .color(Color.RUBY);
            }
        }

        saveConfig();
        return MessageCreateSpec.builder()
                .addEmbed(embedBuilder
                        .build())
                .build().asRequest();
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
                .map(ActiveTime::toString)
                .collect(Collectors.joining(", "));
    }
}
