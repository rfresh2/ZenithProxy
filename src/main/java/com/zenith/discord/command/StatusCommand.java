package com.zenith.discord.command;

import com.zenith.Proxy;
import com.zenith.util.Queue;
import com.zenith.util.cache.data.PlayerCache;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.discordjson.json.MessageCreateRequest;
import discord4j.rest.entity.RestChannel;
import discord4j.rest.util.Color;
import discord4j.rest.util.MultipartRequest;

import java.time.Instant;

import static com.zenith.util.Constants.CACHE;
import static com.zenith.util.Constants.CONFIG;

public class StatusCommand extends Command {
    public StatusCommand(Proxy proxy) {
        super(proxy, "status", "Gets the current proxy status");
    }

    @Override
    public MultipartRequest<MessageCreateRequest> execute(MessageCreateEvent event, RestChannel restChannel) {
        return MessageCreateSpec.builder()
                .addEmbed(EmbedCreateSpec.builder()
                        .title("Proxy Status" + " : " + CONFIG.authentication.username)
                        .color(this.proxy.isConnected() ? Color.CYAN : Color.RUBY)
                        .addField("Status", getStatus(), true)
                        .addField("Connected User", getUserName(), true)
                        .addField("Online Time", getOnlineTime(), true)
                        .addField("Server", CONFIG.client.server.address + ':' + CONFIG.client.server.port, true)
                        .addField("2b2t Queue", getQueueStatus(), false)
                        .addField("Proxy IP", CONFIG.server.getProxyAddress(), false)
                        .addField("Dimension",
                                dimensionIdToString(CACHE.getPlayerCache().getDimension()),
                                true)
                        .addField("Coordinates", getCoordinates(CACHE.getPlayerCache()), true)
                        .addField("Health", ""+((int)CACHE.getPlayerCache().getThePlayer().getHealth()), false)
                        .addField("AutoDisconnect",
                                "Health: " + (CONFIG.client.extra.utility.actions.autoDisconnect.enabled ? "on" : "off")
                                        + " [" + CONFIG.client.extra.utility.actions.autoDisconnect.health + "]"
                                        + "\nAutoClientDisconnect: " + (CONFIG.client.extra.utility.actions.autoDisconnect.autoClientDisconnect ? "on" : "off"), true)
                        .addField("AutoReconnect",
                                (CONFIG.client.extra.autoReconnect.enabled ? "on" : "off")
                                        + " [" + CONFIG.client.extra.autoReconnect.delaySeconds + "]", true)
                        .addField("AutoRespawn",
                                (CONFIG.client.extra.autoRespawn.enabled ? "on" : "off")
                                        + " [" + CONFIG.client.extra.autoRespawn.delayMillis + "]", true)
                        .addField("AntiAFK",
                                (CONFIG.client.extra.antiafk.enabled ? "on" : "off"), true)
                        .addField("VisualRange Notifications", (CONFIG.client.extra.visualRangeAlert ? "on" : "off")
                                + " [Mention: " + (CONFIG.client.extra.visualRangeAlertMention ? "on" : "off") + "]", true)
                        .addField("Client Connection Notifications", (CONFIG.client.extra.clientConnectionMessages ? "on" : "off"), true)
                        .addField("Active Hours", (CONFIG.client.extra.utility.actions.activeHours.enabled ? "on" : "off"), false)
                        .addField("Display Coordinates", (CONFIG.discord.reportCoords ? "on" : "off"), true)
                        .addField("Chat Relay", (CONFIG.discord.chatRelay.channelId.length() > 0 ? (CONFIG.discord.chatRelay.enable ? "on" : "off") : "Not Configured"), false)
                        .addField("AutoUpdate", (CONFIG.autoUpdate ? "on" : "off"), true)
                        .build())
                .build().asRequest();
    }

    private String getUserName() {
        if (this.proxy.getConnectedClientGameProfile().isPresent()) {
            return this.proxy.getConnectedClientGameProfile().get().getName();
        } else {
            return "None";
        }
    };

    private String getStatus() {
        if (proxy.isConnected()) {
            if (proxy.isInQueue()) {
                if (proxy.getIsPrio().isPresent()) {
                    if (proxy.getIsPrio().get()) {
                        return "In Priority Queue [" + this.proxy.getQueuePosition() + " / " + Queue.getQueueStatus().prio + "]\nETA: " + Queue.getQueueEta(Queue.getQueueStatus().prio, this.proxy.getQueuePosition());
                    } else {
                        return "In Regular Queue [" + this.proxy.getQueuePosition() + " / " + Queue.getQueueStatus().regular + "]\nETA: " + Queue.getQueueEta(Queue.getQueueStatus().regular, this.proxy.getQueuePosition());
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
        return "Priority: " + Queue.getQueueStatus().prio + " [" + Queue.getQueueEta(Queue.getQueueStatus().prio, Queue.getQueueStatus().prio) + "]"
                + "\nRegular: " + Queue.getQueueStatus().regular + " [" + Queue.getQueueEta(Queue.getQueueStatus().regular, Queue.getQueueStatus().regular) + "]";
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

    public String getOnlineTime() {
        if (this.proxy.isConnected()) {
            long secondsOnline = Instant.now().getEpochSecond() - this.proxy.getConnectTime().getEpochSecond();
            // hours:minutes:seconds
            return Queue.getEtaStringFromSeconds(secondsOnline);
        } else {
            return "Not Online!";
        }
    }
}
