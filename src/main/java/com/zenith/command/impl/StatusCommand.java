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
import discord4j.core.spec.EmbedCreateSpec;
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
        return CommandUsage.simpleAliases(
            "status",
            CommandCategory.CORE,
            "Gets the current proxy status",
            aliases()
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
        return Proxy.getInstance().getSpectatorConnections()
                .map(connection -> connection.getProfileCache().getProfile().getName())
                .collect(Collectors.toList());
    }

    private String getStatus() {
        if (Proxy.getInstance().isConnected()) {
            if (Proxy.getInstance().isInQueue()) {
                if (Proxy.getInstance().getIsPrio().isPresent()) {
                    if (Proxy.getInstance().getIsPrio().get()) {
                        return "In Prio Queue [" + Proxy.getInstance().getQueuePosition() + " / " + Queue.getQueueStatus().prio() + "]\n"
                            + "ETA: " + Queue.getQueueEta(Proxy.getInstance().getQueuePosition()) + "\n"
                            + "(" + TimestampFormat.LONG_TIME.format(Instant.now().plus(Duration.ofSeconds((long) Queue.getQueueWait(Proxy.getInstance().getQueuePosition())))) +")";
                    } else {
                        return "In Reg Queue [" + Proxy.getInstance().getQueuePosition() + " / " + Queue.getQueueStatus().regular() + "]\n"
                            + "ETA: " + Queue.getQueueEta(Proxy.getInstance().getQueuePosition()) + "\n"
                            + "(" + TimestampFormat.LONG_TIME.format(Instant.now().plus(Duration.ofSeconds((long) Queue.getQueueWait(Proxy.getInstance().getQueuePosition())))) +")";
                    }
                } else {
                    return "Queueing";
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
        return command("status").executes(c -> {
            final EmbedCreateSpec.Builder builder = c.getSource().getEmbedBuilder();
            builder
                .title("ZenithProxy " + LAUNCH_CONFIG.version + " Status: " + CONFIG.authentication.username)
                .color(Proxy.getInstance().isConnected() ? (Proxy.getInstance().isInQueue() ? Color.MOON_YELLOW : Color.MEDIUM_SEA_GREEN) : Color.RUBY)
                .addField("Status", getStatus(), true)
                .addField("Connected User", getCurrentClientUserName(), true)
                .addField("Online Time", getOnlineTime(), true)
                .addField("Proxy IP", CONFIG.server.getProxyAddress(), true)
                .addField("Server", CONFIG.client.server.address + ':' + CONFIG.client.server.port, true)
                .addField("Priority Queue", (CONFIG.authentication.prio ? "yes" : "no") + " [" + (CONFIG.authentication.prioBanned ? "banned" : "unbanned") + "]", true);
            if (Proxy.getInstance().isConnected()) {
                builder.addField("TPS", TPS_CALCULATOR.getTPS(), true);
            }
            if (!getSpectatorUserNames().isEmpty()) {
                builder.addField("Spectators", String.join(", ", getSpectatorUserNames()), true);
            }
            builder.addField("2b2t Queue", getQueueStatus(), true)
                .addField("Dimension",
                          (nonNull(CACHE.getChunkCache().getCurrentDimension()) ? CACHE.getChunkCache().getCurrentDimension().getDimensionName().replace("minecraft:", ""): "None"),
                          true);
            if (CONFIG.discord.reportCoords) {
                builder.addField("Coordinates", getCoordinates(CACHE.getPlayerCache()), true);
            }
            builder.addField("Health", "" + (CACHE.getPlayerCache().getThePlayer().getHealth()), true)
                .addField("AutoDisconnect",
                          "[Health: " + toggleStr(CONFIG.client.extra.utility.actions.autoDisconnect.enabled)
                              + " (" + CONFIG.client.extra.utility.actions.autoDisconnect.health + ")]"
                              + "\n[CancelAutoReconnect: " + toggleStr(CONFIG.client.extra.utility.actions.autoDisconnect.cancelAutoReconnect) + "]"
                              + "\n[AutoClientDisconnect: " + toggleStr(CONFIG.client.extra.utility.actions.autoDisconnect.autoClientDisconnect) + "]"
                              + "\n[Thunder: " + toggleStr(CONFIG.client.extra.utility.actions.autoDisconnect.thunder) + "]"
                    , true)
                .addField("AutoReconnect",
                          toggleStr(CONFIG.client.extra.autoReconnect.enabled)
                              + " [" + CONFIG.client.extra.autoReconnect.delaySeconds + "]", true)
                .addField("AutoRespawn",
                          toggleStr(CONFIG.client.extra.autoRespawn.enabled)
                              + " [" + CONFIG.client.extra.autoRespawn.delayMillis + "]", true)
                .addField("AntiAFK",
                          toggleStr(CONFIG.client.extra.antiafk.enabled), true)
                .addField("AutoEat", toggleStr(CONFIG.client.extra.autoEat.enabled), true)
                .addField("VisualRange Notifications", toggleStr(CONFIG.client.extra.visualRangeAlert)
                    + "\n[Mention: " + toggleStr(CONFIG.client.extra.visualRangeAlertMention) + "]", true)
                .addField("Client Connection Notifications", toggleStr(CONFIG.client.extra.clientConnectionMessages), true)
                .addField("Stalk", toggleStr(CONFIG.client.extra.stalk.enabled), true)
                .addField("Spectators", toggleStr(CONFIG.server.spectator.allowSpectator)
                    + "\n[Public Chat: " + toggleStr(CONFIG.server.spectator.spectatorPublicChatEnabled) + "]", true)
                .addField("Active Hours", toggleStr(CONFIG.client.extra.utility.actions.activeHours.enabled), true)
//                    .addField("Display Coordinates", (CONFIG.discord.reportCoords ? "on" : "off"), true)
                .addField("Chat Relay", (!CONFIG.discord.chatRelay.channelId.isEmpty() ? toggleStr(CONFIG.discord.chatRelay.enable) : "Not Configured")
                    + "\n[WhisperMention: " + toggleStr(CONFIG.discord.chatRelay.mentionRoleOnWhisper) + "]"
                    + "\n[NameMention: " + toggleStr(CONFIG.discord.chatRelay.mentionRoleOnNameMention) + "]", true)
                .addField("AutoReply", toggleStr(CONFIG.client.extra.autoReply.enabled)
                    + "\n[Cooldown: " + CONFIG.client.extra.autoReply.cooldownSeconds + "]", true)
                .addField("Spammer", toggleStr(CONFIG.client.extra.spammer.enabled)
                    + "\n[Whisper: " + toggleStr(CONFIG.client.extra.spammer.whisper) + "]", true)
                .addField("AutoUpdate", toggleStr(CONFIG.autoUpdater.autoUpdate), false);
        });
    }

    @Override
    public List<String> aliases() {
        return asList("s");
    }
}
