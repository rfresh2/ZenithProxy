package com.zenith.command.impl;

import com.github.steveice10.mc.protocol.data.game.entity.type.EntityType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.command.Command;
import com.zenith.command.CommandUsage;
import com.zenith.command.brigadier.CommandCategory;
import com.zenith.command.brigadier.CommandContext;
import com.zenith.discord.Embed;
import com.zenith.module.impl.KillAura;
import discord4j.rest.util.Color;

import java.util.stream.Collectors;

import static com.mojang.brigadier.arguments.DoubleArgumentType.doubleArg;
import static com.mojang.brigadier.arguments.DoubleArgumentType.getDouble;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.mojang.brigadier.arguments.StringArgumentType.string;
import static com.zenith.Shared.CONFIG;
import static com.zenith.Shared.MODULE_MANAGER;
import static com.zenith.command.brigadier.ToggleArgumentType.getToggle;
import static com.zenith.command.brigadier.ToggleArgumentType.toggle;
import static java.util.Arrays.asList;

public class KillAuraCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.full("killAura",
                                 CommandCategory.MODULE,
                                 """
                                 Attacks entities near the player.
                                 
                                 Custom targets list: http://gg.gg/19hyyp
                                 """,
                                 asList("on/off",
                                        "attackDelay <ticks>",
                                        "targetPlayers on/off",
                                        "targetHostileMobs on/off",
                                        "targetNeutralMobs on/off",
                                        "targetNeutralMobs onlyAggressive on/off",
                                        "targetArmorStands on/off",
                                        "targetCustom on/off",
                                        "targetCustom add/del <entityType>",
                                        "weaponSwitch on/off",
                                        "range <number>"),
                                 asList("ka")
        );
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("killAura")
            .then(argument("toggle", toggle()).executes(c -> {
                CONFIG.client.extra.killAura.enabled = getToggle(c, "toggle");
                MODULE_MANAGER.get(KillAura.class).syncEnabledFromConfig();
                c.getSource().getEmbed()
                             .title("Kill Aura " + toggleStrCaps(CONFIG.client.extra.killAura.enabled));
                return 1;
            }))
            .then(literal("attackDelay")
                      .then(argument("ticks", integer(0, 1000)).executes(c -> {
                          CONFIG.client.extra.killAura.attackDelayTicks = c.getArgument("ticks", Integer.class);
                          c.getSource().getEmbed()
                                       .title("Attack Delay Ticks Set!");
                          return 1;
                      })))
            .then(literal("targetPlayers")
                      .then(argument("toggle", toggle()).executes(c -> {
                            CONFIG.client.extra.killAura.targetPlayers = getToggle(c, "toggle");
                            c.getSource().getEmbed()
                                         .title("Target Players " + toggleStrCaps(CONFIG.client.extra.killAura.targetPlayers));
                            return 1;
                      })))
            .then(literal("targetHostileMobs")
                      .then(argument("toggle", toggle()).executes(c -> {
                            CONFIG.client.extra.killAura.targetHostileMobs = getToggle(c, "toggle");
                            c.getSource().getEmbed()
                                         .title("Target Mobs " + toggleStrCaps(CONFIG.client.extra.killAura.targetHostileMobs));
                            return 1;
                      })))
            .then(literal("targetNeutralMobs")
                      .then(argument("toggle", toggle()).executes(c -> {
                            CONFIG.client.extra.killAura.targetNeutralMobs = getToggle(c, "toggle");
                            c.getSource().getEmbed()
                                         .title("Target Neutral Mobs " + toggleStrCaps(CONFIG.client.extra.killAura.targetNeutralMobs));
                            return 1;
                      }))
                      .then(literal("onlyAggressive")
                                .then(argument("toggle", toggle()).executes(c -> {
                                    CONFIG.client.extra.killAura.onlyNeutralAggressive = getToggle(c, "toggle");
                                    c.getSource().getEmbed()
                                                 .title("Target Neutral Mobs Only Aggressive " + toggleStrCaps(CONFIG.client.extra.killAura.onlyNeutralAggressive));
                                    return 1;
                                }))))
            .then(literal("targetArmorStands")
                      .then(argument("toggle", toggle()).executes(c -> {
                            CONFIG.client.extra.killAura.targetArmorStands = getToggle(c, "toggle");
                            c.getSource().getEmbed()
                                         .title("Target Armor Stands " + toggleStrCaps(CONFIG.client.extra.killAura.targetArmorStands));
                            return 1;
                      })))
            .then(literal("weaponSwitch")
                      .then(argument("toggle", toggle()).executes(c -> {
                            CONFIG.client.extra.killAura.switchWeapon = getToggle(c, "toggle");
                            c.getSource().getEmbed()
                                         .title("Weapon Switching " + toggleStrCaps(CONFIG.client.extra.killAura.switchWeapon));
                            return 1;
                      })))
            .then(literal("range").then(argument("range", doubleArg(0.01, 5.0)).executes(c -> {
                CONFIG.client.extra.killAura.attackRange = getDouble(c, "range");
                c.getSource().getEmbed()
                             .title("Attack Range Set!");
                return 1;
            })))
            .then(literal("targetCustom")
                      .then(argument("toggle", toggle()).executes(c -> {
                            CONFIG.client.extra.killAura.targetCustom = getToggle(c, "toggle");
                            c.getSource().getEmbed()
                                         .title("Target Custom " + toggleStrCaps(CONFIG.client.extra.killAura.targetCustom));
                            return 1;
                      }))
                      .then(literal("add")
                                .then(argument("entityType", string()).executes(c -> {
                                    var entityType = c.getArgument("entityType", String.class);
                                    var foundType = entityType.toUpperCase();
                                    try {
                                        var type = Enum.valueOf(EntityType.class, foundType);
                                        if (!CONFIG.client.extra.killAura.customTargets.contains(type))
                                            CONFIG.client.extra.killAura.customTargets.add(type);
                                        c.getSource().getEmbed()
                                                     .title("Added " + type.name());
                                    } catch (Exception e) {
                                        c.getSource().getEmbed()
                                                     .title("Invalid Entity Type")
                                                     .color(Color.RUBY);
                                    }
                                    return 1;
                                })))
                      .then(literal("del")
                                .then(argument("entityType", string()).executes(c -> {
                                    var entityType = c.getArgument("entityType", String.class);
                                    var foundType = entityType.toUpperCase();
                                    try {
                                        var type = Enum.valueOf(EntityType.class, foundType);
                                        CONFIG.client.extra.killAura.customTargets.remove(type);
                                        c.getSource().getEmbed()
                                            .title("Removed " + type.name());
                                    } catch (Exception e) {
                                        c.getSource().getEmbed()
                                            .title("Invalid Entity Type")
                                            .color(Color.RUBY);
                                    }
                                    return 1;
                                }))));
    }

    @Override
    public void postPopulate(Embed builder) {
        builder
            .addField("KillAura", toggleStr(CONFIG.client.extra.killAura.enabled), false)
            .addField("Target Players", toggleStr(CONFIG.client.extra.killAura.targetPlayers), false)
            .addField("Target Hostile Mobs", toggleStr(CONFIG.client.extra.killAura.targetHostileMobs), false)
            .addField("Target Neutral Mobs", toggleStr(CONFIG.client.extra.killAura.targetNeutralMobs), false)
            .addField("Target Custom", toggleStr(CONFIG.client.extra.killAura.targetCustom), false)
            .addField("Only Aggressive Neutral Mobs", toggleStr(CONFIG.client.extra.killAura.onlyNeutralAggressive), false)
            .addField("Target Armor Stands", toggleStr(CONFIG.client.extra.killAura.targetArmorStands), false)
            .addField("Weapon Switching", toggleStr(CONFIG.client.extra.killAura.switchWeapon), false)
            .addField("Attack Delay Ticks", CONFIG.client.extra.killAura.attackDelayTicks, false)
            .addField("Attack Range", CONFIG.client.extra.killAura.attackRange, false)
            .color(Color.CYAN);
        if (CONFIG.client.extra.killAura.targetCustom) {
            builder.description("Custom Targets: " + CONFIG.client.extra.killAura.customTargets.stream().map(Enum::name).collect(
                Collectors.joining(", ", "[", "]")));
        }
    }
}
