package com.zenith.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.command.Command;
import com.zenith.command.CommandCategory;
import com.zenith.command.CommandContext;
import com.zenith.command.CommandUsage;
import com.zenith.discord.Embed;
import com.zenith.module.impl.ReplayMod;
import discord4j.rest.util.Color;

import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
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
            """
            Captures a ReplayMod recording.
            
            Replays can optionally be uploaded to discord if they are under the discord message size limit.
            
            A `maxRecordingTime` of 0 means there is no limit, however, recording are always stopped on disconnects.
            
            `autoStart` will automatically start a new recording when the proxy connects.
            """,
            asList(
                "start",
                "stop",
                "discordUpload on/off",
                "maxRecordingTime <minutes>",
                "autoStart on/off"
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
                    .title("Discord Upload " + toggleStrCaps(CONFIG.client.extra.replayMod.sendRecordingsToDiscord));
                return 1;
            })))
            .then(literal("maxRecordingTime").then(argument("minutes", integer(0, 60 * 6)).executes(c -> {
                CONFIG.client.extra.replayMod.maxRecordingTimeMins = getInteger(c, "minutes");
                c.getSource().getEmbed()
                    .title("Max Recording Time Set");
                return 1;
            })))
            .then(literal("autoStart").then(argument("toggle", toggle()).executes(c -> {
                CONFIG.client.extra.replayMod.autoStartRecording = getToggle(c, "toggle");
                c.getSource().getEmbed()
                    .title("Auto Start " + toggleStrCaps(CONFIG.client.extra.replayMod.autoStartRecording));
                return 1;
            })));
    }

    @Override
    public void postPopulate(final Embed embed) {
        embed
            .color(Color.CYAN)
            .addField("Discord Upload", toggleStr(CONFIG.client.extra.replayMod.sendRecordingsToDiscord), false)
            .addField("Max Recording Time", getMaxRecordingTimeStr(), false)
            .addField("Auto Start", toggleStr(CONFIG.client.extra.replayMod.autoStartRecording), false);
    }

    private String getMaxRecordingTimeStr() {
        if (CONFIG.client.extra.replayMod.maxRecordingTimeMins <= 0) {
            return "No Limit";
        } else {
            return CONFIG.client.extra.replayMod.maxRecordingTimeMins + " minutes";
        }
    }
}
