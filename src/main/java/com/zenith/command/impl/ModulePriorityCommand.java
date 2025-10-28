package com.zenith.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.command.api.Command;
import com.zenith.command.api.CommandCategory;
import com.zenith.command.api.CommandContext;
import com.zenith.command.api.CommandUsage;
import com.zenith.feature.pathfinder.Baritone;
import com.zenith.module.impl.*;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;

import java.util.Comparator;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.zenith.Globals.CONFIG;
import static com.zenith.Globals.MODULE;

public class ModulePriorityCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.builder()
            .name("modulePriority")
            .category(CommandCategory.MODULE)
            .description("""
                Configures the priority of ZenithProxy modules.

                A higher priority means the module's actions (like rotations, clicks, etc.) will take precedence over modules with lower priorities.

                Should not be edited normally, only for advanced use cases.

                Default module priorities may be changed between versions.
                """)
            .usageLines(
                "autoTotem <default/int>",
                "autoArmor <default/int>",
                "autoEat <default/int>",
                "autoOmen <default/int>",
                "click <default/int>",
                "killAura <default/int>",
                "pathfinder <default/int>",
                "spawnPatrol <default/int>",
                "antiAfk <default/int>",
                "autoMend <default/int>",
                "autoDrop <default/int>",
                "autoFish <default/int>",
                "spook <default/int>",
                "list"
            )
            .build();
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("modulePriority")
            .then(literal("autoTotem")
                .then(argument("priority", integer()).executes(c -> {
                    CONFIG.client.extra.autoTotem.priority = getInteger(c, "priority");
                    c.getSource().getEmbed()
                        .title("AutoTotem Priority Set");
                }))
                .then(literal("default").executes(c -> {
                    CONFIG.client.extra.autoTotem.priority = null;
                    c.getSource().getEmbed()
                        .title("AutoTotem Priority Reset");
                })))
            .then(literal("autoArmor")
                .then(argument("priority", integer()).executes(c -> {
                    CONFIG.client.extra.autoArmor.priority = getInteger(c, "priority");
                    c.getSource().getEmbed()
                        .title("AutoArmor Priority Set");
                }))
                .then(literal("default").executes(c -> {
                    CONFIG.client.extra.autoArmor.priority = null;
                    c.getSource().getEmbed()
                        .title("AutoArmor Priority Reset");
                })))
            .then(literal("autoEat")
                .then(argument("priority", integer()).executes(c -> {
                    CONFIG.client.extra.autoEat.priority = getInteger(c, "priority");
                    c.getSource().getEmbed()
                        .title("AutoEat Priority Set");
                }))
                .then(literal("default").executes(c -> {
                    CONFIG.client.extra.autoEat.priority = null;
                    c.getSource().getEmbed()
                        .title("AutoEat Priority Reset");
                })))
            .then(literal("autoOmen")
                .then(argument("priority", integer()).executes(c -> {
                    CONFIG.client.extra.autoOmen.priority = getInteger(c, "priority");
                    c.getSource().getEmbed()
                        .title("AutoOmen Priority Set");
                }))
                .then(literal("default").executes(c -> {
                    CONFIG.client.extra.autoOmen.priority = null;
                    c.getSource().getEmbed()
                        .title("AutoOmen Priority Reset");
                })))
            .then(literal("click")
                .then(argument("priority", integer()).executes(c -> {
                    CONFIG.client.extra.click.priority = getInteger(c, "priority");
                    c.getSource().getEmbed()
                        .title("Click Priority Set");
                }))
                .then(literal("default").executes(c -> {
                    CONFIG.client.extra.click.priority = null;
                    c.getSource().getEmbed()
                        .title("Click Priority Reset");
                })))
            .then(literal("killAura")
                .then(argument("priority", integer()).executes(c -> {
                    CONFIG.client.extra.killAura.actionPriority = getInteger(c, "priority");
                    c.getSource().getEmbed()
                        .title("KillAura Priority Set");
                }))
                .then(literal("default").executes(c -> {
                    CONFIG.client.extra.killAura.actionPriority = null;
                    c.getSource().getEmbed()
                        .title("KillAura Priority Reset");
                })))
            .then(literal("pathfinder")
                .then(argument("priority", integer()).executes(c -> {
                    CONFIG.client.extra.pathfinder.priority = getInteger(c, "priority");
                    c.getSource().getEmbed()
                        .title("Pathfinder Priority Set");
                }))
                .then(literal("default").executes(c -> {
                    CONFIG.client.extra.pathfinder.priority = null;
                    c.getSource().getEmbed()
                        .title("Pathfinder Priority Reset");
                })))
            .then(literal("spawnPatrol")
                .then(argument("priority", integer()).executes(c -> {
                    CONFIG.client.extra.spawnPatrol.priority = getInteger(c, "priority");
                    c.getSource().getEmbed()
                        .title("SpawnPatrol Priority Set");
                }))
                .then(literal("default").executes(c -> {
                    CONFIG.client.extra.spawnPatrol.priority = null;
                    c.getSource().getEmbed()
                        .title("SpawnPatrol Priority Reset");
                })))
            .then(literal("antiAfk")
                .then(argument("priority", integer()).executes(c -> {
                    CONFIG.client.extra.antiafk.priority = getInteger(c, "priority");
                    c.getSource().getEmbed()
                        .title("AntiAFK Priority Set");
                }))
                .then(literal("default").executes(c -> {
                    CONFIG.client.extra.antiafk.priority = null;
                    c.getSource().getEmbed()
                        .title("AntiAFK Priority Reset");
                })))
            .then(literal("autoMend")
                .then(argument("priority", integer()).executes(c -> {
                    CONFIG.client.extra.autoMend.priority = getInteger(c, "priority");
                    c.getSource().getEmbed()
                        .title("AutoMend Priority Set");
                }))
                .then(literal("default").executes(c -> {
                    CONFIG.client.extra.autoMend.priority = null;
                    c.getSource().getEmbed()
                        .title("AutoMend Priority Reset");
                })))
            .then(literal("autoDrop")
                .then(argument("priority", integer()).executes(c -> {
                    CONFIG.client.extra.autoDrop.priority = getInteger(c, "priority");
                    c.getSource().getEmbed()
                        .title("AutoDrop Priority Set");
                }))
                .then(literal("default").executes(c -> {
                    CONFIG.client.extra.autoDrop.priority = null;
                    c.getSource().getEmbed()
                        .title("AutoDrop Priority Reset");
                })))
            .then(literal("autoFish")
                .then(argument("priority", integer()).executes(c -> {
                    CONFIG.client.extra.autoFish.priority = getInteger(c, "priority");
                    c.getSource().getEmbed()
                        .title("AutoFish Priority Set");
                }))
                .then(literal("default").executes(c -> {
                    CONFIG.client.extra.autoFish.priority = null;
                    c.getSource().getEmbed()
                        .title("AutoFish Priority Reset");
                })))
            .then(literal("spook")
                .then(argument("priority", integer()).executes(c -> {
                    CONFIG.client.extra.spook.priority = getInteger(c, "priority");
                    c.getSource().getEmbed()
                        .title("Spook Priority Set");
                }))
                .then(literal("default").executes(c -> {
                    CONFIG.client.extra.spook.priority = null;
                    c.getSource().getEmbed()
                        .title("Spook Priority Reset");
                })))
            .then(literal("list").executes(c -> {
                c.getSource().getEmbed()
                    .title("Module Priority List")
                    .description(priorityList());
            }));
    }

    @Override
    public void defaultHandler(CommandContext ctx) {
        ctx.getEmbed()
            .primaryColor();
    }

    String priorityList() {
        var sb = new StringBuilder();
        Stream.of(
                new ModuleInstance("AutoTotem", () -> MODULE.get(AutoTotem.class).getPriority(), () -> CONFIG.client.extra.autoTotem.priority),
                new ModuleInstance("AutoArmor", () -> MODULE.get(AutoArmor.class).getPriority(), () -> CONFIG.client.extra.autoArmor.priority),
                new ModuleInstance("AutoEat", () -> MODULE.get(AutoEat.class).getPriority(), () -> CONFIG.client.extra.autoEat.priority),
                new ModuleInstance("AutoOmen", () -> MODULE.get(AutoOmen.class).getPriority(), () -> CONFIG.client.extra.autoOmen.priority),
                new ModuleInstance("Click", () -> MODULE.get(Click.class).getPriority(), () -> CONFIG.client.extra.click.priority),
                new ModuleInstance("KillAura", () -> MODULE.get(KillAura.class).getPriority(), () -> CONFIG.client.extra.killAura.actionPriority),
                new ModuleInstance("Pathfinder", () -> Baritone.getPriority(), () -> CONFIG.client.extra.pathfinder.priority),
                new ModuleInstance("SpawnPatrol", () -> MODULE.get(SpawnPatrol.class).getPriority(), () -> CONFIG.client.extra.spawnPatrol.priority),
                new ModuleInstance("AntiAFK", () -> MODULE.get(AntiAFK.class).getPriority(), () -> CONFIG.client.extra.antiafk.priority),
                new ModuleInstance("AutoMend", () -> MODULE.get(AutoMend.class).getPriority(), () -> CONFIG.client.extra.autoMend.priority),
                new ModuleInstance("AutoDrop", () -> MODULE.get(AutoDrop.class).getPriority(), () -> CONFIG.client.extra.autoDrop.priority),
                new ModuleInstance("AutoFish", () -> MODULE.get(AutoFish.class).getPriority(), () -> CONFIG.client.extra.autoFish.priority),
                new ModuleInstance("Spook", () -> MODULE.get(Spook.class).getPriority(), () -> CONFIG.client.extra.spook.priority))
            .sorted(Comparator.reverseOrder())
            .forEach(instance -> {
                sb
                    .append("**")
                    .append(instance.name())
                    .append("**: ")
                    .append(instance.priorityAccessor().getAsInt());
                if (instance.isDefault()) {
                    sb.append(" (default)");
                }
                sb.append("\n");
            });
        return sb.toString();
    }

    record ModuleInstance(String name, IntSupplier priorityAccessor, Supplier<@Nullable Integer> settingValueAccessor) implements Comparable<ModuleInstance> {
        boolean isDefault() {
            return settingValueAccessor.get() == null;
        }

        @Override
        public int compareTo(@NotNull final ModulePriorityCommand.ModuleInstance o) {
            return Integer.compare(this.priorityAccessor.getAsInt(), o.priorityAccessor.getAsInt());
        }
    }
}
