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
import discord4j.rest.util.Color;

import static com.zenith.Shared.CACHE;
import static com.zenith.Shared.CONFIG;
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
                        "packetLog on/off",
                        "sync inventory",
                        "sync chunks",
                        "clearEffects"
                        // todo: packet filter setting
                ));
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("debug").requires(Command::validateAccountOwner)
            .then(literal("autoConnect")
                      .then(argument("toggle", toggle()).executes(c -> {
                            CONFIG.client.autoConnect = getToggle(c, "toggle");
                            c.getSource().getEmbed()
                                .title("Auto Connect " + (CONFIG.client.autoConnect ? "On!" : "Off!"))
                                .color(Color.CYAN);
                            return 1;
                      })))
            .then(literal("packetLog")
                        .then(argument("toggle", toggle()).executes(c -> {
                            boolean toggle = getToggle(c, "toggle");
                            if (toggle) {
                                CONFIG.debug.packet.received = true;
                                CONFIG.debug.packet.receivedBody = true;
                                CONFIG.debug.packet.preSent = true;
                                CONFIG.debug.packet.postSent = true;
//                                CONFIG.debug.packet.postSentBody = true;
                            } else {
                                CONFIG.debug.packet.received = false;
                                CONFIG.debug.packet.postSent = false;
                                CONFIG.debug.packet.preSent = false;
                            }
                            c.getSource().getEmbed()
                                    .title("Packet Log " + (toggle ? "On!" : "Off!"))
                                    .color(Color.CYAN);
                                return 1;
                        })))
            .then(literal("sync")
                        .then(literal("inventory").executes(c -> {
                            PlayerCache.sync();
                            c.getSource().getEmbed()
                                .title("Inventory Synced")
                                .color(Color.CYAN);
                            return 1;
                        }))
                        .then(literal("chunks").executes(c -> {
                            ChunkCache.sync();
                            c.getSource().getEmbed()
                                .title("Synced Chunks")
                                .color(Color.CYAN);
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
                    .title("Cleared Effects")
                    .color(Color.CYAN);
                return 1;
            }));
    }
}
