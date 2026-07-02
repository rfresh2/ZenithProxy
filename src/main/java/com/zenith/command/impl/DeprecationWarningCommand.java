package com.zenith.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.command.api.Command;
import com.zenith.command.api.CommandCategory;
import com.zenith.command.api.CommandContext;
import com.zenith.command.api.CommandUsage;
import com.zenith.discord.Embed;

import static com.zenith.Globals.CONFIG;
import static com.zenith.command.brigadier.ToggleArgumentType.getToggle;
import static com.zenith.command.brigadier.ToggleArgumentType.toggle;

public class DeprecationWarningCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.builder()
            .name("deprecationWarning")
            .category(CommandCategory.MANAGE)
            .description("""
               Configures the ZenithProxy deprecation warning notifications
               """)
            .usageLines(
                "on/off"
            )
            .build();
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("deprecationWarning")
            .then(argument("toggle", toggle()).executes(c -> {
                CONFIG.deprecationWarning_26_1_2 = getToggle(c, "toggle");
                c.getSource().getEmbed()
                    .title("Deprecation Warning " + toggleStrCaps(CONFIG.deprecationWarning_26_1_2));
            }));
    }

    @Override
    public void defaultEmbed(Embed embed) {
        embed
            .addField("Notification", toggleStr(CONFIG.deprecationWarning_26_1_2))
            .primaryColor();
    }
}
