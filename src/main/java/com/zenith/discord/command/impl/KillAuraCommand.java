package com.zenith.discord.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.discord.command.Command;
import com.zenith.discord.command.CommandContext;
import com.zenith.discord.command.CommandUsage;
import discord4j.rest.util.Color;

import java.util.List;

import static com.zenith.util.Constants.CONFIG;
import static java.util.Arrays.asList;

public class KillAuraCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.full("killAura",
                "Attacks entities near the player",
                asList("on/off", "targetPlayers on/off", "targetMobs on/off", "avoidFriendlyMobs on/off"),
                aliases());
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("killaura")
                .then(literal("on").executes(c -> {
                    CONFIG.client.extra.killAura.enabled = true;
                    c.getSource().getEmbedBuilder()
                            .title("Kill Aura On!")
                            .color(Color.CYAN)
                            .addField("Target Players", CONFIG.client.extra.killAura.targetPlayers ? "on" : "off", false)
                            .addField("Target Mobs", CONFIG.client.extra.killAura.targetMobs ? "on" : "off", false)
                            .addField("Avoid Friendly Mobs", CONFIG.client.extra.killAura.avoidFriendlyMobs ? "on" : "off", false);

                }))
                .then(literal("off").executes(c -> {
                    CONFIG.client.extra.killAura.enabled = false;
                    c.getSource().getEmbedBuilder()
                            .title("Kill Aura Off!")
                            .color(Color.CYAN)
                            .addField("Target Players", CONFIG.client.extra.killAura.targetPlayers ? "on" : "off", false)
                            .addField("Target Mobs", CONFIG.client.extra.killAura.targetMobs ? "on" : "off", false)
                            .addField("Avoid Friendly Mobs", CONFIG.client.extra.killAura.avoidFriendlyMobs ? "on" : "off", false);
                }))
                .then(literal("targetplayers")
                        .then(literal("on").executes(c -> {
                            CONFIG.client.extra.killAura.targetPlayers = true;
                            c.getSource().getEmbedBuilder()
                                    .title("Target Players On!")
                                    .color(Color.CYAN)
                                    .addField("Target Players", CONFIG.client.extra.killAura.targetPlayers ? "on" : "off", false)
                                    .addField("Target Mobs", CONFIG.client.extra.killAura.targetMobs ? "on" : "off", false)
                                    .addField("Avoid Friendly Mobs", CONFIG.client.extra.killAura.avoidFriendlyMobs ? "on" : "off", false);
                        }))
                        .then(literal("off").executes(c -> {
                            CONFIG.client.extra.killAura.targetPlayers = false;
                            c.getSource().getEmbedBuilder()
                                    .title("Target Players Off!")
                                    .color(Color.CYAN)
                                    .addField("Target Players", CONFIG.client.extra.killAura.targetPlayers ? "on" : "off", false)
                                    .addField("Target Mobs", CONFIG.client.extra.killAura.targetMobs ? "on" : "off", false)
                                    .addField("Avoid Friendly Mobs", CONFIG.client.extra.killAura.avoidFriendlyMobs ? "on" : "off", false);
                        })))
                .then(literal("targetmobs")
                        .then(literal("on").executes(c -> {
                            CONFIG.client.extra.killAura.targetMobs = true;
                            c.getSource().getEmbedBuilder()
                                    .title("Target Mobs On!")
                                    .color(Color.CYAN)
                                    .addField("Target Players", CONFIG.client.extra.killAura.targetPlayers ? "on" : "off", false)
                                    .addField("Target Mobs", CONFIG.client.extra.killAura.targetMobs ? "on" : "off", false)
                                    .addField("Avoid Friendly Mobs", CONFIG.client.extra.killAura.avoidFriendlyMobs ? "on" : "off", false);
                        }))
                        .then(literal("off").executes(c -> {
                            CONFIG.client.extra.killAura.targetMobs = true;
                            c.getSource().getEmbedBuilder()
                                    .title("Target Mobs Off!")
                                    .color(Color.CYAN)
                                    .addField("Target Players", CONFIG.client.extra.killAura.targetPlayers ? "on" : "off", false)
                                    .addField("Target Mobs", CONFIG.client.extra.killAura.targetMobs ? "on" : "off", false)
                                    .addField("Avoid Friendly Mobs", CONFIG.client.extra.killAura.avoidFriendlyMobs ? "on" : "off", false);
                        })))
                .then(literal("avoidfriendlymobs")
                        .then(literal("on").executes(c -> {
                            CONFIG.client.extra.killAura.avoidFriendlyMobs = true;
                            c.getSource().getEmbedBuilder()
                                    .title("Avoid Friendly Mobs On!")
                                    .color(Color.CYAN)
                                    .addField("Target Players", CONFIG.client.extra.killAura.targetPlayers ? "on" : "off", false)
                                    .addField("Target Mobs", CONFIG.client.extra.killAura.targetMobs ? "on" : "off", false)
                                    .addField("Avoid Friendly Mobs", CONFIG.client.extra.killAura.avoidFriendlyMobs ? "on" : "off", false);
                        }))
                        .then(literal("off").executes(c -> {
                            CONFIG.client.extra.killAura.avoidFriendlyMobs = false;
                            c.getSource().getEmbedBuilder()
                                    .title("Avoid Friendly Mobs Off!")
                                    .color(Color.CYAN)
                                    .addField("Target Players", CONFIG.client.extra.killAura.targetPlayers ? "on" : "off", false)
                                    .addField("Target Mobs", CONFIG.client.extra.killAura.targetMobs ? "on" : "off", false)
                                    .addField("Avoid Friendly Mobs", CONFIG.client.extra.killAura.avoidFriendlyMobs ? "on" : "off", false);
                        })));
    }

    @Override
    public List<String> aliases() {
        return asList("ka");
    }
}
