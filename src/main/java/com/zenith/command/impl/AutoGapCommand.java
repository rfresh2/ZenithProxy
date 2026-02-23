package com.zenith.command.impl;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.command.api.Command;
import com.zenith.command.api.CommandCategory;
import com.zenith.command.api.CommandContext;
import com.zenith.command.api.CommandUsage;
import com.zenith.module.impl.AutoGap;

import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.zenith.Globals.CONFIG;
import static com.zenith.Globals.MODULE;
import static com.zenith.command.brigadier.ToggleArgumentType.getToggle;
import static com.zenith.command.brigadier.ToggleArgumentType.toggle;

public class AutoGapCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.builder()
            .name("autoGap")
            .category(CommandCategory.MODULE)
            .description("Automatically eats golden apples when health is below a set threshold.")
            .usageLines(
                "on/off",
                "health <int>",
                "onFire on/off"
            )
            .build();
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("autoGap")
            .then(argument("toggle", toggle()).executes(c -> {
                CONFIG.client.extra.autoGap.enabled = getToggle(c, "toggle");
                MODULE.get(AutoGap.class).syncEnabledFromConfig();
                c.getSource().getEmbed()
                    .title("AutoGap " + toggleStrCaps(CONFIG.client.extra.autoGap.enabled));
                return OK;
            }))
            .then(literal("health").then(argument("health", integer(-1)).executes(c -> {
                CONFIG.client.extra.autoGap.healthThreshold = IntegerArgumentType.getInteger(c, "health");
                c.getSource().getEmbed()
                    .title("AutoGap Health Threshold Set");
                return OK;
            })))
            .then(literal("onFire").then(argument("toggle", toggle()).executes(c -> {
                CONFIG.client.extra.autoGap.onFire = getToggle(c, "toggle");
                c.getSource().getEmbed()
                    .title("AutoGap On Fire " + toggleStrCaps(CONFIG.client.extra.autoGap.onFire));
                return OK;
            })));
    }

    @Override
    public void defaultHandler(final CommandContext context) {
        if (context.getData().containsKey("noDefaultEmbed")) return;
        context.getEmbed()
            .addField("AutoGap", toggleStr(CONFIG.client.extra.autoGap.enabled))
            .addField("Health Threshold", CONFIG.client.extra.autoGap.healthThreshold)
            .addField("On Fire", toggleStr(CONFIG.client.extra.autoGap.onFire))
            .primaryColor();
    }
}
