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
                asList("on/off"),
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
                            .color(Color.CYAN);
                }))
                .then(literal("off").executes(c -> {
                    CONFIG.client.extra.antiafk.enabled = false;
                    c.getSource().getEmbedBuilder()
                            .title("AntiAFK Off!")
                            .color(Color.CYAN);
                }));
    }

    @Override
    public List<String> aliases() {
        return asList("afk");
    }
}
