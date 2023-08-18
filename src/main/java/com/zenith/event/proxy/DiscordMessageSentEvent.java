package com.zenith.event.proxy;

public class DiscordMessageSentEvent {
    public final String message;
    public DiscordMessageSentEvent(final String message) {
        this.message = message;
    }
}
