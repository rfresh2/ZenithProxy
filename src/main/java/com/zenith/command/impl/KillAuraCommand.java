package com.zenith.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.command.Command;
import com.zenith.command.CommandCategory;
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
import static com.zenith.command.ToggleArgumentType.getToggle;
import static com.zenith.command.ToggleArgumentType.toggle;
import static java.util.Arrays.asList;

public class KillAuraCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.full("killAura",
                                 CommandCategory.MODULE,
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
            .then(argument("toggle", toggle()).executes(c -> {
                CONFIG.client.extra.killAura.enabled = getToggle(c, "toggle");
                MODULE_MANAGER.getModule(KillAura.class).ifPresent(Module::syncEnabledFromConfig);
                c.getSource().getEmbedBuilder()
                             .title("Kill Aura " + (CONFIG.client.extra.killAura.enabled ? "On!" : "Off!"));
                return 1;
            }))
            .then(literal("attackdelay")
                      .then(argument("ticks", integer()).executes(c -> {
                          CONFIG.client.extra.killAura.attackDelayTicks = c.getArgument("ticks", Integer.class);
                          c.getSource().getEmbedBuilder()
                                       .title("Attack Delay Ticks Set!");
                          return 1;
                      })))
            .then(literal("targetplayers")
                      .then(argument("toggle", toggle()).executes(c -> {
                            CONFIG.client.extra.killAura.targetPlayers = getToggle(c, "toggle");
                            c.getSource().getEmbedBuilder()
                                         .title("Target Players " + (CONFIG.client.extra.killAura.targetPlayers ? "On!" : "Off!"));
                            return 1;
                      })))
            .then(literal("targetmobs")
                      .then(argument("toggle", toggle()).executes(c -> {
                            CONFIG.client.extra.killAura.targetHostileMobs = getToggle(c, "toggle");
                            c.getSource().getEmbedBuilder()
                                         .title("Target Mobs " + (CONFIG.client.extra.killAura.targetHostileMobs ? "On!" : "Off!"));
                            return 1;
                      })))
            .then(literal("targetarmorstands")
                      .then(argument("toggle", toggle()).executes(c -> {
                            CONFIG.client.extra.killAura.targetArmorStands = getToggle(c, "toggle");
                            c.getSource().getEmbedBuilder()
                                         .title("Target Armor Stands " + (CONFIG.client.extra.killAura.targetArmorStands ? "On!" : "Off!"));
                            return 1;
                      })))
            .then(literal("weaponswitch")
                      .then(argument("toggle", toggle()).executes(c -> {
                            CONFIG.client.extra.killAura.switchWeapon = getToggle(c, "toggle");
                            c.getSource().getEmbedBuilder()
                                         .title("Weapon Switching " + (CONFIG.client.extra.killAura.switchWeapon ? "On!" : "Off!"));
                            return 1;
                      })))
            .then(literal("range").then(argument("range", doubleArg(0.01, 5.0)).executes(c -> {
                CONFIG.client.extra.killAura.attackRange = getDouble(c, "range");
                c.getSource().getEmbedBuilder()
                             .title("Attack Range Set!");
                return 1;
            })));
    }

    @Override
    public List<String> aliases() {
        return asList("ka");
    }

    @Override
    public void postPopulate(EmbedCreateSpec.Builder builder) {
        builder
            .addField("KillAura", toggleStr(CONFIG.client.extra.killAura.enabled), false)
            .addField("Target Players", toggleStr(CONFIG.client.extra.killAura.targetPlayers), false)
            .addField("Target Hostile Mobs", toggleStr(CONFIG.client.extra.killAura.targetHostileMobs), false)
            .addField("Target Armor Stands", toggleStr(CONFIG.client.extra.killAura.targetArmorStands), false)
            .addField("Weapon Switching", toggleStr(CONFIG.client.extra.killAura.switchWeapon), false)
            .addField("Attack Delay Ticks", CONFIG.client.extra.killAura.attackDelayTicks + "", false)
            .addField("Attack Range", CONFIG.client.extra.killAura.attackRange + "", false)
            .color(Color.CYAN);
    }
}
