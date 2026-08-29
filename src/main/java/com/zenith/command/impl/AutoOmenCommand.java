package com.zenith.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.command.api.Command;
import com.zenith.command.api.CommandCategory;
import com.zenith.command.api.CommandContext;
import com.zenith.command.api.CommandUsage;
import com.zenith.discord.Embed;
import com.zenith.module.impl.AutoOmen;
import com.zenith.util.config.Config.Client.Extra.AutoOmen.Mode;

import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.zenith.Globals.CONFIG;
import static com.zenith.Globals.MODULE;
import static com.zenith.command.brigadier.CustomStringArgumentType.getString;
import static com.zenith.command.brigadier.TimeArgument.time;
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

                Modes:
                * `effectAndRaidInactive`: drink potion only when no omen effect or raid is active
                * `constant`: drink potion at a constant interval. By default the delay matches the omen effect length: 100 seconds (2000 ticks)

                if `consumeFullOmenStack` is disabled, a single omen potion will be left per stack. Stacks are not combined.
                """)
            .usageLines(
                "on/off",
                "mode <effectAndRaidInactive/constant>",
                "consumeFullOmenStack on/off",
                "constantMode delay <ticks>"
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
            }))
            .then(literal("mode").then(argument("mode", enumStrings("effectAndRaidInactive", "constant")).executes(c -> {
                CONFIG.client.extra.autoOmen.mode = strToMode(getString(c, "mode"));
                c.getSource().getEmbed()
                    .title("Mode Set");
            })))
            .then(literal("consumeFullOmenStack").then(argument("toggle", toggle()).executes(c -> {
                CONFIG.client.extra.autoOmen.consumeFullOmenStack = getToggle(c, "toggle");
                c.getSource().getEmbed()
                    .title("Consume Full Omen Stack " + toggleStrCaps(CONFIG.client.extra.autoOmen.consumeFullOmenStack));
            })))
            .then(literal("constantMode").then(literal("delay").then(argument("delay", time(1)).executes(c -> {
                CONFIG.client.extra.autoOmen.constantTicks = getInteger(c, "delay");
                c.getSource().getEmbed()
                    .title("Constant Mode Delay Set");
            }))));
    }

    @Override
    public void defaultEmbed(Embed embed) {
        embed
            .addField("AutoOmen", toggleStr(CONFIG.client.extra.autoOmen.enabled))
            .addField("Mode", modeStr(CONFIG.client.extra.autoOmen.mode))
            .addField("Consume Full Omen Stack", toggleStr(CONFIG.client.extra.autoOmen.consumeFullOmenStack))
            .addField("Constant Mode Delay", CONFIG.client.extra.autoOmen.constantTicks + " ticks")
            .primaryColor();
    }

    private String modeStr(Mode mode) {
        return switch (mode) {
            case EFFECT_AND_RAID_INACTIVE -> "effectAndRaidInactive";
            case CONSTANT -> "constant";
        };
    }

    private Mode strToMode(String mode) {
        return switch (mode) {
            case "effectAndRaidInactive" -> Mode.EFFECT_AND_RAID_INACTIVE;
            case "constant" -> Mode.CONSTANT;
            default -> throw new IllegalArgumentException("unknown mode");
        };
    }
}
