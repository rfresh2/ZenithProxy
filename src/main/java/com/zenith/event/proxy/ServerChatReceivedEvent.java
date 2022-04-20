package com.zenith.event.proxy;

import com.collarmc.pounce.EventInfo;
import com.collarmc.pounce.Preference;

@EventInfo(preference = Preference.POOL)
public class ServerChatReceivedEvent {
    public String message; // raw string without formatting
    public ServerChatReceivedEvent(String message) {
        this.message = message;
    }
}
