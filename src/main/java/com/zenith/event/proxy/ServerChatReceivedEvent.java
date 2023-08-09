package com.zenith.event.proxy;

import com.zenith.cache.data.tab.PlayerEntry;

import java.util.Optional;

public class ServerChatReceivedEvent {
    public String message; // raw string without formatting
    public Optional<PlayerEntry> sender;
    public boolean isWhisper;

    public ServerChatReceivedEvent(final Optional<PlayerEntry> sender, String message, boolean isWhisper) {
        this.sender = sender;
        this.message = message;
        this.isWhisper = isWhisper;
    }
}
