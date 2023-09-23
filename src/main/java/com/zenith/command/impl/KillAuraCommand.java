package com.zenith.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.command.Command;
import com.zenith.command.CommandContext;
import com.zenith.command.CommandUsage;
import com.zenith.module.Module;
import com.zenith.module.impl.KillAura;
import discord4j.rest.util.Color;

import java.util.List;

import static com.zenith.Shared.CONFIG;
import static com.zenith.Shared.MODULE_MANAGER;
import static java.util.Arrays.asList;

public class KillAuraCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.full("killAura",
                "Attacks entities near the player",
                asList("on/off",
                       "targetPlayers on/off",
                       "targetMobs on/off",
                       "weaponSwitch on/off"),
                aliases());
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("killaura")
                .then(literal("on").executes(c -> {
                    CONFIG.client.extra.killAura.enabled = true;
                    MODULE_MANAGER.getModule(KillAura.class).ifPresent(Module::syncEnabledFromConfig);
                    c.getSource().getEmbedBuilder()
                        .title("Kill Aura On!")
                        .color(Color.CYAN)
                        .addField("Target Players", CONFIG.client.extra.killAura.targetPlayers ? "on" : "off", false)
                        .addField("Target Hostile Mobs", CONFIG.client.extra.killAura.targetHostileMobs ? "on" : "off", false)
                        .addField("Weapon Switching", CONFIG.client.extra.killAura.switchWeapon ? "on" : "off", false);

                }))
                .then(literal("off").executes(c -> {
                    CONFIG.client.extra.killAura.enabled = false;
                    MODULE_MANAGER.getModule(KillAura.class).ifPresent(Module::syncEnabledFromConfig);
                    c.getSource().getEmbedBuilder()
                        .title("Kill Aura Off!")
                        .color(Color.CYAN)
                        .addField("Target Players", CONFIG.client.extra.killAura.targetPlayers ? "on" : "off", false)
                        .addField("Target Hostile Mobs", CONFIG.client.extra.killAura.targetHostileMobs ? "on" : "off", false)
                        .addField("Weapon Switching", CONFIG.client.extra.killAura.switchWeapon ? "on" : "off", false);
                }))
                .then(literal("targetplayers")
                        .then(literal("on").executes(c -> {
                            CONFIG.client.extra.killAura.targetPlayers = true;
                            c.getSource().getEmbedBuilder()
                                .title("Target Players On!")
                                .color(Color.CYAN)
                                .addField("Target Players", CONFIG.client.extra.killAura.targetPlayers ? "on" : "off", false)
                                .addField("Target Hostile Mobs", CONFIG.client.extra.killAura.targetHostileMobs ? "on" : "off", false)
                                .addField("Weapon Switching", CONFIG.client.extra.killAura.switchWeapon ? "on" : "off", false);
                        }))
                        .then(literal("off").executes(c -> {
                            CONFIG.client.extra.killAura.targetPlayers = false;
                            c.getSource().getEmbedBuilder()
                                .title("Target Players Off!")
                                .color(Color.CYAN)
                                .addField("Target Players", CONFIG.client.extra.killAura.targetPlayers ? "on" : "off", false)
                                .addField("Target Hostile Mobs", CONFIG.client.extra.killAura.targetHostileMobs ? "on" : "off", false)
                                .addField("Weapon Switching", CONFIG.client.extra.killAura.switchWeapon ? "on" : "off", false);
                        })))
                .then(literal("targetmobs")
                        .then(literal("on").executes(c -> {
                            CONFIG.client.extra.killAura.targetHostileMobs = true;
                            c.getSource().getEmbedBuilder()
                                .title("Target Mobs On!")
                                .color(Color.CYAN)
                                .addField("Target Players", CONFIG.client.extra.killAura.targetPlayers ? "on" : "off", false)
                                .addField("Target Hostile Mobs", CONFIG.client.extra.killAura.targetHostileMobs ? "on" : "off", false)
                                .addField("Weapon Switching", CONFIG.client.extra.killAura.switchWeapon ? "on" : "off", false);
                        }))
                        .then(literal("off").executes(c -> {
                            CONFIG.client.extra.killAura.targetHostileMobs = true;
                            c.getSource().getEmbedBuilder()
                                .title("Target Mobs Off!")
                                .color(Color.CYAN)
                                .addField("Target Players", CONFIG.client.extra.killAura.targetPlayers ? "on" : "off", false)
                                .addField("Target Hostile Mobs", CONFIG.client.extra.killAura.targetHostileMobs ? "on" : "off", false)
                                .addField("Weapon Switching", CONFIG.client.extra.killAura.switchWeapon ? "on" : "off", false);
                        })))
                .then(literal("weaponswitch")
                        .then(literal("on").executes(c -> {
                            CONFIG.client.extra.killAura.switchWeapon = true;
                            c.getSource().getEmbedBuilder()
                                .title("Weapon Switching On!")
                                .color(Color.CYAN)
                                .addField("Target Players", CONFIG.client.extra.killAura.targetPlayers ? "on" : "off", false)
                                .addField("Target Hostile Mobs", CONFIG.client.extra.killAura.targetHostileMobs ? "on" : "off", false)
                                .addField("Weapon Switching", CONFIG.client.extra.killAura.switchWeapon ? "on" : "off", false);
                        }))
                        .then(literal("off").executes(c -> {
                            CONFIG.client.extra.killAura.switchWeapon = false;
                            c.getSource().getEmbedBuilder()
                                .title("Weapon Switching Off!")
                                .color(Color.CYAN)
                                .addField("Target Players", CONFIG.client.extra.killAura.targetPlayers ? "on" : "off", false)
                                .addField("Target Hostile Mobs", CONFIG.client.extra.killAura.targetHostileMobs ? "on" : "off", false)
                                .addField("Weapon Switching", CONFIG.client.extra.killAura.switchWeapon ? "on" : "off", false);
                        })));
    }

    @Override
    public List<String> aliases() {
        return asList("ka");
    }
}
