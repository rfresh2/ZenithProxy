package com.zenith.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.command.Command;
import com.zenith.command.CommandUsage;
import com.zenith.command.brigadier.CommandCategory;
import com.zenith.command.brigadier.CommandContext;

import static com.zenith.Shared.CONFIG;
import static com.zenith.command.brigadier.ToggleArgumentType.getToggle;
import static com.zenith.command.brigadier.ToggleArgumentType.toggle;
import static java.util.Arrays.asList;

public class DisplayCoordsCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.full(
            "displayCoords",
            CommandCategory.MANAGE,
            "Sets whether proxy status commands should display coordinates. Only usable by account owner(s).",
            asList("on/off"),
            asList("coords")
        );
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("displayCoords").requires(Command::validateAccountOwner)
            .then(argument("toggle", toggle()).executes(c -> {
                CONFIG.discord.reportCoords = getToggle(c, "toggle");
                c.getSource().getEmbed()
                    .title("Coordinates " + toggleStrCaps(CONFIG.discord.reportCoords))
                    .primaryColor();
                return 1;
            }));
    }
}
