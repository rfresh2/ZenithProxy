package com.zenith.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.command.Command;
import com.zenith.command.CommandUsage;
import com.zenith.command.brigadier.CommandCategory;
import com.zenith.command.brigadier.CommandContext;
import com.zenith.discord.Embed;
import com.zenith.module.impl.Spook;
import com.zenith.util.Config;

import static com.zenith.Shared.CONFIG;
import static com.zenith.Shared.MODULE;
import static com.zenith.command.brigadier.ToggleArgumentType.getToggle;
import static com.zenith.command.brigadier.ToggleArgumentType.toggle;
import static java.util.Arrays.asList;

public class SpookCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.args(
            "spook",
            CommandCategory.MODULE,
            """
            Rotates and stares at players in visual range.
            
            Can often confuse other players in-game into thinking you are a real player.
            """,
            asList(
                "on/off",
                "mode <visualRange/nearest>"
            )
        );
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
            .then(literal("mode")
                      .then(literal("nearest").executes(c -> {
                          CONFIG.client.extra.spook.spookTargetingMode = Config.Client.Extra.Spook.TargetingMode.NEAREST;
                          c.getSource().getEmbed()
                              .title("Spook Mode Updated!");
                      }))
                      .then(literal("visualrange").executes(c -> {
                          CONFIG.client.extra.spook.spookTargetingMode = Config.Client.Extra.Spook.TargetingMode.VISUAL_RANGE;
                          c.getSource().getEmbed()
                              .title("Spook Mode Updated!");
                      })));
    }

    @Override
    public void postPopulate(final Embed builder) {
        builder
            .addField("Spook", toggleStr(CONFIG.client.extra.spook.enabled), false)
            .addField("Mode", CONFIG.client.extra.spook.spookTargetingMode.toString().toLowerCase(), false)
            .primaryColor();
    }
}
