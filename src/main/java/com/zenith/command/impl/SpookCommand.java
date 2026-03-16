package com.zenith.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.command.api.Command;
import com.zenith.command.api.CommandCategory;
import com.zenith.command.api.CommandContext;
import com.zenith.command.api.CommandUsage;
import com.zenith.discord.Embed;
import com.zenith.module.impl.Spook;
import com.zenith.util.config.Config;

import static com.zenith.Globals.CONFIG;
import static com.zenith.Globals.MODULE;
import static com.zenith.command.brigadier.ToggleArgumentType.getToggle;
import static com.zenith.command.brigadier.ToggleArgumentType.toggle;

public class SpookCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.builder()
            .name("spook")
            .category(CommandCategory.MODULE)
            .description("""
            Rotates and stares at players in visual range.

            Can often confuse other players in-game into thinking you are a real player.
            """)
            .usageLines(
                "on/off",
                "mode <visualRange/nearest>"
            )
            .build();
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("spook")
            .then(argument("toggle", toggle()).executes(c -> {
                CONFIG.client.extra.spook.enabled = getToggle(c, "toggle");
                MODULE.get(Spook.class).syncEnabledFromConfig();
                c.getSource().getEmbed()
                    .title("Spook " + toggleStrCaps(CONFIG.client.extra.spook.enabled));
                return OK;
            }))
            .then(literal("mode").then(argument("modeArg", enumStrings("nearest", "visualRange")).executes(c -> {
                var arg = c.getArgument("modeArg", String.class);
                var mode = switch (arg) {
                    case "nearest" -> Config.Client.Extra.Spook.TargetingMode.NEAREST;
                    case "visualRange" -> Config.Client.Extra.Spook.TargetingMode.VISUAL_RANGE;
                    default -> null;
                };
                if (mode == null) {
                    c.getSource().getEmbed()
                        .title("Invalid mode: " + arg);
                    return ERROR;
                }
                CONFIG.client.extra.spook.spookTargetingMode = mode;
                c.getSource().getEmbed()
                    .title("Spook Mode Updated!");
                return OK;
            })));
    }

    @Override
    public void defaultEmbed(final Embed builder) {
        builder
            .addField("Spook", toggleStr(CONFIG.client.extra.spook.enabled), false)
            .addField("Mode", CONFIG.client.extra.spook.spookTargetingMode.toString().toLowerCase(), false)
            .primaryColor();
    }
}
