package com.zenith.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.Proxy;
import com.zenith.cache.data.PlayerCache;
import com.zenith.command.Command;
import com.zenith.command.CommandUsage;
import com.zenith.command.brigadier.CommandCategory;
import com.zenith.command.brigadier.CommandContext;
import com.zenith.feature.queue.Queue;
import com.zenith.module.impl.*;
import com.zenith.network.server.ServerSession;
import discord4j.common.util.TimestampFormat;

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
            """
            Prints the current status of ZenithProxy, the in-game player, and modules.
            """,
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
        ServerSession currentConnection = Proxy.getInstance().getCurrentPlayer().get();
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
        return Proxy.getInstance().isConnected()
            ? Proxy.getInstance().getOnlineTimeString()
            : "Not Online!";
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("status")
            .then(literal("modules").executes(c -> {
                c.getSource().getEmbed()
                    .title("ZenithProxy " + LAUNCH_CONFIG.version + " - " + CONFIG.authentication.username)
                    .color(Proxy.getInstance().isConnected()
                               ? (Proxy.getInstance().isInQueue()
                                    ? CONFIG.theme.inQueue.discord()
                                    : CONFIG.theme.success.discord())
                               : CONFIG.theme.error.discord())
                    .thumbnail(Proxy.getInstance().getAvatarURL(CONFIG.authentication.username).toString())
                    .addField("AutoDisconnect", toggleStr(MODULE.get(AutoDisconnect.class).isEnabled()), true)
                    .addField("AutoReconnect", toggleStr(MODULE.get(AutoReconnect.class).isEnabled()), true)
                    .addField("KillAura", toggleStr(MODULE.get(KillAura.class).isEnabled()), true)
                    .addField("AutoTotem", toggleStr(MODULE.get(AutoTotem.class).isEnabled()), true)
                    .addField("AutoEat", toggleStr(MODULE.get(AutoEat.class).isEnabled()), true)
                    .addField("AntiAFK", toggleStr(MODULE.get(AntiAFK.class).isEnabled()), true)
                    .addField("AutoRespawn", toggleStr(MODULE.get(AutoRespawn.class).isEnabled()), true)
                    .addField("ViaVersion", "Z->S: " + toggleStr(CONFIG.client.viaversion.enabled)
                        + "\nP->Z: " + toggleStr(CONFIG.server.viaversion.enabled), true)
                    .addField("VisualRange", toggleStr(MODULE.get(VisualRange.class).isEnabled()), true)
                    .addField("AntiLeak", toggleStr(MODULE.get(AntiLeak.class).isEnabled()), true)
                    .addField("AntiKick", toggleStr(MODULE.get(AntiKick.class).isEnabled()), true)
                    .addField("AutoFish", toggleStr(MODULE.get(AutoFish.class).isEnabled()), true)
                    .addField("Spook", toggleStr(MODULE.get(Spook.class).isEnabled()), true)
                    .addField("Active Hours", toggleStr(MODULE.get(ActiveHours.class).isEnabled()), true)
                    .addField("AutoReply", toggleStr(MODULE.get(AutoReply.class).isEnabled()), true)
                    .addField("ActionLimiter", toggleStr(MODULE.get(ActionLimiter.class).isEnabled()), true)
                    .addField("Spammer", toggleStr(MODULE.get(Spammer.class).isEnabled()), true)
                    .addField("Replay Recording", toggleStr(MODULE.get(ReplayMod.class).isEnabled()), true)
                    .addField("ESP", toggleStr(MODULE.get(ESP.class).isEnabled()), true)
                    .addField("AutoArmor", toggleStr(MODULE.get(AutoArmor.class).isEnabled()), true)
                    .addField("ChatHistory", toggleStr(MODULE.get(ChatHistory.class).isEnabled()), true);
            }))
            .executes(c -> {
                final var embed = c.getSource().getEmbed();
                embed
                    .title("ZenithProxy " + LAUNCH_CONFIG.version + " - " + CONFIG.authentication.username)
                    .color(Proxy.getInstance().isConnected()
                               ? (Proxy.getInstance().isInQueue()
                        ? CONFIG.theme.inQueue.discord()
                        : CONFIG.theme.success.discord())
                               : CONFIG.theme.error.discord())
                    .thumbnail(Proxy.getInstance().getAvatarURL(CONFIG.authentication.username).toString())
                    .addField("Status", getStatus(), true)
                    .addField("Controlling Player", getCurrentClientUserName(), true)
                    .addField("Online For", getOnlineTime(), true)
                    // end row 1
                    .addField("Health",  (CACHE.getPlayerCache().getThePlayer().getHealth()), true)
                    .addField("Dimension",
                              (nonNull(CACHE.getChunkCache().getCurrentDimension()) ? CACHE.getChunkCache().getCurrentDimension().name(): "None"),
                              true)
                    .addField("Ping", (Proxy.getInstance().isConnected() ? Proxy.getInstance().getClient().getPing() : 0) + "ms", true)
                    // end row 2
                    .addField("Proxy IP", CONFIG.server.getProxyAddress(), true)
                    .addField("Server", CONFIG.client.server.address + ':' + CONFIG.client.server.port, true)
                    .addField("Priority Queue", (CONFIG.authentication.prio ? "yes" : "no") + " [" + (CONFIG.authentication.prioBanned ? "banned" : "unbanned") + "]", true);
                    // end row 3
                embed.addField("Spectators", toggleStr(CONFIG.server.spectator.allowSpectator),true);
                if (!getSpectatorUserNames().isEmpty())
                    embed.addField("Online Spectators", String.join(", ", getSpectatorUserNames()), true);
                embed
                    .addField("2b2t Queue", getQueueStatus(), true);
                if (CONFIG.discord.reportCoords)
                    embed.addField("Coordinates", getCoordinates(CACHE.getPlayerCache()), true);
                embed
                    .addField("AutoUpdate", toggleStr(LAUNCH_CONFIG.auto_update), true);
                 return OK;
            });
    }
}
