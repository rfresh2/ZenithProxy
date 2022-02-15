package com.zenith.discord.command;

import com.zenith.Proxy;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.discordjson.json.MessageCreateRequest;
import discord4j.rest.entity.RestChannel;
import discord4j.rest.util.Color;
import discord4j.rest.util.MultipartRequest;

import static com.zenith.util.Constants.CONFIG;
import static com.zenith.util.Constants.DISCORD_LOG;

public class DisconnectCommand extends Command {
    public DisconnectCommand(Proxy proxy) {
        super(proxy, "disconnect", "Disconnect the current player from the server");
    }

    @Override
    public MultipartRequest<MessageCreateRequest> execute(MessageCreateEvent event, RestChannel restChannel) {
        try {
            this.proxy.disconnect();
            return getDisconnectMessageCreateRequest(true);
        } catch (final Exception e) {
            DISCORD_LOG.error("Failed to disconnect", e);
            return getDisconnectMessageCreateRequest(false);
        }
    }

    private MultipartRequest<MessageCreateRequest> getDisconnectMessageCreateRequest(boolean success) {
        return MessageCreateSpec.builder()
                .addEmbed(EmbedCreateSpec.builder()
                        .title("ZenithProxy Disconnect " + (success ? "Succeeded" : "Failed") + " : " + CONFIG.authentication.username)
                        .color((success ? Color.LIGHT_SEA_GREEN : Color.RED))
                        .image(this.proxy.getAvatarURL().toString())
                        .addField("Server", CONFIG.client.server.address, true)
                        .build())
                .build().asRequest();
    }
}
