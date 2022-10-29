package com.zenith.discord.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.discord.command.Command;
import com.zenith.discord.command.CommandContext;
import com.zenith.discord.command.CommandUsage;
import discord4j.rest.util.Color;

import java.util.List;

import static com.zenith.util.Constants.CONFIG;
import static java.util.Arrays.asList;

public class DisplayCoordsCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.full(
                "displayCoords",
                "Sets whether proxy status commands should display coordinates. Only usable by account owner(s).",
                asList("on/off"),
                aliases()
        );
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("displayCoords").requires(Command::validateAccountOwner)
                .then(literal("on").executes(c -> {
                    CONFIG.discord.reportCoords = true;
                    c.getSource().getEmbedBuilder()
                            .title("Coordinates On!")
                            .color(Color.CYAN);
                }))
                .then(literal("off").executes(c -> {
                    CONFIG.discord.reportCoords = false;
                    c.getSource().getEmbedBuilder()
                            .title("Coordinates Off!")
                            .color(Color.CYAN);
                }));
    }

    @Override
    public List<String> aliases() {
        return asList("coords");
    }
}
