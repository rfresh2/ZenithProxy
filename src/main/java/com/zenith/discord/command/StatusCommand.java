package com.zenith.discord.command;

import com.zenith.Proxy;
import com.zenith.util.Queue;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.discordjson.json.MessageCreateRequest;
import discord4j.rest.entity.RestChannel;
import discord4j.rest.util.Color;
import discord4j.rest.util.MultipartRequest;

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
                        .addField("Proxy IP", "todo:" + CONFIG.server.bind.port, false)
                        .build())
                .build().asRequest();
    }
}
