package com.zenith.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.Proxy;
import com.zenith.cache.data.PlayerCache;
import com.zenith.command.Command;
import com.zenith.command.CommandContext;
import com.zenith.command.CommandUsage;
import com.zenith.feature.queue.Queue;
import com.zenith.network.server.ServerConnection;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;

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
        return Proxy.getInstance().getSpectatorConnections().stream()
                .map(connection -> connection.getProfileCache().getProfile().getName())
                .collect(Collectors.toList());
    }

    private String getStatus() {
        if (Proxy.getInstance().isConnected()) {
            if (Proxy.getInstance().isInQueue()) {
                if (Proxy.getInstance().getIsPrio().isPresent()) {
                    if (Proxy.getInstance().getIsPrio().get()) {
                        return "In Prio Queue [" + Proxy.getInstance().getQueuePosition() + " / " + Queue.getQueueStatus().prio() + "]\nETA: " + Queue.getQueueEta(Proxy.getInstance().getQueuePosition()) + "\n(<t:" + (Instant.now().getEpochSecond() + (long) Queue.getQueueWait(Proxy.getInstance().getQueuePosition())) +":T>)";
                    } else {
                        return "In Reg Queue [" + Proxy.getInstance().getQueuePosition() + " / " + Queue.getQueueStatus().regular() + "]\nETA: " + Queue.getQueueEta(Proxy.getInstance().getQueuePosition()) + "\n(<t:" + (Instant.now().getEpochSecond() + (long) Queue.getQueueWait(Proxy.getInstance().getQueuePosition())) +":T>)";
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

    private String dimensionIdToString(final int dimension) {
        if (dimension == 0) {
            return "Overworld";
        } else if (dimension == -1) {
            return "Nether";
        } else if (dimension == 1) {
            return "The End";
        } else {
            return "N/A";
        }
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
                    .color(Proxy.getInstance().isConnected() ? (Proxy.getInstance().isInQueue() ? Color.MOON_YELLOW : Color.CYAN) : Color.RUBY)
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
                            dimensionIdToString(CACHE.getPlayerCache().getDimension()),
                            true);
            if (CONFIG.discord.reportCoords) {
                builder.addField("Coordinates", getCoordinates(CACHE.getPlayerCache()), true);
            }
            builder.addField("Health", "" + ((int) CACHE.getPlayerCache().getThePlayer().getHealth()), true)
                    .addField("AutoDisconnect",
                            "[Health: " + (CONFIG.client.extra.utility.actions.autoDisconnect.enabled ? "on" : "off")
                                    + " (" + CONFIG.client.extra.utility.actions.autoDisconnect.health + ")]"
                                    + "\n[CancelAutoReconnect: " + (CONFIG.client.extra.utility.actions.autoDisconnect.cancelAutoReconnect ? "on" : "off") + "]"
                                    + "\n[AutoClientDisconnect: " + (CONFIG.client.extra.utility.actions.autoDisconnect.autoClientDisconnect ? "on" : "off") + "]"
                                    + "\n[Thunder: " + (CONFIG.client.extra.utility.actions.autoDisconnect.thunder ? "on" : "off") + "]"
                            , true)
                    .addField("AutoReconnect",
                            (CONFIG.client.extra.autoReconnect.enabled ? "on" : "off")
                                    + " [" + CONFIG.client.extra.autoReconnect.delaySeconds + "]", true)
                    .addField("AutoRespawn",
                            (CONFIG.client.extra.autoRespawn.enabled ? "on" : "off")
                                    + " [" + CONFIG.client.extra.autoRespawn.delayMillis + "]", true)
                    .addField("AntiAFK",
                            (CONFIG.client.extra.antiafk.enabled ? "on" : "off"), true)
                    .addField("AutoEat", (CONFIG.client.extra.autoEat.enabled ? "on" : "off"), true)
                    .addField("VisualRange Notifications", (CONFIG.client.extra.visualRangeAlert ? "on" : "off")
                            + "\n[Mention: " + (CONFIG.client.extra.visualRangeAlertMention ? "on" : "off") + "]", true)
                    .addField("Client Connection Notifications", (CONFIG.client.extra.clientConnectionMessages ? "on" : "off"), true)
                    .addField("Stalk", (CONFIG.client.extra.stalk.enabled ? "on" : "off"), true)
                    .addField("Spectators", (CONFIG.server.spectator.allowSpectator ? "on" : "off")
                            + "\n[Public Chat: " + (CONFIG.server.spectator.spectatorPublicChatEnabled ? "on" : "off") + "]", true)
                    .addField("Active Hours", (CONFIG.client.extra.utility.actions.activeHours.enabled ? "on" : "off"), true)
                    .addField("Display Coordinates", (CONFIG.discord.reportCoords ? "on" : "off"), true)
                    .addField("Chat Relay", (CONFIG.discord.chatRelay.channelId.length() > 0 ? (CONFIG.discord.chatRelay.enable ? "on" : "off") : "Not Configured")
                            + "\n[WhisperMention: " + (CONFIG.discord.chatRelay.mentionRoleOnWhisper ? "on" : "off") + "]"
                            + "\n[NameMention: " + (CONFIG.discord.chatRelay.mentionRoleOnNameMention ? "on" : "off") + "]", true)
                    .addField("AutoReply", (CONFIG.client.extra.autoReply.enabled ? "on" : "off")
                            + "\n[Cooldown: " + CONFIG.client.extra.autoReply.cooldownSeconds + "]", true)
                    .addField("AutoUpdate", (CONFIG.autoUpdater.autoUpdate ? "on" : "off"), false);
        });
    }

    @Override
    public List<String> aliases() {
        return asList("s");
    }
}
