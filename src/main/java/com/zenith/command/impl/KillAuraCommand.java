package com.zenith.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.command.Command;
import com.zenith.command.CommandContext;
import com.zenith.command.CommandUsage;
import com.zenith.module.Module;
import com.zenith.module.impl.KillAura;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;

import java.util.List;

import static com.mojang.brigadier.arguments.DoubleArgumentType.doubleArg;
import static com.mojang.brigadier.arguments.DoubleArgumentType.getDouble;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.zenith.Shared.CONFIG;
import static com.zenith.Shared.MODULE_MANAGER;
import static java.util.Arrays.asList;

public class KillAuraCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.full("killAura",
                "Attacks entities near the player",
                asList("on/off",
                       "attackDelay <ticks>",
                       "targetPlayers on/off",
                       "targetMobs on/off",
                       "targetArmorStands on/off",
                       "weaponSwitch on/off",
                       "range <number>"),
                aliases());
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("killaura")
            .then(literal("on").executes(c -> {
                CONFIG.client.extra.killAura.enabled = true;
                MODULE_MANAGER.getModule(KillAura.class).ifPresent(Module::syncEnabledFromConfig);
                populate(c.getSource().getEmbedBuilder()
                    .title("Kill Aura On!")
                    .color(Color.CYAN));
            }))
            .then(literal("off").executes(c -> {
                CONFIG.client.extra.killAura.enabled = false;
                MODULE_MANAGER.getModule(KillAura.class).ifPresent(Module::syncEnabledFromConfig);
                populate(c.getSource().getEmbedBuilder()
                             .title("Kill Aura Off!")
                             .color(Color.CYAN));
            }))
            .then(literal("attackdelay")
                      .then(argument("ticks", integer()).executes(c -> {
                            CONFIG.client.extra.killAura.attackDelayTicks = c.getArgument("ticks", Integer.class);
                            populate(c.getSource().getEmbedBuilder()
                                .title("Attack Delay Ticks Set!")
                                .color(Color.CYAN));
                            return 1;
                      })))
            .then(literal("targetplayers")
                      .then(literal("on").executes(c -> {
                          CONFIG.client.extra.killAura.targetPlayers = true;
                          populate(c.getSource().getEmbedBuilder()
                              .title("Target Players On!")
                              .color(Color.CYAN));
                      }))
                      .then(literal("off").executes(c -> {
                          CONFIG.client.extra.killAura.targetPlayers = false;
                          populate(c.getSource().getEmbedBuilder()
                              .title("Target Players Off!")
                              .color(Color.CYAN));
                      })))
            .then(literal("targetmobs")
                      .then(literal("on").executes(c -> {
                          CONFIG.client.extra.killAura.targetHostileMobs = true;
                          populate(c.getSource().getEmbedBuilder()
                              .title("Target Mobs On!")
                              .color(Color.CYAN));
                      }))
                      .then(literal("off").executes(c -> {
                          CONFIG.client.extra.killAura.targetHostileMobs = false;
                          populate(c.getSource().getEmbedBuilder()
                              .title("Target Mobs Off!")
                              .color(Color.CYAN));
                      })))
            .then(literal("targetarmorstands")
                      .then(literal("on").executes(c -> {
                          CONFIG.client.extra.killAura.targetArmorStands = true;
                          populate(c.getSource().getEmbedBuilder()
                              .title("Target Armor Stands On!")
                              .color(Color.CYAN));
                      }))
                      .then(literal("off").executes(c -> {
                          CONFIG.client.extra.killAura.targetArmorStands = false;
                          populate(c.getSource().getEmbedBuilder()
                              .title("Target Armor Stands Off!")
                              .color(Color.CYAN));
                      })))
            .then(literal("weaponswitch")
                      .then(literal("on").executes(c -> {
                          CONFIG.client.extra.killAura.switchWeapon = true;
                          populate(c.getSource().getEmbedBuilder()
                              .title("Weapon Switching On!")
                              .color(Color.CYAN));
                      }))
                      .then(literal("off").executes(c -> {
                          CONFIG.client.extra.killAura.switchWeapon = false;
                          populate(c.getSource().getEmbedBuilder()
                              .title("Weapon Switching Off!")
                              .color(Color.CYAN));
                      })))
            .then(literal("range").then(argument("range", doubleArg(0.01, 5.0)).executes(c -> {
                CONFIG.client.extra.killAura.attackRange = getDouble(c, "range");
                populate(c.getSource().getEmbedBuilder()
                    .title("Attack Range Set!")
                    .color(Color.CYAN));
                return 1;
            })));
    }

    @Override
    public List<String> aliases() {
        return asList("ka");
    }

    private EmbedCreateSpec.Builder populate(EmbedCreateSpec.Builder builder) {
        return builder
            .addField("Target Players", CONFIG.client.extra.killAura.targetPlayers ? "on" : "off", false)
            .addField("Target Hostile Mobs", CONFIG.client.extra.killAura.targetHostileMobs ? "on" : "off", false)
            .addField("Target Armor Stands", CONFIG.client.extra.killAura.targetArmorStands ? "on" : "off", false)
            .addField("Weapon Switching", CONFIG.client.extra.killAura.switchWeapon ? "on" : "off", false)
            .addField("Attack Delay Ticks", CONFIG.client.extra.killAura.attackDelayTicks+"", false)
            .addField("Attack Range", CONFIG.client.extra.killAura.attackRange+"", false);
    }
}
