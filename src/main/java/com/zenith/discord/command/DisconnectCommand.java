package com.zenith.discord.command;

import com.zenith.Proxy;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.discordjson.json.MessageCreateRequest;
import discord4j.rest.entity.RestChannel;
import discord4j.rest.util.MultipartRequest;

import static com.zenith.util.Constants.*;

public class DisconnectCommand extends Command {
    public DisconnectCommand(Proxy proxy) {
        super(proxy, "disconnect", "Disconnect the current player from the server");
    }

    @Override
    public MultipartRequest<MessageCreateRequest> execute(MessageCreateEvent event, RestChannel restChannel) {
        try {
            this.proxy.disconnect();
            if (this.proxy.cancelAutoReconnect()) {
                return getAutoReconnectCancelledMessage();
            }
            return null;
        } catch (final Exception e) {
            DISCORD_LOG.error("Failed to disconnect", e);
            return getFailedToDisconnectMessage();
        }
    }

    private MultipartRequest<MessageCreateRequest> getFailedToDisconnectMessage() {
        return MessageCreateSpec.builder()
                .addEmbed(EmbedCreateSpec.builder()
                        .title("ZenithProxy Failed to Disconnect : " + CONFIG.authentication.username)
                        .build())
                .build().asRequest();
    }

    private MultipartRequest<MessageCreateRequest> getAutoReconnectCancelledMessage() {
        return MessageCreateSpec.builder()
                .addEmbed(EmbedCreateSpec.builder()
                        .title("AutoReconnect Cancelled")
                        .build())
                .build().asRequest();
    }
}
