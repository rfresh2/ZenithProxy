package com.zenith.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.command.api.Command;
import com.zenith.command.api.CommandCategory;
import com.zenith.command.api.CommandContext;
import com.zenith.command.api.CommandUsage;
import com.zenith.discord.Embed;
import com.zenith.module.impl.AutoOmen;

import static com.zenith.Globals.CONFIG;
import static com.zenith.Globals.MODULE;
import static com.zenith.command.brigadier.ToggleArgumentType.getToggle;
import static com.zenith.command.brigadier.ToggleArgumentType.toggle;

public class AutoOmenCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.builder()
            .name("autoOmen")
            .category(CommandCategory.MODULE)
            .description("""
                Automatically drinks Bad Omen potions in the inventory.
                
                Useful for raid farms on MC 1.21+ servers.
                
                `whileRaidOmen`: Allows drinking Bad Omen potions even while the Raid Omen effect is active.
                Note: Enabling this may cause a loop where the player keeps drinking potions, constantly resetting the 30-second effect timer, and preventing the raid from ever starting.
                """)
            .usageLines(
                "on/off",
                "whileRaidOmen on/off"
            )
            .build();
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("autoOmen")
            .then(argument("toggle", toggle()).executes(c -> {
                CONFIG.client.extra.autoOmen.enabled = getToggle(c, "toggle");
                c.getSource().getEmbed()
                    .title("AutoOmen " + toggleStrCaps(CONFIG.client.extra.autoOmen.enabled));
                MODULE.get(AutoOmen.class).syncEnabledFromConfig();
                return OK;
            }))
            .then(literal("whileRaidOmen")
                .then(argument("toggle", toggle()).executes(c -> {
                    CONFIG.client.extra.autoOmen.whileRaidOmen = getToggle(c, "toggle");
                    c.getSource().getEmbed()
                        .title("AutoOmen While Raid Omen " + toggleStrCaps(CONFIG.client.extra.autoOmen.whileRaidOmen));
                    MODULE.get(AutoOmen.class).syncEnabledFromConfig();
                    return OK;
            })));
    }

    @Override
    public void defaultEmbed(Embed embed) {
        embed
            .addField("AutoOmen", toggleStr(CONFIG.client.extra.autoOmen.enabled), false)
            .addField("While Raid Omen", toggleStr(CONFIG.client.extra.autoOmen.whileRaidOmen), false)
            .primaryColor();
    }
}
