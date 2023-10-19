package com.zenith.command.impl;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.command.Command;
import com.zenith.command.CommandContext;
import com.zenith.command.CommandUsage;
import com.zenith.module.Module;
import com.zenith.module.impl.AutoFish;
import discord4j.core.spec.EmbedCreateSpec;

import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.zenith.Shared.*;
import static com.zenith.command.ToggleArgumentType.getToggle;
import static com.zenith.command.ToggleArgumentType.toggle;
import static java.util.Arrays.asList;

public class AutoFishCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.args("autoFish",
                                 "Configures the AutoFish module",
                                 asList(
                                     "on/off",
                                     "castDelay <ticks>",
                                     "rotation <yaw> <pitch>",
                                     "rotation sync"
                                 ));
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("autofish")
            .then(argument("toggle", toggle()).executes(c -> {
                CONFIG.client.extra.autoFish.enabled = getToggle(c, "toggle");
                MODULE_MANAGER.getModule(AutoFish.class).ifPresent(Module::syncEnabledFromConfig);
                c.getSource().getEmbedBuilder()
                    .title("AutoFish " + (CONFIG.client.extra.autoFish.enabled ? "On!" : "Off!"));
                return 1;
            }))
            .then(literal("castDelay")
                      .then(argument("delay", integer(0, 2000)).executes(c -> {
                          CONFIG.client.extra.autoFish.castDelay = IntegerArgumentType.getInteger(c, "delay");
                          c.getSource().getEmbedBuilder()
                              .title("Cast Delay set to " + CONFIG.client.extra.autoFish.castDelay + " ticks");
                          return 1;
                      })))
            .then(literal("rotation")
                      .then(literal("sync").executes(c -> {
                          // normalize yaw and pitch to -180 to 180 and -90 to 90
                          CONFIG.client.extra.autoFish.yaw = ((180.0f + CACHE.getPlayerCache().getYaw()) % 360.0f) - 180.0f;
                          CONFIG.client.extra.autoFish.pitch = ((90.0f + CACHE.getPlayerCache().getPitch()) % 180.0f) - 90.0f;
                            c.getSource().getEmbedBuilder()
                                .title("Rotation synced to player!");
                      }))
                      .then(argument("yaw", integer(-180, 180))
                                .then(argument("pitch", integer(-90, 90)).executes(c -> {
                                    CONFIG.client.extra.autoFish.yaw = IntegerArgumentType.getInteger(c, "yaw");
                                    CONFIG.client.extra.autoFish.pitch = IntegerArgumentType.getInteger(c, "pitch");
                                    c.getSource().getEmbedBuilder()
                                        .title("Rotation set to " + CONFIG.client.extra.autoFish.yaw + " " + CONFIG.client.extra.autoFish.pitch);
                                    return 1;
                                }))));
    }

    @Override
    public void postPopulate(final EmbedCreateSpec.Builder builder) {
        builder
            .addField("AutoFish", toggleStr(CONFIG.client.extra.autoFish.enabled), false)
            .addField("Cast Delay", CONFIG.client.extra.autoFish.castDelay + " ticks", false)
            .addField("Yaw", ""+CONFIG.client.extra.autoFish.yaw, false)
            .addField("Pitch", ""+CONFIG.client.extra.autoFish.pitch, false);
    }
}
