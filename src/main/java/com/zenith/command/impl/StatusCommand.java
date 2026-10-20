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
import static com.zenith.discord.DiscordBot.escape;
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
            return "||[%d, %d, %d]||".formatted(
                MathHelper.floorI(entity.getX()),
                MathHelper.floorI(entity.getY()),
                MathHelper.floorI(entity.getZ())
            );
        } else {
            return "Coords disabled";
        }
    }

    private String getCurrentClientUserName() {
        ServerSession currentConnection = Proxy.getInstance().getCurrentPlayer().get();
        if (nonNull(currentConnection)) {
            return "%s [%dms]".formatted(
                escape(currentConnection.getName()),
                currentConnection.getPing()
            );
        } else {
            return "None";
        }
    }

    private List<String> getSpectatorUserNames() {
        return Proxy.getInstance().getSpectatorConnections().stream()
                .map(connection -> "%s [%dms]".formatted(
                    escape(connection.getName()),
                    connection.getPing())
                )
                .collect(Collectors.toList());
    }

    private String getStatus() {
        if (Proxy.getInstance().isConnected()) {
            if (Proxy.getInstance().isInQueue()) {
                if (Proxy.getInstance().isPrio()) {
                    return "In Prio Queue [%d / %d]\nETA: %s\n(%s)".formatted(
                        Proxy.getInstance().getQueuePosition(),
                        Queue.getQueueStatus().prio(),
                        Queue.getQueueEta(Proxy.getInstance().getQueuePosition()),
                        TimeFormat.DATE_TIME_SHORT.format(Instant.now()
                            .plus(Duration.ofSeconds(Queue.getQueueWait(Proxy.getInstance().getQueuePosition()))))
                    );
                } else {
                    return "In Queue [%d / %d]\nETA: %s\n(%s)".formatted(
                        Proxy.getInstance().getQueuePosition(),
                        Queue.getQueueStatus().regular(),
                        Queue.getQueueEta(Proxy.getInstance().getQueuePosition()),
                        TimeFormat.DATE_TIME_SHORT.format(Instant.now()
                            .plus(Duration.ofSeconds(Queue.getQueueWait(Proxy.getInstance().getQueuePosition()))))
                    );
                }
            } else {
                return "Online";
            }
        } else {
            return "Disconnected";
        }
    }

    private String getQueueStatus() {
        return "Priority: %d [%s]\nRegular: %d [%s]".formatted(
            Queue.getQueueStatus().prio(),
            Queue.getQueueEta(Queue.getQueueStatus().prio()),
            Queue.getQueueStatus().regular(),
            Queue.getQueueEta(Queue.getQueueStatus().regular())
        );
    }

    public String getOnlineTime() {
        return Proxy.getInstance().isConnected()
            ? Proxy.getInstance().getOnlineTimeString()
            : "Offline";
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("status")
            .then(literal("modules").executes(c -> {
                c.getSource().getEmbed()
                    .title("ZenithProxy " + VERSION + " - " + CONFIG.authentication.username)
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
                    .title("ZenithProxy " + VERSION + " - " + CONFIG.authentication.username)
                    .color(Proxy.getInstance().isConnected()
                               ? (Proxy.getInstance().isInQueue()
                        ? CONFIG.theme.inQueue.discord()
                        : CONFIG.theme.success.discord())
                               : CONFIG.theme.error.discord())
                    .thumbnail(getThumbnailImage())
                    .addField("Status", getStatus())
                    //end row 1
                    .addField("Uptime", MathHelper.formatDuration(Duration.ofMillis(System.currentTimeMillis() - Proxy.getInstance().getStartTime())), true)
                    .addField("Online Duration", getOnlineTime(), true)
                    .addField("Ping", (Proxy.getInstance().isConnected() ? Proxy.getInstance().getClient().getPing() : 0) + "ms", true)
                    // end row 2
                    .addField("Health",  (CACHE.getPlayerCache().getThePlayer().getHealth()), true)
                    .addField("Dimension",
                              (nonNull(CACHE.getChunkCache().getCurrentDimension()) ? CACHE.getChunkCache().getCurrentDimension().name(): "None"),
                              true)
                    .addField("TPS", "%s [%s]".formatted(TPS.getTPS(), CLIENT_TPS.getTPS()), true)
                    // end row 3
                    .addField("Proxy IP", escape(CONFIG.server.getProxyAddress()), true)
                    .addField("Server", escape(CONFIG.client.server.address + ':' + CONFIG.client.server.port), true)
                    .addField("Connected Player", getCurrentClientUserName(), true);
                    // end row 4
                if (Proxy.getInstance().isOn2b2t()) {
                    embed.addField("Priority Queue", (CONFIG.authentication.prio ? "yes" : "no"), true);
                }
                if (!getSpectatorUserNames().isEmpty())
                    embed.addField("Spectators", String.join(", ", getSpectatorUserNames()), true);
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
            ? "https://cdn.2b2t.vc/default-zenith-server-icon.png"
            : Proxy.getInstance().getPlayerHeadURL(CONFIG.authentication.username).toString();
    }
}
