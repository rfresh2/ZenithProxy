package com.zenith.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.Proxy;
import com.zenith.command.api.Command;
import com.zenith.command.api.CommandCategory;
import com.zenith.command.api.CommandContext;
import com.zenith.command.api.CommandUsage;
import com.zenith.discord.Embed;
import com.zenith.feature.player.ClickTarget;
import com.zenith.feature.player.Input;
import com.zenith.feature.player.InputRequest;
import com.zenith.util.config.Config.Client.Extra.Click.HoldClickTarget;
import com.zenith.util.config.Config.Client.Extra.Click.HoldRightClickMode;

import static com.mojang.brigadier.arguments.FloatArgumentType.floatArg;
import static com.mojang.brigadier.arguments.FloatArgumentType.getFloat;
import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.zenith.Globals.CONFIG;
import static com.zenith.Globals.INPUTS;
import static com.zenith.command.brigadier.CustomStringArgumentType.getString;
import static com.zenith.command.brigadier.ToggleArgumentType.getToggle;
import static com.zenith.command.brigadier.ToggleArgumentType.toggle;

public class ClickCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.builder()
            .name("click")
            .category(CommandCategory.MODULE)
            .description("""
             Simulates a click to the block or entity in front of you
             """)
            .usageLines(
                "left",
                "left target <any/none/entity/block>",
                "left hold",
                "left hold interval <ticks>",
                "right",
                "right target <any/none/entity/block>",
                "right hold",
                "right hold <mainHand/offHand/alternate>",
                "right hold interval <ticks>",
                "addedBlockReach <float>",
                "addedEntityReach <float>",
                "hold forceRotation on/off",
                "hold forceRotation <yaw> <pitch>",
                "hold sneak on/off",
                "hold target <any/none/entity/block>",
                "stop"
            )
            .build();
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("click")
            .then(literal("stop").executes(c -> {
                CONFIG.client.extra.click.holdLeftClick = false;
                CONFIG.client.extra.click.holdRightClick = false;
                c.getSource().getEmbed()
                    .title("Click Hold Off")
                    .primaryColor();
                return OK;
            }))
            .then(literal("left").executes(c -> {
                    if (!Proxy.getInstance().isConnected()) {
                        c.getSource().getEmbed()
                            .title("Not Connected")
                            .description("Must be connected to click");
                        return ERROR;
                    }
                    INPUTS.submit(InputRequest.builder()
                        .owner(this)
                        .input(Input.builder()
                            .leftClick(true)
                            .build())
                        .priority(100000)
                        .build());
                    c.getSource().getEmbed()
                        .title("Left Clicked")
                        .primaryColor();
                    return OK;
                })
                .then(literal("target").then(argument("targetType", enumStrings(HoldClickTarget.values())).executes(c -> {
                    if (!Proxy.getInstance().isConnected()) {
                        c.getSource().getEmbed()
                            .title("Not Connected")
                            .description("Must be connected to click");
                        return ERROR;
                    }
                    var target = HoldClickTarget.valueOf(getString(c, "targetType").toUpperCase());
                    var clickTarget = switch (target) {
                        case ANY -> ClickTarget.Any.INSTANCE;
                        case NONE -> ClickTarget.None.INSTANCE;
                        case BLOCK -> ClickTarget.AnyBlock.INSTANCE;
                        case ENTITY -> ClickTarget.AnyEntity.INSTANCE;
                    };
                    INPUTS.submit(InputRequest.builder()
                        .owner(this)
                        .input(Input.builder()
                            .leftClick(true)
                            .clickTarget(clickTarget)
                            .build())
                        .priority(100000)
                        .build());
                    c.getSource().getEmbed()
                        .title("Left Clicked")
                        .primaryColor();
                    return OK;
                })))
                .then(literal("hold").executes(c -> {
                        CONFIG.client.extra.click.holdLeftClick = true;
                        CONFIG.client.extra.click.holdRightClick = false;
                        c.getSource().getEmbed()
                            .title("Left Click Hold")
                            .primaryColor();
                        return OK;
                    })
                    .then(literal("interval").then(argument("interval", integer(0, 100)).executes(c -> {
                        CONFIG.client.extra.click.holdLeftClickInterval = getInteger(c, "interval");
                        c.getSource().getEmbed()
                            .title("Left Click Hold Interval Set")
                            .primaryColor();
                        return OK;
                    })))))
            .then(literal("right").executes(c -> {
                    if (!Proxy.getInstance().isConnected()) {
                        c.getSource().getEmbed()
                            .title("Not Connected")
                            .description("Must be connected to click");
                        return ERROR;
                    }
                    INPUTS.submit(InputRequest.builder()
                        .owner(this)
                        .input(Input.builder()
                            .rightClick(true)
                            .build())
                        .priority(100000)
                        .build());
                    c.getSource().getEmbed()
                        .title("Right Clicked")
                        .primaryColor();
                    return OK;
                })
                .then(literal("target").then(argument("targetType", enumStrings(HoldClickTarget.values())).executes(c -> {
                    if (!Proxy.getInstance().isConnected()) {
                        c.getSource().getEmbed()
                            .title("Not Connected")
                            .description("Must be connected to click");
                        return ERROR;
                    }
                    var target = HoldClickTarget.valueOf(getString(c, "targetType").toUpperCase());
                    var clickTarget = switch (target) {
                        case ANY -> ClickTarget.Any.INSTANCE;
                        case NONE -> ClickTarget.None.INSTANCE;
                        case BLOCK -> ClickTarget.AnyBlock.INSTANCE;
                        case ENTITY -> ClickTarget.AnyEntity.INSTANCE;
                    };
                    INPUTS.submit(InputRequest.builder()
                        .owner(this)
                        .input(Input.builder()
                            .rightClick(true)
                            .clickTarget(clickTarget)
                            .build())
                        .priority(100000)
                        .build());
                    c.getSource().getEmbed()
                        .title("Right Clicked")
                        .primaryColor();
                    return OK;
                })))
                .then(literal("hold")
                    .executes(c -> {
                        CONFIG.client.extra.click.holdLeftClick = false;
                        CONFIG.client.extra.click.holdRightClick = true;
                        CONFIG.client.extra.click.holdRightClickMode = HoldRightClickMode.MAIN_HAND;
                        c.getSource().getEmbed()
                            .title("Right Click Hold")
                            .primaryColor();
                        return OK;
                    })
                    .then(literal("mainHand").executes(c -> {
                        CONFIG.client.extra.click.holdLeftClick = false;
                        CONFIG.client.extra.click.holdRightClick = true;
                        CONFIG.client.extra.click.holdRightClickMode = HoldRightClickMode.MAIN_HAND;
                        c.getSource().getEmbed()
                            .title("Right Click Hold (Main Hand)")
                            .primaryColor();
                        return OK;
                    }))
                    .then(literal("offHand").executes(c -> {
                        CONFIG.client.extra.click.holdLeftClick = false;
                        CONFIG.client.extra.click.holdRightClick = true;
                        CONFIG.client.extra.click.holdRightClickMode = HoldRightClickMode.OFF_HAND;
                        c.getSource().getEmbed()
                            .title("Right Click Hold (Offhand)")
                            .primaryColor();
                        return OK;
                    }))
                    .then(literal("alternate").executes(c -> {
                        CONFIG.client.extra.click.holdLeftClick = false;
                        CONFIG.client.extra.click.holdRightClick = true;
                        CONFIG.client.extra.click.holdRightClickMode = HoldRightClickMode.ALTERNATE_HANDS;
                        c.getSource().getEmbed()
                            .title("Right Click Hold (Alternate)")
                            .primaryColor();
                        return OK;
                    }))
                    .then(literal("interval").then(argument("ticks", integer(0, 100)).executes(c -> {
                        CONFIG.client.extra.click.holdRightClickInterval = getInteger(c, "ticks");
                        c.getSource().getEmbed()
                            .title("Right Click Hold Interval Set")
                            .primaryColor();
                        return OK;
                    })))))
            .then(literal("addedBlockReach").then(argument("reach", floatArg(-10, 10)).executes(c -> {
                float f = getFloat(c, "reach");
                CONFIG.client.extra.click.additionalBlockReach = f;
                c.getSource().getEmbed()
                    .title("Additional Block Reach Set")
                    .primaryColor();
                return OK;
            })))
            .then(literal("addedEntityReach").then(argument("reach", floatArg(-10, 10)).executes(c -> {
                float f = getFloat(c, "reach");
                CONFIG.client.extra.click.additionalEntityReach = f;
                c.getSource().getEmbed()
                    .title("Additional Entity Reach Set")
                    .primaryColor();
                return OK;
            })))
            .then(literal("hold")
                .then(literal("forceRotation")
                    .then(argument("toggle", toggle()).executes(c -> {
                        CONFIG.client.extra.click.hasRotation = getToggle(c, "toggle");
                        c.getSource().getEmbed()
                            .title("Hold Force Rotation Set")
                            .primaryColor();
                        return OK;
                    }))
                    .then(argument("yaw", floatArg(-180, 180)).then(argument("pitch", floatArg(-90, 90)).executes(c -> {
                        CONFIG.client.extra.click.hasRotation = true;
                        CONFIG.client.extra.click.rotationYaw = getFloat(c, "yaw");
                        CONFIG.client.extra.click.rotationPitch = getFloat(c, "pitch");
                        c.getSource().getEmbed()
                            .title("Hold Force Rotation Set")
                            .primaryColor();
                        return OK;
                    }))))
                .then(literal("sneak").then(argument("toggle", toggle()).executes(c -> {
                    CONFIG.client.extra.click.holdSneak = getToggle(c, "toggle");
                    c.getSource().getEmbed()
                        .title("Hold Sneak Set")
                        .primaryColor();
                    return OK;
                })))
                .then(literal("target")
                    .then(argument("targetType", enumStrings(HoldClickTarget.values())).executes(c -> {
                        String targetString = getString(c, "targetType");
                        CONFIG.client.extra.click.holdClickTarget = HoldClickTarget.valueOf(targetString.toUpperCase());
                        c.getSource().getEmbed()
                            .title("Hold Target Set")
                            .primaryColor();
                        return OK;
                    }))));
    }

    @Override
    public void defaultEmbed(Embed embed) {
        embed
            .addField("Click Hold", CONFIG.client.extra.click.holdLeftClick ? "Left" : CONFIG.client.extra.click.holdRightClick ? "Right" : "off")
            .addField("Click Hold Force Rotation", toggleStr(CONFIG.client.extra.click.hasRotation) + (
                CONFIG.client.extra.click.hasRotation
                    ? " [" + String.format("%.2f", CONFIG.client.extra.click.rotationYaw) + ", " + String.format("%.2f", CONFIG.client.extra.click.rotationPitch) + "]"
                    : ""))
            .addField("Click Hold Sneak", toggleStr(CONFIG.client.extra.click.holdSneak))
            .addField("Click Hold Target", CONFIG.client.extra.click.holdClickTarget.toString().toLowerCase())
            .addField("Left Click Hold Interval", CONFIG.client.extra.click.holdLeftClickInterval + " ticks")
            .addField("Right Click Hold Mode", rightClickHoldModeToString(CONFIG.client.extra.click.holdRightClickMode))
            .addField("Right Click Hold Interval", CONFIG.client.extra.click.holdRightClickInterval + " ticks")
            .addField("Added Block Reach", CONFIG.client.extra.click.additionalBlockReach)
            .addField("Added Entity Reach", CONFIG.client.extra.click.additionalEntityReach)
            .primaryColor();
    }

    private String rightClickHoldModeToString(HoldRightClickMode mode) {
        return switch (mode) {
            case MAIN_HAND -> "mainHand";
            case OFF_HAND -> "offHand";
            case ALTERNATE_HANDS -> "alternate";
        };
    }
}
