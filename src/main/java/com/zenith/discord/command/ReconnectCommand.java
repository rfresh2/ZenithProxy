package com.zenith.discord.command;

import com.zenith.Proxy;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.discordjson.json.MessageCreateRequest;
import discord4j.rest.entity.RestChannel;
import discord4j.rest.util.MultipartRequest;

import static com.zenith.util.Constants.SYSTEM_DISCONNECT;

public class ReconnectCommand extends Command {
    public ReconnectCommand(Proxy proxy) {
        super(proxy, "reconnect", "disconnect and reconnect the proxy client");
    }

    @Override
    public MultipartRequest<MessageCreateRequest> execute(MessageCreateEvent event, RestChannel restChannel) {
        this.proxy.disconnect(SYSTEM_DISCONNECT);
        this.proxy.cancelAutoReconnect();
        this.proxy.connect();
        return null;
    }
}
