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
                        .addField("Status", proxy.isConnected() ?
                                (this.proxy.isInQueue()
                                        ? "Queueing [" + this.proxy.getQueuePosition() + " / " + Queue.getQueueStatus().regular + "]"
                                        : "Online")
                                : "Disconnected", true)
                        .addField("Server", CONFIG.client.server.address, true)
                        .addField("Proxy IP", CONFIG.server.getProxyAddress(), false)
                        .addField("Dimension",
                                dimensionIdToString(CACHE.getPlayerCache().getDimension()),
                                true)
                        .addField("Coordinates", getCoordinates(CACHE.getPlayerCache()), true)
                        .addField("Health", ""+CACHE.getPlayerCache().getThePlayer().getHealth(), false)
                        .build())
                .build().asRequest();
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
        return "["
                + (int) playerCache.getX() + ", "
                + (int) playerCache.getY() + ", "
                + (int) playerCache.getZ()
                + "]";
    }
}
