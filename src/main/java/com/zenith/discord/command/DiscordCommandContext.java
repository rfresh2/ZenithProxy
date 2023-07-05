package com.zenith.discord.command;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.entity.RestChannel;

import java.util.ArrayList;
import java.util.List;

public class DiscordCommandContext extends CommandContext {
    private final MessageCreateEvent messageCreateEvent;
    private final RestChannel restChannel;

    public DiscordCommandContext(final String input, EmbedCreateSpec.Builder embedBuilder, final List<String> multiLineOutput, MessageCreateEvent messageCreateEvent, RestChannel restChannel) {
        super(input, CommandSource.DISCORD, embedBuilder, multiLineOutput);
        this.messageCreateEvent = messageCreateEvent;
        this.restChannel = restChannel;
    }

    public static DiscordCommandContext create(final String input, final MessageCreateEvent messageCreateEvent, RestChannel restChannel) {
        return new DiscordCommandContext(input, EmbedCreateSpec.builder(), new ArrayList<>(), messageCreateEvent, restChannel);
    }

    public MessageCreateEvent getMessageCreateEvent() {
        return messageCreateEvent;
    }

    public RestChannel getRestChannel() {
        return restChannel;
    }
}
