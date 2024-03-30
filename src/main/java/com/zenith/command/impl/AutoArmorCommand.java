package com.zenith.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.command.Command;
import com.zenith.command.CommandUsage;
import com.zenith.command.brigadier.CommandCategory;
import com.zenith.command.brigadier.CommandContext;
import com.zenith.module.impl.AutoArmor;
import discord4j.rest.util.Color;

import static com.zenith.Shared.CONFIG;
import static com.zenith.Shared.MODULE;
import static com.zenith.command.brigadier.ToggleArgumentType.getToggle;
import static com.zenith.command.brigadier.ToggleArgumentType.toggle;
import static java.util.Arrays.asList;

public class AutoArmorCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.args(
            "autoArmor",
            CommandCategory.MODULE,
            "Automatically equips the best armor in your inventory.",
            asList(
                "on/off"
            )
        );
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("autoArmor")
            .then(argument("toggle", toggle()).executes(c -> {
                CONFIG.client.extra.autoArmor.enabled = getToggle(c, "toggle");
                MODULE.get(AutoArmor.class).syncEnabledFromConfig();
                c.getSource().getEmbed()
                    .title("AutoArmor " + toggleStrCaps(CONFIG.client.extra.autoArmor.enabled))
                    .color(Color.CYAN);
                return 1;
            }));
    }
}
