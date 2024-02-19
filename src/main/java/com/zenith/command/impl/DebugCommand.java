package com.zenith.command.impl;

import com.github.steveice10.mc.protocol.data.game.entity.Effect;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.ClientboundRemoveMobEffectPacket;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.Proxy;
import com.zenith.cache.data.PlayerCache;
import com.zenith.cache.data.chunk.ChunkCache;
import com.zenith.command.Command;
import com.zenith.command.CommandCategory;
import com.zenith.command.CommandContext;
import com.zenith.command.CommandUsage;
import com.zenith.discord.Embed;
import discord4j.rest.util.Color;

import static com.zenith.Shared.CACHE;
import static com.zenith.Shared.CONFIG;
import static com.zenith.command.CustomStringArgumentType.wordWithChars;
import static com.zenith.command.ToggleArgumentType.getToggle;
import static com.zenith.command.ToggleArgumentType.toggle;
import static java.util.Arrays.asList;

public class DebugCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.args(
            "debug",
            CommandCategory.MANAGE,
            "Debug settings for developers",
            asList(
                        "autoConnect on/off",
                        "sync inventory",
                        "sync chunks",
                        "clearEffects",
                        "packetLog on/off",
                        "packetLog client on/off", // todo: subcommands for configuring subsettings more explicitly
                        "packetLog server on/off",
                        "packetLog filter <string>",
                        "sendChunksBeforePlayerSpawn on/off"
                ));
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("debug").requires(Command::validateAccountOwner)
            .then(literal("autoConnect")
                      .then(argument("toggle", toggle()).executes(c -> {
                            CONFIG.client.autoConnect = getToggle(c, "toggle");
                            c.getSource().getEmbed()
                                .title("Auto Connect " + (CONFIG.client.autoConnect ? "On!" : "Off!"));
                            return 1;
                      })))
            .then(literal("packetLog")
                      .then(argument("toggle", toggle()).executes(c -> {
                          CONFIG.debug.packetLog.enabled = getToggle(c, "toggle");
                          c.getSource().getEmbed()
                              .title("Packet Log " + (CONFIG.debug.packetLog.enabled ? "On!" : "Off!"));
                          return 1;
                      }))
                      .then(literal("client")
                                .then(argument("toggle", toggle()).executes(c -> {
                                    var toggle = getToggle(c, "toggle");
                                    if (toggle) {
                                        CONFIG.debug.packetLog.clientPacketLog.received = true;
                                        CONFIG.debug.packetLog.clientPacketLog.receivedBody = true;
                                        CONFIG.debug.packetLog.clientPacketLog.preSent = true;
                                        CONFIG.debug.packetLog.clientPacketLog.postSent = true;
                                    } else {
                                        CONFIG.debug.packetLog.clientPacketLog.received = false;
                                        CONFIG.debug.packetLog.clientPacketLog.postSent = false;
                                        CONFIG.debug.packetLog.clientPacketLog.preSent = false;
                                    }
                                    c.getSource().getEmbed()
                                        .title("Client Packet Log " + (toggle ? "On!" : "Off!"));
                                    return 1;
                                })))
                      .then(literal("server")
                                .then(argument("toggle", toggle()).executes(c -> {
                                    var toggle = getToggle(c, "toggle");
                                    if (toggle) {
                                        CONFIG.debug.packetLog.serverPacketLog.received = true;
                                        CONFIG.debug.packetLog.serverPacketLog.receivedBody = true;
                                        CONFIG.debug.packetLog.serverPacketLog.preSent = true;
                                        CONFIG.debug.packetLog.serverPacketLog.postSent = true;
                                    } else {
                                        CONFIG.debug.packetLog.serverPacketLog.received = false;
                                        CONFIG.debug.packetLog.serverPacketLog.postSent = false;
                                        CONFIG.debug.packetLog.serverPacketLog.preSent = false;
                                    }
                                    c.getSource().getEmbed()
                                        .title("Server Packet Log " + (toggle ? "On!" : "Off!"));
                                    return 1;
                                })))
                      .then(literal("filter")
                                .then(argument("filter", wordWithChars()).executes(c -> {
                                    CONFIG.debug.packetLog.packetFilter = c.getArgument("filter", String.class);
                                    if ("off".equalsIgnoreCase(CONFIG.debug.packetLog.packetFilter))
                                        CONFIG.debug.packetLog.packetFilter = "";
                                    c.getSource().getEmbed()
                                        .title("Packet Log Filter Set: " + CONFIG.debug.packetLog.packetFilter);
                                    return 1;
                                }))))
            .then(literal("sync")
                        .then(literal("inventory").executes(c -> {
                            PlayerCache.sync();
                            c.getSource().getEmbed()
                                .title("Inventory Synced");
                            return 1;
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
                    asList(Effect.values()).forEach(effect -> {
                        session.sendAsync(new ClientboundRemoveMobEffectPacket(
                            CACHE.getPlayerCache().getEntityId(),
                            effect));
                    });
                }
                c.getSource().getEmbed()
                    .title("Cleared Effects");
                return 1;
            }))
            .then(literal("sendChunksBeforePlayerSpawn")
                      .then(argument("toggle", toggle()).executes(c -> {
                          CONFIG.debug.sendChunksBeforePlayerSpawn = getToggle(c, "toggle");
                          c.getSource().getEmbed()
                              .title("Send Chunks Before Player Spawn " + (CONFIG.debug.sendChunksBeforePlayerSpawn ? "On!" : "Off!"));
                          return 1;
                      })));
    }

    @Override
    public void postPopulate(final Embed builder) {
        builder
            .addField("Auto Connect", toggleStr(CONFIG.client.autoConnect), false)
            .addField("Packet Log", toggleStr(CONFIG.debug.packetLog.enabled), false)
            .addField("Client Packet Log", toggleStr(CONFIG.debug.packetLog.clientPacketLog.received), false)
            .addField("Server Packet Log", toggleStr(CONFIG.debug.packetLog.serverPacketLog.received), false)
            .addField("Packet Log Filter", CONFIG.debug.packetLog.packetFilter, false)
            .addField("Send Chunks Before Player Spawn", toggleStr(CONFIG.debug.sendChunksBeforePlayerSpawn), false)
            .color(Color.CYAN);
    }
}
