package com.zenith.event.proxy;

import com.collarmc.pounce.EventInfo;
import com.collarmc.pounce.Preference;

@EventInfo(preference = Preference.POOL)
public class DiscordMessageSentEvent {
    public String message;
    public DiscordMessageSentEvent(final String message) {
        this.message = message;
    }
}
