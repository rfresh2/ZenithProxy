package com.zenith.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.command.api.Command;
import com.zenith.command.api.CommandCategory;
import com.zenith.command.api.CommandContext;
import com.zenith.command.api.CommandUsage;
import com.zenith.discord.Embed;
import com.zenith.module.impl.AutoOmen;

import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
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
                """)
            .usageLines(
                "on/off",
                "whileRaidActive on/off",
                "whileOmenActive on/off",
                "consumeFullOmenStack on/off"
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
            .then(literal("whileRaidActive").then(argument("toggle", toggle()).executes(c -> {
                CONFIG.client.extra.autoOmen.whileRaidActive = getToggle(c, "toggle");
                c.getSource().getEmbed()
                    .title("AutoOmen While Raid Active " + toggleStrCaps(CONFIG.client.extra.autoOmen.whileRaidActive));
            })))
            .then(literal("whileOmenActive").then(argument("toggle", toggle()).executes(c -> {;
                CONFIG.client.extra.autoOmen.whileOmenActive = getToggle(c, "toggle");
                c.getSource().getEmbed()
                    .title("AutoOmen While Omen Active " + toggleStrCaps(CONFIG.client.extra.autoOmen.whileOmenActive));
            })))
            .then(literal("consumeFullOmenStack").then(argument("toggle", toggle()).executes(c -> {
                CONFIG.client.extra.autoOmen.consumeFullOmenStack = getToggle(c, "toggle");
                c.getSource().getEmbed()
                    .title("Consume Full Omen Stack " + toggleStrCaps(CONFIG.client.extra.autoOmen.consumeFullOmenStack));
            })))
            .then(literal("raidCooldown").then(argument("ms", integer(1)).executes(c -> {
                CONFIG.client.extra.autoOmen.raidCooldownMs = getInteger(c, "ms");
                c.getSource().getEmbed()
                    .title("Raid Cooldown Set");
            })))
            .then(literal("omenCooldown").then(argument("ms", integer(1)).executes(c -> {
                CONFIG.client.extra.autoOmen.omenCooldownMs = getInteger(c, "ms");
                c.getSource().getEmbed()
                    .title("Omen Cooldown Set");
            })));
    }

    @Override
    public void defaultEmbed(Embed embed) {
        embed
            .addField("AutoOmen", toggleStr(CONFIG.client.extra.autoOmen.enabled))
            .addField("While Raid Active", toggleStr(CONFIG.client.extra.autoOmen.whileRaidActive))
            .addField("While Omen Active", toggleStr(CONFIG.client.extra.autoOmen.whileOmenActive))
            .addField("Consume Full Omen Stack", toggleStr(CONFIG.client.extra.autoOmen.consumeFullOmenStack))
            .primaryColor();
    }
}
