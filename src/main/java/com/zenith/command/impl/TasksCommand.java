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

import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
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
                        getString(c, "id"),
                        new CommandAction(command),
                        new TimedCondition(getInteger(c, "delay") * 50L),
                        repeat
                            ? new ContinuationForever()
                            : new ContinuationOnce()
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
                        getString(c, "id"),
                        new CommandAction(command),
                        new EventCondition(eventClass),
                        repeat
                            ? new ContinuationForever()
                            : new ContinuationOnce()
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
                })))))))
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
