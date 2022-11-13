package com.zenith.discord.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.discord.command.Command;
import com.zenith.discord.command.CommandContext;
import com.zenith.discord.command.CommandUsage;
import discord4j.rest.util.Color;

import java.util.List;

import static com.zenith.util.Constants.CONFIG;
import static java.util.Arrays.asList;

public class AntiAFKCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.full(
                "antiAFK",
                "Configure the AntiAFK feature",
                asList("on/off", "safeWalk on/off", "gravity on/off", "safeGravity on/off"),
                aliases()
        );
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("antiAFK")
                .then(literal("on").executes(c -> {
                    CONFIG.client.extra.antiafk.enabled = true;
                    c.getSource().getEmbedBuilder()
                            .title("AntiAFK On!")
                            .addField("AntiAFK", CONFIG.client.extra.antiafk.enabled ? "on" : "off", false)
                            .addField("SafeWalk", CONFIG.client.extra.antiafk.actions.safeWalk ? "on" : "off", false)
                            .addField("Gravity", CONFIG.client.extra.antiafk.actions.gravity ? "on" : "off", false)
                            .addField("SafeGravity", CONFIG.client.extra.antiafk.actions.safeGravity ? "on" : "off", false)
                            .color(Color.CYAN);
                }))
                .then(literal("off").executes(c -> {
                    CONFIG.client.extra.antiafk.enabled = false;
                    c.getSource().getEmbedBuilder()
                            .title("AntiAFK Off!")
                            .addField("AntiAFK", CONFIG.client.extra.antiafk.enabled ? "on" : "off", false)
                            .addField("SafeWalk", CONFIG.client.extra.antiafk.actions.safeWalk ? "on" : "off", false)
                            .addField("Gravity", CONFIG.client.extra.antiafk.actions.gravity ? "on" : "off", false)
                            .addField("SafeGravity", CONFIG.client.extra.antiafk.actions.safeGravity ? "on" : "off", false)
                            .color(Color.CYAN);
                }))
                .then(literal("safewalk")
                        .then(literal("on").executes(c -> {
                            CONFIG.client.extra.antiafk.actions.safeWalk = true;
                            c.getSource().getEmbedBuilder()
                                    .title("SafeWalk On!")
                                    .addField("AntiAFK", CONFIG.client.extra.antiafk.enabled ? "on" : "off", false)
                                    .addField("SafeWalk", CONFIG.client.extra.antiafk.actions.safeWalk ? "on" : "off", false)
                                    .addField("Gravity", CONFIG.client.extra.antiafk.actions.gravity ? "on" : "off", false)
                                    .addField("SafeGravity", CONFIG.client.extra.antiafk.actions.safeGravity ? "on" : "off", false)
                                    .color(Color.CYAN);
                        }))
                        .then(literal("off").executes(c -> {
                            CONFIG.client.extra.antiafk.actions.safeWalk = false;
                            c.getSource().getEmbedBuilder()
                                    .title("SafeWalk Off!")
                                    .addField("AntiAFK", CONFIG.client.extra.antiafk.enabled ? "on" : "off", false)
                                    .addField("SafeWalk", CONFIG.client.extra.antiafk.actions.safeWalk ? "on" : "off", false)
                                    .addField("Gravity", CONFIG.client.extra.antiafk.actions.gravity ? "on" : "off", false)
                                    .addField("SafeGravity", CONFIG.client.extra.antiafk.actions.safeGravity ? "on" : "off", false)
                                    .color(Color.CYAN);
                        })))
                .then(literal("gravity")
                        .then(literal("on").executes(c -> {
                            CONFIG.client.extra.antiafk.actions.gravity = true;
                            c.getSource().getEmbedBuilder()
                                    .title("Gravity On!")
                                    .addField("AntiAFK", CONFIG.client.extra.antiafk.enabled ? "on" : "off", false)
                                    .addField("SafeWalk", CONFIG.client.extra.antiafk.actions.safeWalk ? "on" : "off", false)
                                    .addField("Gravity", CONFIG.client.extra.antiafk.actions.gravity ? "on" : "off", false)
                                    .addField("SafeGravity", CONFIG.client.extra.antiafk.actions.safeGravity ? "on" : "off", false)
                                    .color(Color.CYAN);
                        }))
                        .then(literal("off").executes(c -> {
                            CONFIG.client.extra.antiafk.actions.gravity = false;
                            c.getSource().getEmbedBuilder()
                                    .title("Gravity Off!")
                                    .addField("AntiAFK", CONFIG.client.extra.antiafk.enabled ? "on" : "off", false)
                                    .addField("SafeWalk", CONFIG.client.extra.antiafk.actions.safeWalk ? "on" : "off", false)
                                    .addField("Gravity", CONFIG.client.extra.antiafk.actions.gravity ? "on" : "off", false)
                                    .addField("SafeGravity", CONFIG.client.extra.antiafk.actions.safeGravity ? "on" : "off", false)
                                    .color(Color.CYAN);
                        })))
                .then(literal("safegravity")
                        .then(literal("on").executes(c -> {
                            CONFIG.client.extra.antiafk.actions.safeGravity = true;
                            c.getSource().getEmbedBuilder()
                                    .title("SafeGravity On!")
                                    .addField("AntiAFK", CONFIG.client.extra.antiafk.enabled ? "on" : "off", false)
                                    .addField("SafeWalk", CONFIG.client.extra.antiafk.actions.safeWalk ? "on" : "off", false)
                                    .addField("Gravity", CONFIG.client.extra.antiafk.actions.gravity ? "on" : "off", false)
                                    .addField("SafeGravity", CONFIG.client.extra.antiafk.actions.safeGravity ? "on" : "off", false)
                                    .color(Color.CYAN);
                        }))
                        .then(literal("off").executes(c -> {
                            CONFIG.client.extra.antiafk.actions.safeGravity = false;
                            c.getSource().getEmbedBuilder()
                                    .title("SafeGravity Off!")
                                    .addField("AntiAFK", CONFIG.client.extra.antiafk.enabled ? "on" : "off", false)
                                    .addField("SafeWalk", CONFIG.client.extra.antiafk.actions.safeWalk ? "on" : "off", false)
                                    .addField("Gravity", CONFIG.client.extra.antiafk.actions.gravity ? "on" : "off", false)
                                    .addField("SafeGravity", CONFIG.client.extra.antiafk.actions.safeGravity ? "on" : "off", false)
                                    .color(Color.CYAN);
                        })));
    }

    @Override
    public List<String> aliases() {
        return asList("afk");
    }
}
