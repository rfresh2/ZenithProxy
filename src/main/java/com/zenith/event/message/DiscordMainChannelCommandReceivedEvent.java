package com.zenith.event.message;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public record DiscordMainChannelCommandReceivedEvent(MessageReceivedEvent event) {
    public String message() {
        return event.getMessage().getContentRaw();
    }

    @Deprecated
    public Member member() {
        return event.getMember();
    }
}
