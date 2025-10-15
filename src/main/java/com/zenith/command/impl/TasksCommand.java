package com.zenith.command.impl;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.command.api.Command;
import com.zenith.command.api.CommandCategory;
import com.zenith.command.api.CommandContext;
import com.zenith.command.api.CommandUsage;
import com.zenith.event.client.ClientConnectEvent;
import com.zenith.event.client.ClientDeathEvent;
import com.zenith.event.client.ClientDisconnectEvent;
import com.zenith.event.client.ClientOnlineEvent;
import com.zenith.event.player.PlayerConnectedEvent;
import com.zenith.event.player.PlayerDisconnectedEvent;
import com.zenith.feature.tasks.*;
import com.zenith.module.impl.Tasks;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.regex.Pattern;

import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.mojang.brigadier.arguments.StringArgumentType.greedyString;
import static com.zenith.Globals.COMMAND;
import static com.zenith.Globals.MODULE;
import static com.zenith.command.brigadier.CustomStringArgumentType.getString;
import static com.zenith.command.brigadier.CustomStringArgumentType.wordWithChars;
import static com.zenith.command.brigadier.TimeArgument.time;

public class TasksCommand extends Command {
    private static final BiMap<String, Class<?>> EVENT_MAP = ImmutableBiMap.of(
        "connect", ClientConnectEvent.class,
        "death", ClientDeathEvent.class,
        "disconnect", ClientDisconnectEvent.class,
        "online", ClientOnlineEvent.class,
        "playerConnect", PlayerConnectedEvent.class,
        "playerDisconnect", PlayerDisconnectedEvent.class
    );
    private static final Pattern TIME_PATTERN = Pattern.compile("[0-9]{1,2}:[0-9]{2}");

    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.builder()
            .name("tasks")
            .category(CommandCategory.MODULE)
            .description("""
                [BETA]

                Schedules commands to be executed after a delay or after specified events.

                Tasks do NOT save and persist through ZenithProxy restarts (yet).
                """)
            .usageLines(
                "add timed <repeat/once> <id> <delay> <command>",
                "add event <repeat/once> <id> <" + String.join("/", EVENT_MAP.keySet().stream().sorted().toList()) + "> <command>",
                "add interval <repeat/once> <id> <interval> <daily/hourly/minutely/secondly/tickly> <startTime> <timezoneId> <command>",
                "del <id>",
                "list",
                "clear"
            )
            .aliases("task")
            .build();
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("tasks")
            .then(literal("add")
                .then(literal("timed").then(argument("repeat", enumStrings("repeat", "once")).then(argument("id", wordWithChars()).then(argument("delay", time()).then(argument("command", greedyString()).executes(c -> {
                    var taskId = getString(c, "id");
                    var repeat = getString(c, "repeat").equalsIgnoreCase("repeat");
                    var command = getString(c, "command");
                    var parse = COMMAND.parse(CommandContext.create(command, new CommandAction.CommandActionSource()));
                    if (!parse.getExceptions().isEmpty() || parse.getReader().canRead()) {
                        c.getSource().getEmbed()
                            .title("Invalid Command")
                            .description("Invalid command: `" + command + "`"
                                + (parse.getExceptions().isEmpty() ? "" : "\nExceptions: " + parse.getExceptions().values())
                            );
                        return ERROR;
                    }
                    var task = new Task(
                        taskId,
                        new CommandAction(command),
                        new TimedCondition(getInteger(c, "delay") * 50L),
                        repeat
                            ? new ForeverContinuation()
                            : new OnceContinuation()
                    );
                    MODULE.get(Tasks.class).addTask(task);
                    c.getSource().getEmbed()
                        .title("Task Added")
                        .addField("Task ID", task.getId())
                        .addField("Type", "Timed")
                        .addField("Repeat", repeat)
                        .addField("Delay", getInteger(c, "delay") + " ticks")
                        .addField("Command", command);
                    return OK;
                }))))))
                .then(literal("event").then(argument("repeat", enumStrings("repeat", "once")).then(argument("id", wordWithChars()).then(argument("event", enumStrings(EVENT_MAP.keySet())).then(argument("command", greedyString()).executes(c -> {
                    var taskId = getString(c, "id");
                    var eventId = getString(c, "event");
                    var repeat = getString(c, "repeat").equalsIgnoreCase("repeat");
                    var command = getString(c, "command");
                    var parse = COMMAND.parse(CommandContext.create(command, new CommandAction.CommandActionSource()));
                    if (!parse.getExceptions().isEmpty() || parse.getReader().canRead()) {
                        c.getSource().getEmbed()
                            .title("Invalid Command")
                            .description("Invalid command: `" + command + "`"
                                + (parse.getExceptions().isEmpty() ? "" : "\nExceptions: " + parse.getExceptions().values())
                            );
                        return ERROR;
                    }
                    Class<?> eventClass = null;
                    for (var entry : EVENT_MAP.entrySet()) {
                        if (entry.getKey().equalsIgnoreCase(eventId)) {
                            eventClass = entry.getValue();
                            break;
                        }
                    }
                    if (eventClass == null) {
                        c.getSource().getEmbed()
                            .title("Invalid Event")
                            .description("Unknown event id '" + eventId + "'");
                        return ERROR;
                    }
                    var task = new Task(
                        taskId,
                        new CommandAction(command),
                        new EventCondition(eventClass),
                        repeat
                            ? new ForeverContinuation()
                            : new OnceContinuation()
                    );
                    MODULE.get(Tasks.class).addTask(task);
                    c.getSource().getEmbed()
                        .title("Task Added")
                        .addField("Task ID", task.getId())
                        .addField("Type", "Event")
                        .addField("Repeat", repeat)
                        .addField("Event", EVENT_MAP.inverse().get(eventClass))
                        .addField("Command", command);
                    return OK;
                }))))))
                .then(literal("interval").then(argument("repeat", enumStrings("repeat", "once")).then(argument("id", wordWithChars()).then(argument("interval", integer(1)).then(argument("period", enumStrings("daily", "hourly", "minutely", "secondly")).then(argument("startTime", wordWithChars()).then(argument("timezoneId", wordWithChars()).then(argument("command", greedyString()).executes(c -> {
                    var taskId = getString(c, "id");
                    var repeat = getString(c, "repeat").equalsIgnoreCase("repeat");
                    var command = getString(c, "command");
                    var parse = COMMAND.parse(CommandContext.create(command, new CommandAction.CommandActionSource()));
                    if (!parse.getExceptions().isEmpty() || parse.getReader().canRead()) {
                        c.getSource().getEmbed()
                            .title("Invalid Command")
                            .description("Invalid command: `" + command + "`"
                                + (parse.getExceptions().isEmpty() ? "" : "\nExceptions: " + parse.getExceptions().values())
                            );
                        return ERROR;
                    }
                    var startTimeStr = getString(c, "startTime");
                    if (!TIME_PATTERN.matcher(startTimeStr).matches()) {
                        c.getSource().getEmbed()
                            .title("Invalid Time Format")
                            .addField("Help", "Time format: XX:XX, e.g.: 1:42, 14:42, 14:01", false);
                        return ERROR;
                    }
                    var timezoneId = getString(c, "timezoneId");
                    if (ZoneId.getAvailableZoneIds().stream().noneMatch(id -> id.equals(timezoneId))) {
                        c.getSource().getEmbed()
                            .title("Invalid Timezone")
                            .addField("Help", "Time zone Ids: https://w.wiki/8Yif", false);
                        return ERROR;
                    }
                    var startHour = Integer.parseInt(startTimeStr.split(":")[0]);
                    var startMinute = Integer.parseInt(startTimeStr.split(":")[1]);
                    if (startHour < 0 || startHour > 23 || startMinute < 0 || startMinute > 59) {
                        c.getSource().getEmbed()
                            .title("Invalid Time")
                            .addField("Help", "Time format: XX:XX, e.g.: 1:42, 14:42, 14:01", false);
                        return ERROR;
                    }
                    var localT = LocalDateTime.now(ZoneId.of(timezoneId));
                    var startTime = ZonedDateTime.of(
                        localT.getYear(),
                        localT.getMonthValue(),
                        localT.getDayOfMonth(),
                        startHour,
                        startMinute,
                        0,
                        0,
                        ZoneId.of(timezoneId)
                    ).toInstant();
                    var period = getString(c, "period").toLowerCase();
                    var interval = getInteger(c, "interval");
                    var condition = switch (period) {
                        case "daily" -> IntervalCondition.daily(startTime, interval);
                        case "hourly" -> IntervalCondition.hourly(startTime, interval);
                        case "minutely" -> IntervalCondition.minutely(startTime, interval);
                        case "secondly" -> IntervalCondition.secondly(startTime, interval);
                        default -> throw new IllegalStateException("Unexpected value: " + period);
                    };
                    var task = new Task(
                        taskId,
                        new CommandAction(command),
                        condition,
                        repeat
                            ? new ForeverContinuation()
                            : new OnceContinuation()
                    );
                    MODULE.get(Tasks.class).addTask(task);
                    c.getSource().getEmbed()
                        .title("Task Added")
                        .addField("Task ID", task.getId())
                        .addField("Type", "Interval")
                        .addField("Repeat", repeat)
                        .addField("Interval", interval + " " + period)
                        .addField("Start Time", startTimeStr + " (" + timezoneId + ")")
                        .addField("Command", command);
                    return OK;
                }))))))))))
            .then(literal("del").then(argument("id", wordWithChars()).executes(c -> {
                var id = getString(c, "id");
                MODULE.get(Tasks.class).removeTask(id);
                c.getSource().getEmbed()
                    .title("Task Removed")
                    .addField("Task ID", id);
            })))
            .then(literal("list").executes(c -> {
                var tasksStr = MODULE.get(Tasks.class).getTasks().entrySet().stream()
                    .map(e -> "`" + e.getKey()
                        + ": " + (e.getValue().getCondition() instanceof TimedCondition ? "Timed" : "Event")
                        + (e.getValue().getAction() instanceof CommandAction cmd
                            ? " -> " + cmd.getCommand()
                            : "")
                        + "`" )
                    .reduce((a, b) -> a + "\n" + b)
                    .orElse("None");
                c.getSource().getEmbed()
                    .title("Task List")
                    .description(tasksStr);
            }));
    }

    @Override
    public void defaultHandler(CommandContext ctx) {
        ctx.getEmbed()
            .primaryColor();
    }
}
