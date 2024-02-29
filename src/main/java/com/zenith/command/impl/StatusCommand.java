package com.zenith.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.Proxy;
import com.zenith.cache.data.PlayerCache;
import com.zenith.command.Command;
import com.zenith.command.CommandCategory;
import com.zenith.command.CommandContext;
import com.zenith.command.CommandUsage;
import com.zenith.feature.queue.Queue;
import com.zenith.network.server.ServerConnection;
import discord4j.common.util.TimestampFormat;
import discord4j.rest.util.Color;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import static com.zenith.Shared.*;
import static java.util.Arrays.asList;
import static java.util.Objects.nonNull;

public class StatusCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.full(
            "status",
            CommandCategory.CORE,
            "Gets the current proxy status",
            asList(
                "",
                "modules"
                ),
            asList("s")
        );
    }

    public static String getCoordinates(final PlayerCache playerCache) {
        if (CONFIG.discord.reportCoords) {
            return "||["
                    + (int) playerCache.getX() + ", "
                    + (int) playerCache.getY() + ", "
                    + (int) playerCache.getZ()
                    + "]||";
        } else {
            return "Coords disabled";
        }
    }

    private String getCurrentClientUserName() {
        ServerConnection currentConnection = Proxy.getInstance().getCurrentPlayer().get();
        if (nonNull(currentConnection)) {
            return currentConnection.getProfileCache().getProfile().getName();
        } else {
            return "None";
        }
    }

    private List<String> getSpectatorUserNames() {
        return Proxy.getInstance().getSpectatorConnections().stream()
                .map(connection -> connection.getProfileCache().getProfile().getName())
                .collect(Collectors.toList());
    }

    private String getStatus() {
        if (Proxy.getInstance().isConnected()) {
            if (Proxy.getInstance().isInQueue()) {
                if (Proxy.getInstance().isPrio()) {
                    return "In Prio Queue [" + Proxy.getInstance().getQueuePosition() + " / " + Queue.getQueueStatus().prio() + "]\n"
                        + "ETA: " + Queue.getQueueEta(Proxy.getInstance().getQueuePosition()) + "\n"
                        + "(" + TimestampFormat.LONG_TIME.format(Instant.now().plus(Duration.ofSeconds(Queue.getQueueWait(Proxy.getInstance().getQueuePosition())))) +")";
                } else {
                    return "In Queue [" + Proxy.getInstance().getQueuePosition() + " / " + Queue.getQueueStatus().regular() + "]\n"
                        + "ETA: " + Queue.getQueueEta(Proxy.getInstance().getQueuePosition()) + "\n"
                        + "(" + TimestampFormat.LONG_TIME.format(Instant.now().plus(Duration.ofSeconds(Queue.getQueueWait(Proxy.getInstance().getQueuePosition())))) +")";
                }
            } else {
                return "Online";
            }
        } else {
            return "Disconnected";
        }
    }

    private String getQueueStatus() {
        return "Priority: " + Queue.getQueueStatus().prio() + " [" + Queue.getQueueEta(Queue.getQueueStatus().prio()) + "]"
                + "\nRegular: " + Queue.getQueueStatus().regular() + " [" + Queue.getQueueEta(Queue.getQueueStatus().regular()) + "]";
    }

    public String getOnlineTime() {
        if (Proxy.getInstance().isConnected()) {
            long secondsOnline = Instant.now().getEpochSecond() - Proxy.getInstance().getConnectTime().getEpochSecond();
            // hours:minutes:seconds
            return Queue.getEtaStringFromSeconds(secondsOnline);
        } else {
            return "Not Online!";
        }
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("status")
            .then(literal("modules").executes(c -> {
                c.getSource().getEmbed()
                    .title("ZenithProxy " + LAUNCH_CONFIG.version + " Modules Status: " + CONFIG.authentication.username)
                    .color(Proxy.getInstance().isConnected() ? (Proxy.getInstance().isInQueue() ? Color.MOON_YELLOW : Color.MEDIUM_SEA_GREEN) : Color.RUBY)
                    .addField("AutoDisconnect", "[Health: " + toggleStr(CONFIG.client.extra.utility.actions.autoDisconnect.enabled)
                        + " (" + CONFIG.client.extra.utility.actions.autoDisconnect.health + ")]", true)
                    .addField("AutoReconnect", toggleStr(CONFIG.client.extra.autoReconnect.enabled)
                        + " [" + CONFIG.client.extra.autoReconnect.delaySeconds + "]", true)
                    .addField("KillAura", toggleStr(CONFIG.client.extra.killAura.enabled), true)
                    .addField("AutoTotem", toggleStr(CONFIG.client.extra.autoTotem.enabled), true)
                    .addField("AutoEat", toggleStr(CONFIG.client.extra.autoEat.enabled), true)
                    .addField("AntiAFK", toggleStr(CONFIG.client.extra.antiafk.enabled), true)
                    .addField("AutoRespawn", toggleStr(CONFIG.client.extra.autoRespawn.enabled)
                        + " [" + CONFIG.client.extra.autoRespawn.delayMillis + "]", true)
                    .addField("ViaVersion", "Client: " + toggleStr(CONFIG.client.viaversion.enabled)
                        + "\nServer: " + toggleStr(CONFIG.server.viaversion.enabled), true)
                    .addField("VisualRange", toggleStr(CONFIG.client.extra.visualRangeAlert), true)
                    .addField("AntiLeak", toggleStr(CONFIG.client.extra.antiLeak.enabled), true)
                    .addField("AntiKick", toggleStr(CONFIG.client.extra.antiKick.enabled), true)
                    .addField("AutoFish", toggleStr(CONFIG.client.extra.autoFish.enabled), true)
                    .addField("Spook", toggleStr(CONFIG.client.extra.spook.enabled), true)
                    .addField("Stalk", toggleStr(CONFIG.client.extra.stalk.enabled), true)
                    .addField("Active Hours", toggleStr(CONFIG.client.extra.utility.actions.activeHours.enabled), true)
                    .addField("AutoReply", toggleStr(CONFIG.client.extra.autoReply.enabled), true)
                    .addField("ActionLimiter", toggleStr(CONFIG.client.extra.actionLimiter.enabled), true)
                    .addField("Spammer", toggleStr(CONFIG.client.extra.spammer.enabled), true);
            }))
            .executes(c -> {
                final var embed = c.getSource().getEmbed();
                embed
                    .title("ZenithProxy " + LAUNCH_CONFIG.version + " Status: " + CONFIG.authentication.username)
                    .color(Proxy.getInstance().isConnected() ? (Proxy.getInstance().isInQueue() ? Color.MOON_YELLOW : Color.MEDIUM_SEA_GREEN) : Color.RUBY)
                    .addField("Status", getStatus(), true)
                    .addField("Connected User", getCurrentClientUserName(), true)
                    .addField("Online Time", getOnlineTime(), true)
                    .addField("Proxy IP", CONFIG.server.getProxyAddress(), true)
                    .addField("Server", CONFIG.client.server.address + ':' + CONFIG.client.server.port, true)
                    .addField("Priority Queue", (CONFIG.authentication.prio ? "yes" : "no") + " [" + (CONFIG.authentication.prioBanned ? "banned" : "unbanned") + "]", true);
                if (Proxy.getInstance().isConnected())
                    embed.addField("TPS", TPS_CALCULATOR.getTPS(), true);
                embed.addField("Spectators", toggleStr(CONFIG.server.spectator.allowSpectator),true);
                if (!getSpectatorUserNames().isEmpty())
                    embed.addField("Online Spectators", String.join(", ", getSpectatorUserNames()), true);
                embed.addField("2b2t Queue", getQueueStatus(), true)
                    .addField("Dimension",
                              (nonNull(CACHE.getChunkCache().getCurrentDimension()) ? CACHE.getChunkCache().getCurrentDimension().dimensionName().replace("minecraft:", ""): "None"),
                              true);
                if (CONFIG.discord.reportCoords)
                    embed.addField("Coordinates", getCoordinates(CACHE.getPlayerCache()), true);
                embed.addField("Health",  (CACHE.getPlayerCache().getThePlayer().getHealth()), true)
                    .addField("Chat Relay", (!CONFIG.discord.chatRelay.channelId.isEmpty() ? toggleStr(CONFIG.discord.chatRelay.enable) : "Not Configured"), true)
                    .addField("AutoUpdate", toggleStr(CONFIG.autoUpdater.autoUpdate), false);
                 return 1;
            });
    }
}
