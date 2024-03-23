package com.zenith.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.command.Command;
import com.zenith.command.CommandUsage;
import com.zenith.command.brigadier.CommandCategory;
import com.zenith.command.brigadier.CommandContext;
import com.zenith.feature.pathing.raycast.RaycastHelper;
import discord4j.rest.util.Color;

public class RaycastCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.simple(
            "raycast",
            CommandCategory.INFO,
            "Raycast to the block in front of you"
        );
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("raycast").executes(c -> {
            var result = RaycastHelper.playerBlockRaycast(4.5, false);
            c.getSource().getEmbed()
                .title("Raycast Result")
                .addField("Hit", result.hit(), false)
                .addField("Block", result.block().toString(), false)
                .addField("Pos", result.x() + ", " + result.y() + ", " + result.z(), false)
                .addField("Direction", result.direction().name(), false)
                .color(Color.CYAN);
        })
            .then(literal("e").executes(c -> {
                var result = RaycastHelper.playerEntityRaycast(4.5);
                c.getSource().getEmbed()
                    .title("Raycast Result")
                    .addField("Hit", result.hit(), false)
                    .addField("Entity", result.entity() != null ? result.entity().getEntityId() : -1, false)
                    .color(Color.CYAN);
            }));
    }
}
