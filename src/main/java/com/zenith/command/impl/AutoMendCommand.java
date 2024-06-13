package com.zenith.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.command.Command;
import com.zenith.command.CommandUsage;
import com.zenith.command.brigadier.CommandCategory;
import com.zenith.command.brigadier.CommandContext;
import com.zenith.discord.Embed;
import com.zenith.module.impl.AutoMend;

import static com.zenith.Shared.CONFIG;
import static com.zenith.Shared.MODULE;
import static com.zenith.command.brigadier.ToggleArgumentType.getToggle;
import static com.zenith.command.brigadier.ToggleArgumentType.toggle;
import static java.util.Arrays.asList;

public class AutoMendCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.args(
            "autoMend",
            CommandCategory.MODULE,
            """
            Equips items that are both damaged and have the mending enchantment to the offhand.
            
            Can be enabled while at an XP farm to repair items in your inventory.
            """,
            asList(
                "on/off"
            )
        );
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("autoMend")
            .then(argument("toggle", toggle()).executes(c -> {
                CONFIG.client.extra.autoMend.enabled = getToggle(c, "toggle");
                MODULE.get(AutoMend.class).syncEnabledFromConfig();
                c.getSource().getEmbed()
                    .title("AutoMend " + toggleStrCaps(CONFIG.client.extra.autoMend.enabled));
                return OK;
            }));
    }

    @Override
    public void postPopulate(Embed embed) {
        embed
            .addField("Auto Mend", toggleStr(CONFIG.client.extra.autoMend.enabled), false)
            .primaryColor();
    }
}
