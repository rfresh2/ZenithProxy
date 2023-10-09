package com.zenith.event.proxy;

import discord4j.core.event.domain.message.MessageCreateEvent;

public record DiscordMessageSentEvent(String message, MessageCreateEvent event) { }
