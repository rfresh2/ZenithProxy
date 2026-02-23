package com.zenith.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.Proxy;
import com.zenith.cache.data.inventory.Container;
import com.zenith.command.api.Command;
import com.zenith.command.api.CommandCategory;
import com.zenith.command.api.CommandContext;
import com.zenith.command.api.CommandUsage;
import com.zenith.discord.Embed;
import com.zenith.event.client.ClientBotTick;
import com.zenith.feature.inventory.InventoryActionRequest;
import com.zenith.feature.player.ClickTarget;
import com.zenith.feature.player.Input;
import com.zenith.feature.player.InputRequest;
import com.zenith.mc.item.ItemRegistry;
import com.zenith.util.config.Config.Client.Extra.Click.HoldClickTarget;
import com.zenith.util.config.Config.Client.Extra.Click.HoldRightClickMode;
import org.geysermc.mcprotocollib.protocol.data.game.entity.EquipmentSlot;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.Hand;
import org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentTypes;
import org.jspecify.annotations.NonNull;

import java.util.concurrent.atomic.AtomicInteger;

import static com.github.rfresh2.EventConsumer.of;
import static com.mojang.brigadier.arguments.FloatArgumentType.floatArg;
import static com.mojang.brigadier.arguments.FloatArgumentType.getFloat;
import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.zenith.Globals.*;
import static com.zenith.command.brigadier.CustomStringArgumentType.getString;
import static com.zenith.command.brigadier.TimeArgument.time;
import static com.zenith.command.brigadier.ToggleArgumentType.getToggle;
import static com.zenith.command.brigadier.ToggleArgumentType.toggle;

public class ClickCommand extends Command {
    // manual actions triggered by this command - uses very high priority
    public static final int PRIORITY = 100000;

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
                "right useItem",
                "right useItem <mainHand/offHand>",
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
                        .priority(PRIORITY)
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
                        .priority(PRIORITY)
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
                        .priority(PRIORITY)
                        .build());
                    c.getSource().getEmbed()
                        .title("Right Clicked")
                        .primaryColor();
                    return OK;
                })
                .then(literal("useItem").executes(c -> {
                    if (!Proxy.getInstance().isConnected()) {
                        c.getSource().getEmbed()
                            .title("Not Connected")
                            .description("Must be connected to click");
                        return ERROR;
                    }
                    var itemInMainhand = CACHE.getPlayerCache().getEquipment(EquipmentSlot.MAIN_HAND);
                    var itemInOffhand = CACHE.getPlayerCache().getEquipment(EquipmentSlot.OFF_HAND);
                    if (itemInMainhand == Container.EMPTY_STACK && itemInOffhand == Container.EMPTY_STACK) {
                        c.getSource().getEmbed()
                            .title("No Item")
                            .description("Must have an item in mainHand or offHand");
                        return ERROR;
                    }
                    var itemToUse = itemInMainhand != Container.EMPTY_STACK ? itemInMainhand : itemInOffhand;
                    var hand = itemToUse == itemInMainhand ? Hand.MAIN_HAND : Hand.OFF_HAND;
                    useItem(hand, itemToUse);
                    c.getSource().getEmbed()
                        .title("Using Item")
                        .addField("Hand", hand.toString().toLowerCase())
                        .addField("Item", ItemRegistry.REGISTRY.get(itemToUse.getId()).name())
                        .primaryColor();
                    return OK;
                })
                    .then(literal("mainHand").executes(c -> {
                        if (!Proxy.getInstance().isConnected()) {
                            c.getSource().getEmbed()
                                .title("Not Connected")
                                .description("Must be connected to click");
                            return ERROR;
                        }
                        var itemInMainhand = CACHE.getPlayerCache().getEquipment(EquipmentSlot.MAIN_HAND);
                        if (itemInMainhand == Container.EMPTY_STACK) {
                            c.getSource().getEmbed()
                                .title("No Item")
                                .description("Must have an item in mainHand");
                            return ERROR;
                        }
                        useItem(Hand.MAIN_HAND, itemInMainhand);
                        c.getSource().getEmbed()
                            .title("Using Item")
                            .addField("Hand", Hand.MAIN_HAND.toString().toLowerCase())
                            .addField("Item", ItemRegistry.REGISTRY.get(itemInMainhand.getId()).name())
                            .primaryColor();
                        return OK;
                    }))
                    .then(literal("offHand").executes(c -> {
                        if (!Proxy.getInstance().isConnected()) {
                            c.getSource().getEmbed()
                                .title("Not Connected")
                                .description("Must be connected to click");
                            return ERROR;
                        }
                        var itemInOffHand = CACHE.getPlayerCache().getEquipment(EquipmentSlot.OFF_HAND);
                        if (itemInOffHand == Container.EMPTY_STACK) {
                            c.getSource().getEmbed()
                                .title("No Item")
                                .description("Must have an item in offHand");
                            return ERROR;
                        }
                        useItem(Hand.OFF_HAND, itemInOffHand);
                        c.getSource().getEmbed()
                            .title("Using Item")
                            .addField("Hand", Hand.MAIN_HAND.toString().toLowerCase())
                            .addField("Item", ItemRegistry.REGISTRY.get(itemInOffHand.getId()).name())
                            .primaryColor();
                        return OK;
                    })))
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
                        .priority(PRIORITY)
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
                    .then(literal("interval").then(argument("ticks", time(0, 100)).executes(c -> {
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

    private void useItem(Hand hand, @NonNull ItemStack item) {
        var itemData = ItemRegistry.REGISTRY.get(item.getId());
        var components = item.withAddedComponents(itemData.components()).getDataComponents();
        var isConsumable = components.contains(DataComponentTypes.CONSUMABLE);
        int ticks = isConsumable ? 50 : 0;
        var startUse = InputRequest.builder()
            .owner(this)
            .input(Input.builder()
                .clickTarget(ClickTarget.None.INSTANCE)
                .rightClick(true)
                .hand(hand)
                .build())
            .priority(PRIORITY)
            .build();
        var req = INPUTS.submit(startUse);
        if (ticks > 0) {
            req.addInputExecutedListener(f -> {
                if (f.isAccepted()) {
                    noActionForNextNTicks(ticks);
                }
            });
        }
    }

    private void noActionForNextNTicks(final int ticks) {
        var tickSubOwner = new Object();
        var counter = new AtomicInteger(0);
        EVENT_BUS.subscribe(tickSubOwner, of(ClientBotTick.class, event -> {
            INPUTS.submit(InputRequest.noInput(this, PRIORITY));
            INVENTORY.submit(InventoryActionRequest.noAction(this, PRIORITY));
            if (counter.incrementAndGet() >= ticks) {
                EVENT_BUS.unsubscribe(tickSubOwner);
            }
        }), of(ClientBotTick.Stopped.class, event -> EVENT_BUS.unsubscribe(tickSubOwner)));
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
