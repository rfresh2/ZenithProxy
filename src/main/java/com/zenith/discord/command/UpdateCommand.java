package com.zenith.discord.command;

import com.zenith.Proxy;
import com.zenith.event.proxy.UpdateStartEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.discordjson.json.MessageCreateRequest;
import discord4j.rest.entity.RestChannel;
import discord4j.rest.util.Color;
import discord4j.rest.util.MultipartRequest;

import static com.zenith.util.Constants.*;

public class UpdateCommand extends Command {

    public UpdateCommand(final Proxy proxy) {
        super(proxy, "update", "Restarts and updates the proxy software");
    }

    @Override
    public MultipartRequest<MessageCreateRequest> execute(MessageCreateEvent event, RestChannel restChannel) {
        validateUserHasAccountOwnerRole(event, restChannel);
        try {
            EVENT_BUS.dispatch(new UpdateStartEvent());
            CONFIG.discord.isUpdating = true;
            this.proxy.stop();
        } catch (final Exception e) {
            DISCORD_LOG.error("Failed to update", e);
            CONFIG.discord.isUpdating = false;
            saveConfig();
            return getFailedUpdateMessage();
        }
        return null;
    }

    private MultipartRequest<MessageCreateRequest> getFailedUpdateMessage() {
        return MessageCreateSpec.builder()
                .addEmbed(EmbedCreateSpec.builder()
                        .title("Failed updating")
                        .color(Color.RED)
                        .build())
                .build().asRequest();
    }
}
