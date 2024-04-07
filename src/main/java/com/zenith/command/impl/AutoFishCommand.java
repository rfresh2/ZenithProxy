package com.zenith.command.impl;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.command.Command;
import com.zenith.command.CommandUsage;
import com.zenith.command.brigadier.CommandCategory;
import com.zenith.command.brigadier.CommandContext;
import com.zenith.discord.Embed;
import com.zenith.module.impl.AutoFish;
import com.zenith.util.math.MathHelper;

import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.zenith.Shared.*;
import static com.zenith.command.brigadier.ToggleArgumentType.getToggle;
import static com.zenith.command.brigadier.ToggleArgumentType.toggle;
import static java.util.Arrays.asList;

public class AutoFishCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.args("autoFish",
                                 CommandCategory.MODULE,
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
        return command("autoFish")
            .then(argument("toggle", toggle()).executes(c -> {
                CONFIG.client.extra.autoFish.enabled = getToggle(c, "toggle");
                MODULE.get(AutoFish.class).syncEnabledFromConfig();
                c.getSource().getEmbed()
                    .title("AutoFish " + toggleStr(CONFIG.client.extra.autoFish.enabled));
                return OK;
            }))
            .then(literal("castDelay")
                      .then(argument("delay", integer(0, 2000)).executes(c -> {
                          CONFIG.client.extra.autoFish.castDelay = IntegerArgumentType.getInteger(c, "delay");
                          c.getSource().getEmbed()
                              .title("Cast Delay set to " + CONFIG.client.extra.autoFish.castDelay + " ticks");
                          return OK;
                      })))
            .then(literal("rotation")
                      .then(literal("sync").executes(c -> {
                          // normalize yaw and pitch to -180 to 180 and -90 to 90
                          CONFIG.client.extra.autoFish.yaw = MathHelper.wrapYaw(CACHE.getPlayerCache().getYaw());
                          CONFIG.client.extra.autoFish.pitch = MathHelper.wrapPitch(CACHE.getPlayerCache().getPitch());
                            c.getSource().getEmbed()
                                .title("Rotation synced to player!");
                      }))
                      .then(argument("yaw", integer(-180, 180))
                                .then(argument("pitch", integer(-90, 90)).executes(c -> {
                                    CONFIG.client.extra.autoFish.yaw = IntegerArgumentType.getInteger(c, "yaw");
                                    CONFIG.client.extra.autoFish.pitch = IntegerArgumentType.getInteger(c, "pitch");
                                    c.getSource().getEmbed()
                                        .title("Rotation set to " + CONFIG.client.extra.autoFish.yaw + " " + CONFIG.client.extra.autoFish.pitch);
                                    return OK;
                                }))));
    }

    @Override
    public void postPopulate(final Embed builder) {
        builder
            .addField("AutoFish", toggleStr(CONFIG.client.extra.autoFish.enabled), false)
            .addField("Cast Delay", CONFIG.client.extra.autoFish.castDelay + " ticks", false)
            .addField("Yaw", CONFIG.client.extra.autoFish.yaw, false)
            .addField("Pitch", CONFIG.client.extra.autoFish.pitch, false)
            .primaryColor();
    }
}
