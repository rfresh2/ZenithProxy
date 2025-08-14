package com.zenith.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.Proxy;
import com.zenith.cache.data.entity.Entity;
import com.zenith.command.api.Command;
import com.zenith.command.api.CommandCategory;
import com.zenith.command.api.CommandContext;
import com.zenith.command.api.CommandUsage;
import com.zenith.feature.queue.Queue;
import com.zenith.module.impl.*;
import com.zenith.network.server.ServerSession;
import com.zenith.util.ImageInfo;
import com.zenith.util.math.MathHelper;
import net.dv8tion.jda.api.utils.TimeFormat;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import static com.zenith.Globals.*;
import static java.util.Objects.nonNull;

public class StatusCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.builder()
            .name("status")
            .category(CommandCategory.CORE)
            .description("""
            Prints the current status of ZenithProxy, the in-game player, and modules.
            """)
            .usageLines(
                "",
                "modules"
            )
            .aliases("s")
            .build();
    }

    public static String getCoordinates(final Entity entity) {
        if (CONFIG.discord.reportCoords) {
            return "||["
                    + MathHelper.floorI(entity.getX()) + ", "
                    + MathHelper.floorI(entity.getY()) + ", "
                    + MathHelper.floorI(entity.getZ())
                    + "]||";
        } else {
            return "Coords disabled";
        }
    }

    private String getCurrentClientUserName() {
        ServerSession currentConnection = Proxy.getInstance().getCurrentPlayer().get();
        if (nonNull(currentConnection)) {
            return currentConnection.getName() + " [" + currentConnection.getPing() + "ms]";
        } else {
            return "None";
        }
    }

    private List<String> getSpectatorUserNames() {
        return Proxy.getInstance().getSpectatorConnections().stream()
                .map(connection -> connection.getName() + " [" + connection.getPing() + "ms]")
                .collect(Collectors.toList());
    }

    private String getStatus() {
        if (Proxy.getInstance().isConnected()) {
            if (Proxy.getInstance().isInQueue()) {
                if (Proxy.getInstance().isPrio()) {
                    return "In Prio Queue [" + Proxy.getInstance().getQueuePosition() + " / " + Queue.getQueueStatus().prio() + "]\n"
                        + "ETA: " + Queue.getQueueEta(Proxy.getInstance().getQueuePosition()) + "\n"
                        + "(" + TimeFormat.TIME_LONG.format(Instant.now().plus(Duration.ofSeconds(Queue.getQueueWait(Proxy.getInstance().getQueuePosition())))) +")";
                } else {
                    return "In Queue [" + Proxy.getInstance().getQueuePosition() + " / " + Queue.getQueueStatus().regular() + "]\n"
                        + "ETA: " + Queue.getQueueEta(Proxy.getInstance().getQueuePosition()) + "\n"
                        + "(" + TimeFormat.TIME_LONG.format(Instant.now().plus(Duration.ofSeconds(Queue.getQueueWait(Proxy.getInstance().getQueuePosition())))) +")";
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
                    .thumbnail(getThumbnailImage())
                    .addField("Plugins", ImageInfo.inImageCode() ? "N/A (`java` required)" : toggleStr(CONFIG.plugins.enabled), true)
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
                    .thumbnail(getThumbnailImage())
                    .addField("Status", getStatus(), true)
                    .addField("Connected Player", getCurrentClientUserName(), true)
                    .addField("Online Duration", getOnlineTime(), true)
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
                if (CONFIG.server.queueStatusRefreshWhileNotOn2b2t || Proxy.getInstance().isOn2b2t()) {
                    embed
                        .addField("2b2t Queue", getQueueStatus(), true);
                }
                if (CONFIG.discord.reportCoords)
                    embed.addField("Coordinates", getCoordinates(CACHE.getPlayerCache().getThePlayer()), true);
                embed
                    .addField("AutoUpdate", toggleStr(LAUNCH_CONFIG.auto_update), true);
                 return OK;
            });
    }

    private static String getThumbnailImage() {
        return "Unknown".equals(CONFIG.authentication.username)
            ? "https://raw.githubusercontent.com/rfresh2/ZenithProxy/1.21.0/src/main/resources/servericon.png"
            : Proxy.getInstance().getPlayerHeadURL(CONFIG.authentication.username).toString();
    }
}
