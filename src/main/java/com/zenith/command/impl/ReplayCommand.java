package com.zenith.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.command.Command;
import com.zenith.command.CommandCategory;
import com.zenith.command.CommandContext;
import com.zenith.command.CommandUsage;
import com.zenith.module.impl.ReplayMod;
import discord4j.rest.util.Color;

import static com.zenith.Shared.CONFIG;
import static com.zenith.Shared.MODULE_MANAGER;
import static com.zenith.command.ToggleArgumentType.getToggle;
import static com.zenith.command.ToggleArgumentType.toggle;
import static java.util.Arrays.asList;

public class ReplayCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.args(
            "replay",
            CommandCategory.MODULE,
            "Captures a ReplayMod recording",
            asList(
                "start",
                "stop",
                "discordUpload on/off"
            )
        );
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("replay")
            .then(literal("start").executes(c -> {
                var module = MODULE_MANAGER.get(ReplayMod.class);
                if (module.isEnabled()) {
                    c.getSource().getEmbed()
                        .title("Error")
                        .color(Color.RUBY)
                        .description("ReplayMod is already recording");
                    return 1;
                }
                module.enable();
                c.getSource().setNoOutput(true);
                return 1;
            }))
            .then(literal("stop").executes(c -> {
                var module = MODULE_MANAGER.get(ReplayMod.class);
                if (!module.isEnabled()) {
                    c.getSource().getEmbed()
                        .title("Error")
                        .color(Color.RUBY)
                        .description("ReplayMod is not recording");
                    return 1;
                }
                module.disable();
                c.getSource().setNoOutput(true);
                return 1;
            }))
            .then(literal("discordUpload").requires(Command::validateAccountOwner).then(argument("toggle", toggle()).executes(c -> {
                CONFIG.client.extra.replayMod.sendRecordingsToDiscord = getToggle(c, "toggle");
                c.getSource().getEmbed()
                    .title("Discord Upload " + toggleStrCaps(CONFIG.client.extra.replayMod.sendRecordingsToDiscord))
                    .color(Color.CYAN);
                return 1;
            })));
    }
}
