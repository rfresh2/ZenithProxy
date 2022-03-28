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
                        .title("ZenithProxy Status" + " : " + CONFIG.authentication.username)
                        .color(this.proxy.isConnected() ? Color.CYAN : Color.RUBY)
                        .addField("Status", getStatus(), true)
                        .addField("Server", CONFIG.client.server.address, true)
                        .addField("Proxy IP", CONFIG.server.getProxyAddress(), false)
                        .addField("Dimension",
                                dimensionIdToString(CACHE.getPlayerCache().getDimension()),
                                true)
                        .addField("Coordinates", getCoordinates(CACHE.getPlayerCache()), true)
                        .addField("Health", ""+((int)CACHE.getPlayerCache().getThePlayer().getHealth()), false)
                        .addField("AutoDisconnect",
                                (CONFIG.client.extra.utility.actions.autoDisconnect.enabled ? "on" : "off")
                                        + " [" + CONFIG.client.extra.utility.actions.autoDisconnect.health + "]", true)
                        .addField("AutoReconnect",
                                (CONFIG.client.extra.autoReconnect.enabled ? "on" : "off")
                                        + " [" + CONFIG.client.extra.autoReconnect.delaySeconds + "]", true)
                        .addField("AutoRespawn",
                                (CONFIG.client.extra.autoRespawn.enabled ? "on" : "off")
                                        + " [" + CONFIG.client.extra.autoRespawn.delayMillis + "]", true)
                        .addField("AntiAFK",
                                (CONFIG.client.extra.antiafk.enabled ? "on" : "off"), true)
                        .addField("VisualRange Notifications", (CONFIG.client.extra.visualRangeAlert ? "on" : "off"), true)
                        .addField("Client Connection Notifications", (CONFIG.client.extra.clientConnectionMessages ? "on" : "off"), true)
                        .build())
                .build().asRequest();
    }

    private String getStatus() {
        if (proxy.isConnected()) {
            if (proxy.isInQueue()) {
                if (proxy.getIsPrio().isPresent()) {
                    if (proxy.getIsPrio().get()) {
                        return "In Priority Queue [" + this.proxy.getQueuePosition() + " / " + Queue.getQueueStatus().prio + "]\nETA: " + getQueueEta(Queue.getQueueStatus().prio, this.proxy.getQueuePosition());
                    } else {
                        return "In Regular Queue [" + this.proxy.getQueuePosition() + " / " + Queue.getQueueStatus().regular + "]\nETA: " + getQueueEta(Queue.getQueueStatus().regular, this.proxy.getQueuePosition());
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
            return "["
                    + (int) playerCache.getX() + ", "
                    + (int) playerCache.getY() + ", "
                    + (int) playerCache.getZ()
                    + "]";
        } else {
            return "Coords disabled";
        }
    }

    public String getQueueEta(final Integer queueLength, final Integer queuePos) {
        double seconds = Queue.getQueueWait(queueLength, queuePos);
        return (int)(seconds / 3600) + ":" + (int)((seconds / 60) % 60) + ":" + (int)(seconds % 60);
    }
}
