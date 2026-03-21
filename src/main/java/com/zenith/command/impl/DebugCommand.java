package com.zenith.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.Proxy;
import com.zenith.cache.data.PlayerCache;
import com.zenith.cache.data.chunk.ChunkCache;
import com.zenith.command.api.Command;
import com.zenith.command.api.CommandCategory;
import com.zenith.command.api.CommandContext;
import com.zenith.command.api.CommandUsage;
import com.zenith.feature.api.mclogs.MclogsApi;
import com.zenith.feature.gui.GuiBuilder;
import com.zenith.feature.gui.SlotBuilder;
import com.zenith.mc.item.ItemRegistry;
import com.zenith.util.timer.Timer;
import com.zenith.util.timer.Timers;
import net.kyori.adventure.text.Component;
import org.geysermc.mcprotocollib.protocol.codec.MinecraftTypes;
import org.geysermc.mcprotocollib.protocol.data.game.entity.Effect;
import org.geysermc.mcprotocollib.protocol.data.game.inventory.ContainerType;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentTypes;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundRemoveMobEffectPacket;

import java.nio.file.Path;

import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.zenith.Globals.*;
import static com.zenith.command.brigadier.CustomStringArgumentType.wordWithChars;
import static com.zenith.command.brigadier.ToggleArgumentType.getToggle;
import static com.zenith.command.brigadier.ToggleArgumentType.toggle;
import static com.zenith.util.DisconnectMessages.MANUAL_DISCONNECT;
import static java.util.Arrays.asList;

public class DebugCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.builder()
            .name("debug")
            .category(CommandCategory.MANAGE)
            .description("""
            Debug settings for features in testing or for use in development.
            """)
            .usageLines(
                "sync inventory",
                "sync chunks",
                "clearEffects",
                "packetLog on/off",
                "packetLog client on/off", // todo: subcommands for configuring subsettings more explicitly
                "packetLog server on/off",
                "packetLog filter <string>",
                "kickDisconnect on/off",
                "dc",
                "debugLogs on/off",
                "chunkCacheFullbright on/off",
                "defaultClientRenderDistance <int>",
                "lockFile on/off",
                "uploadLog",
                "uploadDebugLog",
                "uploadLauncherLog",
                "passthroughResourcePacks on/off"
            )
            .build();
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("debug").requires(Command::validateAccountOwner)
            .then(literal("packetLog")
                .then(argument("toggle", toggle()).executes(c -> {
                    CONFIG.debug.packetLog.enabled = getToggle(c, "toggle");
                    c.getSource().getEmbed()
                        .title("Packet Log " + toggleStrCaps(CONFIG.debug.packetLog.enabled));
                }))
                .then(literal("client")
                    .then(argument("toggle", toggle()).executes(c -> {
                        var toggle = getToggle(c, "toggle");
                        if (toggle) {
                            CONFIG.debug.packetLog.clientPacketLog.received = true;
                            CONFIG.debug.packetLog.clientPacketLog.receivedBody = true;
                            CONFIG.debug.packetLog.clientPacketLog.postSent = true;
                            CONFIG.debug.packetLog.clientPacketLog.postSentBody = true;
                        } else {
                            CONFIG.debug.packetLog.clientPacketLog.received = false;
                            CONFIG.debug.packetLog.clientPacketLog.postSent = false;
                            CONFIG.debug.packetLog.clientPacketLog.preSent = false;
                        }
                        c.getSource().getEmbed()
                            .title("Client Packet Log " + toggleStrCaps(toggle));
                    })))
                .then(literal("server")
                    .then(argument("toggle", toggle()).executes(c -> {
                        var toggle = getToggle(c, "toggle");
                        if (toggle) {
                            CONFIG.debug.packetLog.serverPacketLog.received = true;
                            CONFIG.debug.packetLog.serverPacketLog.receivedBody = true;
                            CONFIG.debug.packetLog.serverPacketLog.postSent = true;
                            CONFIG.debug.packetLog.serverPacketLog.postSentBody = true;
                        } else {
                            CONFIG.debug.packetLog.serverPacketLog.received = false;
                            CONFIG.debug.packetLog.serverPacketLog.postSent = false;
                            CONFIG.debug.packetLog.serverPacketLog.preSent = false;
                        }
                        c.getSource().getEmbed()
                            .title("Server Packet Log " + toggleStrCaps(toggle));
                    })))
                .then(literal("filter")
                    .then(argument("filter", wordWithChars()).executes(c -> {
                        CONFIG.debug.packetLog.packetFilter = c.getArgument("filter", String.class);
                        if ("off".equalsIgnoreCase(CONFIG.debug.packetLog.packetFilter))
                            CONFIG.debug.packetLog.packetFilter = "";
                        c.getSource().getEmbed()
                            .title("Packet Log Filter Set: " + CONFIG.debug.packetLog.packetFilter);
                    })))
                .then(literal("logLevelDebug").then(argument("toggle", toggle()).executes(c -> {
                    CONFIG.debug.packetLog.logLevelDebug = getToggle(c, "toggle");
                    c.getSource().getEmbed()
                        .title("Log Level Debug " + toggleStrCaps(CONFIG.debug.packetLog.logLevelDebug));
                }))))
            .then(literal("sync")
                .then(literal("inventory").executes(c -> {
                    PlayerCache.inventorySync();
                    c.getSource().getEmbed()
                        .title("Inventory Synced");
                    c.getSource().getData().put("noDefaultEmbed", true);
                }))
                .then(literal("chunks").executes(c -> {
                    ChunkCache.sync();
                    c.getSource().getEmbed()
                        .title("Synced Chunks");
                    c.getSource().getData().put("noDefaultEmbed", true);
                })))
            .then(literal("clearEffects").executes(c -> {
                CACHE.getPlayerCache().getThePlayer().getPotionEffectMap().clear();
                var session = Proxy.getInstance().getCurrentPlayer().get();
                if (session != null) {
                    asList(Effect.values()).forEach(effect -> session.sendAsync(new ClientboundRemoveMobEffectPacket(
                        CACHE.getPlayerCache().getEntityId(),
                        effect)));
                }
                c.getSource().getEmbed()
                    .title("Cleared Effects");
                c.getSource().getData().put("noDefaultEmbed", true);
            }))
            .then(literal("kickDisconnect").then(argument("toggle", toggle()).executes(c -> {
                CONFIG.debug.kickDisconnect = getToggle(c, "toggle");
                c.getSource().getEmbed()
                    .title("Kick Disconnect " + toggleStrCaps(CONFIG.debug.kickDisconnect));
            })))
            // insta disconnect
            .then(literal("dc").executes(c -> {
                c.getSource().setNoOutput(true);
                Proxy.getInstance().kickDisconnect(MANUAL_DISCONNECT, null);
            }))
            .then(literal("debugLogs").then(argument("toggle", toggle()).executes(c -> {
                CONFIG.debug.debugLogs = getToggle(c, "toggle");
                c.getSource().getEmbed()
                    .title("Debug Logs " + toggleStrCaps(CONFIG.debug.debugLogs));
            })))
            .then(literal("terminalDebugLogs").then(argument("toggle", toggle()).executes(c -> {
                CONFIG.debug.terminalDebugLogs = getToggle(c, "toggle");
                c.getSource().getEmbed()
                    .title("Terminal Debug Logs " + toggleStrCaps(CONFIG.debug.terminalDebugLogs));
            })))
            .then(literal("chunkCacheFullbright").then(argument("toggle", toggle()).executes(c -> {
                CONFIG.debug.server.cache.fullbrightChunkBlocklight = getToggle(c, "toggle");
                c.getSource().getEmbed()
                    .title("Chunk Cache Fullbright " + toggleStrCaps(CONFIG.debug.server.cache.fullbrightChunkBlocklight));
            })))
            .then(literal("maxCachedMaps").then(argument("count", integer(0)).executes(c -> {
                CONFIG.debug.server.cache.maxCachedMaps = getInteger(c, "count");
                c.getSource().getEmbed()
                    .title("Max Cached Maps Set");
            })))
            .then(literal("binaryNbtComponentSerializer").then(argument("toggle", toggle()).executes(c -> {
                MinecraftTypes.useBinaryNbtComponentSerializer = getToggle(c, "toggle");
                c.getSource().getEmbed()
                    .title("Binary NBT Component Serializer " + toggleStrCaps(MinecraftTypes.useBinaryNbtComponentSerializer));
            })))
            .then(literal("defaultClientRenderDistance").then(argument("dist", integer(1, 256)).executes(c -> {
                CONFIG.client.defaultClientRenderDistance = getInteger(c, "dist");
                c.getSource().getEmbed()
                    .title("Default Client Render Distance Set");
            })))
            .then(literal("inventorySyncOnLogin").then(argument("toggle", toggle()).executes(c -> {
                CONFIG.debug.inventorySyncOnLogin = getToggle(c, "toggle");
                c.getSource().getEmbed()
                    .title("Inventory Sync On Login " + toggleStrCaps(CONFIG.debug.inventorySyncOnLogin));
            })))
            .then(literal("gui").executes(c -> {
                var activePlayer = Proxy.getInstance().getActivePlayer();
                if (activePlayer == null) {
                    c.getSource().getEmbed()
                        .title("No player connected")
                        .errorColor();
                    return;
                }
                final Timer blockTimer = Timers.tickTimer();
                GUI.open(
                    GuiBuilder.create()
                        .session(activePlayer)
                        .addPage(GuiBuilder.createPage()
                            .id("page-1")
                            .title(Component.text("test-page-1"))
                            .containerType(ContainerType.GENERIC_9X3)
                            .slot(8, SlotBuilder.create()
                                .item(ItemRegistry.NETHERITE_BLOCK)
                                .amount(64)
                                .name(Component.text("a nice block"))
                                .tickHandler((slot, gui, page, index) -> {
                                    if (!blockTimer.tick(10)) return;
                                    var amount = slot.item().getAmount();
                                    amount++;
                                    if (amount >= 64) {
                                        amount = 1;
                                    }
                                    slot.item().setAmount(amount);
                                    page.setStale();
                                })
                                .build())
                            .slotsRange(11, 20, SlotBuilder.create()
                                .item(ItemRegistry.DIAMOND_SWORD)
                                .dataComponent(DataComponentTypes.DAMAGE, 12)
                                .name(Component.text("a nice sword, but a bit broken"))
                                .buttonClickHandler((button, g, page, event) -> {
                                    if (event.isLeftOrRightClick()) {
                                        g.session().sendAsyncAlert("Button clicked on page 1 at index " + event.slot());
                                    }
                                })
                                .build())
                            .slot(21, SlotBuilder.create()
                                .item(ItemRegistry.GOLDEN_AXE)
                                .dataComponent(DataComponentTypes.DAMAGE, 0)
                                .name(Component.text("magic axe"))
                                .tickHandler((slot, gui, page, index) -> {
                                    int damage = slot.item().getDataComponents().get(DataComponentTypes.DAMAGE);
                                    damage++;
                                    if (damage > 32) {
                                        damage = 0;
                                    }
                                    slot.item().getDataComponents().put(DataComponentTypes.DAMAGE, damage);
                                    page.setStale();
                                })
                                .build())
                            .slot(22, SlotBuilder.create()
                                .playerHead("rfresh2", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMTg3YmFhNDc2NzIzNGMwMWMwNGI4YmJlYjUxOGEwNTNkY2U3MzlmNGEwNDM1OGE0MjQzMDJmYjRhMDE3MmY4In19fQ==")
                                .build())
                            .build())
                        .build());
                c.getSource().getData().put("noDefaultEmbed", true);
            }))
            .then(literal("lockFile").then(argument("toggle", toggle()).executes(c -> {
                CONFIG.debug.lockFile = getToggle(c, "toggle");
                c.getSource().getEmbed()
                    .title("Lock File " + toggleStrCaps(CONFIG.debug.lockFile));
            })))
            .then(literal("uploadLog").executes(c -> {
                uploadLog(c.getSource(), "log/latest.log");
            }))
            .then(literal("uploadDebugLog").executes(c -> {
                uploadLog(c.getSource(), "log/debug.log");
            }))
            .then(literal("uploadLauncherLog").executes(c -> {
                uploadLog(c.getSource(), "log/launcher.log");
            }))
            .then(literal("passthroughResourcePacks").then(argument("toggle", toggle()).executes(c -> {;
                CONFIG.debug.passthroughResourcePacks = getToggle(c, "toggle");
                c.getSource().getEmbed()
                    .title("Passthrough Resource Packs " + toggleStrCaps(CONFIG.debug.passthroughResourcePacks));
            })))
            .then(literal("inputManagerDebugLogs").then(argument("toggle", toggle()).executes(c -> {
                CONFIG.debug.inputManagerDebugLogs = getToggle(c, "toggle");
                c.getSource().getEmbed()
                    .title("Input Manager Debug Logs " + toggleStrCaps(CONFIG.debug.inputManagerDebugLogs));
            })))
            .then(literal("botPitchPrecisionClamping").then(argument("toggle", toggle()).executes(c -> {
                CONFIG.debug.botPitchPrecisionClamping = getToggle(c, "toggle");
                c.getSource().getEmbed()
                    .title("Bot Pitch Precision Clamping " + toggleStrCaps(CONFIG.debug.botPitchPrecisionClamping));
            })))
            .then(literal("botRotateBeforeInteract").then(argument("toggle", toggle()).executes(c -> {
                CONFIG.debug.botRotateBeforeInteract = getToggle(c, "toggle");
                c.getSource().getEmbed()
                    .title("Bot Rotate Before Interact " + toggleStrCaps(CONFIG.debug.botRotateBeforeInteract));
            })))
            .then(literal("inventoryRequestServerSyncOnAction").then(argument("toggle", toggle()).executes(c -> {
                CONFIG.debug.inventoryRequestServerSyncOnAction = getToggle(c, "toggle");
                c.getSource().getEmbed()
                    .title("Inventory Request Server Sync On Action " + toggleStrCaps(CONFIG.debug.inventoryRequestServerSyncOnAction));
                c.getSource().getData().put("noDefaultEmbed", true);
            })))
            .then(literal("chainBreakSpeed2b2tFix").then(argument("toggle", toggle()).executes(c -> {
                CONFIG.debug.chainBreakSpeed2b2tFix = getToggle(c, "toggle");
                c.getSource().getEmbed()
                    .title("Chain Break Speed 2b2t Fix " + toggleStrCaps(CONFIG.debug.chainBreakSpeed2b2tFix));
            })))
            .then(literal("entityPushing").then(argument("toggle", toggle()).executes(c -> {
                CONFIG.debug.entityPushing = getToggle(c, "toggle");
                c.getSource().getEmbed()
                    .title("Entity Pushing " + toggleStrCaps(CONFIG.debug.entityPushing));
            })));
    }

    private static void uploadLog(CommandContext c, String path) {
        MclogsApi.INSTANCE.uploadLog(Path.of(path))
            .ifPresentOrElse(response -> {
                if (response.success()) {
                    c.getEmbed()
                        .title("Log Uploaded")
                        .description("**Link**: " + response.url())
                        .addField("Warning", "May contain sensitive information like coords, be careful who you share the link with");
                } else {
                    c.getEmbed()
                        .title("Error Uploading Log")
                        .description(response.error())
                        .errorColor();
                }
            }, () -> {
                c.getEmbed()
                    .title("Log Upload Failed")
                    .errorColor();
            });
        c.getData().put("noDefaultEmbed", true);
    }

    @Override
    public void defaultHandler(final CommandContext ctx) {
        if (!ctx.getData().containsKey("noDefaultEmbed")) {
            ctx.getEmbed()
                .addField("Packet Log", toggleStr(CONFIG.debug.packetLog.enabled))
                .addField("Client Packet Log", toggleStr(CONFIG.debug.packetLog.clientPacketLog.received))
                .addField("Server Packet Log", toggleStr(CONFIG.debug.packetLog.serverPacketLog.received))
                .addField("Packet Log Filter", CONFIG.debug.packetLog.packetFilter)
                .addField("Kick Disconnect", toggleStr(CONFIG.debug.kickDisconnect))
                .addField("Debug Logs", toggleStr(CONFIG.debug.debugLogs))
                .addField("Terminal Debug Logs", toggleStr(CONFIG.debug.terminalDebugLogs))
                .addField("Chunk Cache Fullbright", toggleStr(CONFIG.debug.server.cache.fullbrightChunkBlocklight))
                .addField("Max Cached Maps", CONFIG.debug.server.cache.maxCachedMaps)
                .addField("Default Client Render Distance", CONFIG.client.defaultClientRenderDistance)
                .addField("Lock File", toggleStr(CONFIG.debug.lockFile))
                .addField("Passthrough Resource Packs", toggleStr(CONFIG.debug.passthroughResourcePacks));
        }
        ctx.getEmbed()
            .primaryColor();
    }
}
