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
import static com.zenith.util.Constants.DISCORD_LOG;

public class ConnectCommand extends Command {

    public ConnectCommand(Proxy proxy) {
        super(proxy, "connect", "Connect the current player to the server");
    }

    @Override
    public MultipartRequest<MessageCreateRequest> execute(MessageCreateEvent event, RestChannel restChannel) {
        try {
            this.proxy.connect();
            return null;
        } catch (final Exception e) {
            DISCORD_LOG.error("Failed to connect", e);
            return getFailedConnectionMessage();
        }
    }

    private MultipartRequest<MessageCreateRequest> getFailedConnectionMessage() {
        return MessageCreateSpec.builder()
                .addEmbed(EmbedCreateSpec.builder()
                        .title("Proxy Failed to Connect!" + " : " + CONFIG.authentication.username)
                        .color(Color.RED)
                        .addField("Server", CONFIG.client.server.address, true)
                        .addField("Proxy IP", CONFIG.server.getProxyAddress(), false)
                        .build())
                .build().asRequest();
    }
}
