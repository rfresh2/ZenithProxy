package com.zenith.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.Proxy;
import com.zenith.cache.data.PlayerCache;
import com.zenith.cache.data.chunk.ChunkCache;
import com.zenith.command.Command;
import com.zenith.command.CommandUsage;
import com.zenith.command.brigadier.CommandCategory;
import com.zenith.command.brigadier.CommandContext;
import com.zenith.discord.Embed;
import org.geysermc.mcprotocollib.protocol.codec.MinecraftCodecHelper;
import org.geysermc.mcprotocollib.protocol.data.game.entity.Effect;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundRemoveMobEffectPacket;

import static com.zenith.Shared.*;
import static com.zenith.command.brigadier.CustomStringArgumentType.wordWithChars;
import static com.zenith.command.brigadier.ToggleArgumentType.getToggle;
import static com.zenith.command.brigadier.ToggleArgumentType.toggle;
import static java.util.Arrays.asList;

public class DebugCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.args(
            "debug",
            CommandCategory.MANAGE,
            """
            Debug settings for features in testing or for use in development.
            """,
            asList(
                "deprecationWarning on/off",
                "sync inventory",
                "sync chunks",
                "clearEffects",
                "packetLog on/off",
                "packetLog client on/off", // todo: subcommands for configuring subsettings more explicitly
                "packetLog server on/off",
                "packetLog filter <string>",
                "sendChunksBeforePlayerSpawn on/off",
                "binaryNbtComponentSerializer on/off",
                "kickDisconnect on/off",
                "dc",
                "teleportResync on/off",
                "ncpStrictInventory on/off",
                "clientTickFixedRate on/off"
            )
        );
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("debug").requires(Command::validateAccountOwner)
            .then(literal("packetLog")
                      .then(argument("toggle", toggle()).executes(c -> {
                          CONFIG.debug.packetLog.enabled = getToggle(c, "toggle");
                          c.getSource().getEmbed()
                              .title("Packet Log " + toggleStrCaps(CONFIG.debug.packetLog.enabled));
                          return OK;
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
                                    return OK;
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
                                    return OK;
                                })))
                      .then(literal("filter")
                                .then(argument("filter", wordWithChars()).executes(c -> {
                                    CONFIG.debug.packetLog.packetFilter = c.getArgument("filter", String.class);
                                    if ("off".equalsIgnoreCase(CONFIG.debug.packetLog.packetFilter))
                                        CONFIG.debug.packetLog.packetFilter = "";
                                    c.getSource().getEmbed()
                                        .title("Packet Log Filter Set: " + CONFIG.debug.packetLog.packetFilter);
                                    return OK;
                                }))))
            .then(literal("sync")
                        .then(literal("inventory").executes(c -> {
                            PlayerCache.sync();
                            c.getSource().getEmbed()
                                .title("Inventory Synced");
                            return OK;
                        }))
                        .then(literal("chunks").executes(c -> {
                            ChunkCache.sync();
                            c.getSource().getEmbed()
                                .title("Synced Chunks");
                            return 1;
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
                return 1;
            }))
            .then(literal("sendChunksBeforePlayerSpawn")
                      .then(argument("toggle", toggle()).executes(c -> {
                          CONFIG.debug.sendChunksBeforePlayerSpawn = getToggle(c, "toggle");
                          c.getSource().getEmbed()
                              .title("Send Chunks Before Player Spawn " + toggleStrCaps(CONFIG.debug.sendChunksBeforePlayerSpawn));
                          return 1;
                      })))
            .then(literal("binaryNbtComponentSerializer")
                      .then(argument("toggle", toggle()).executes(c -> {
                          CONFIG.debug.binaryNbtComponentSerializer = getToggle(c, "toggle");
                          MinecraftCodecHelper.useBinaryNbtComponentSerializer = CONFIG.debug.binaryNbtComponentSerializer;
                          c.getSource().getEmbed()
                              .title("Binary Nbt Component Serializer " + toggleStrCaps(CONFIG.debug.binaryNbtComponentSerializer));
                          return 1;
                      })))
            .then(literal("kickDisconnect").then(argument("toggle", toggle()).executes(c -> {
                CONFIG.debug.kickDisconnect = getToggle(c, "toggle");
                c.getSource().getEmbed()
                    .title("Kick Disconnect " + toggleStrCaps(CONFIG.debug.kickDisconnect));
                return 1;
            })))
            // insta disconnect
            .then(literal("dc").executes(c -> {
                c.getSource().setNoOutput(true);
                Proxy.getInstance().kickDisconnect(MANUAL_DISCONNECT, null);
            }))
            .then(literal("teleportResync").then(argument("toggle", toggle()).executes(c -> {
                CONFIG.debug.resyncTeleports = getToggle(c, "toggle");
                c.getSource().getEmbed()
                    .title("Teleport Resync " + toggleStrCaps(CONFIG.debug.resyncTeleports));
                return 1;
            })))
            .then(literal("ncpStrictInventory").then(argument("toggle", toggle()).executes(c -> {
                CONFIG.debug.ncpStrictInventory = getToggle(c, "toggle");
                c.getSource().getEmbed()
                    .title("NCP Strict Inventory " + toggleStrCaps(CONFIG.debug.ncpStrictInventory));
                return 1;
            })))
            .then(literal("clientTickFixedRate").then(argument("toggle", toggle()).executes(c -> {
                CONFIG.debug.clientTickFixedRate = getToggle(c, "toggle");
                c.getSource().getEmbed()
                    .title("Client Tick Fixed Rate " + toggleStrCaps(CONFIG.debug.clientTickFixedRate));
                return OK;
            })));
    }

    @Override
    public void postPopulate(final Embed builder) {
        builder
            .addField("Packet Log", toggleStr(CONFIG.debug.packetLog.enabled), false)
            .addField("Client Packet Log", toggleStr(CONFIG.debug.packetLog.clientPacketLog.received), false)
            .addField("Server Packet Log", toggleStr(CONFIG.debug.packetLog.serverPacketLog.received), false)
            .addField("Packet Log Filter", CONFIG.debug.packetLog.packetFilter, false)
            .addField("Send Chunks Before Player Spawn", toggleStr(CONFIG.debug.sendChunksBeforePlayerSpawn), false)
            .addField("Binary Nbt Component Serializer", toggleStr(CONFIG.debug.binaryNbtComponentSerializer), false)
            .addField("Kick Disconnect", toggleStr(CONFIG.debug.kickDisconnect), false)
            .addField("Teleport Resync", toggleStr(CONFIG.debug.resyncTeleports), false)
            .addField("NCP Strict Inventory", toggleStr(CONFIG.debug.ncpStrictInventory), false)
            .addField("Client Tick Fixed Rate", toggleStr(CONFIG.debug.clientTickFixedRate), false)
            .primaryColor();
    }
}
