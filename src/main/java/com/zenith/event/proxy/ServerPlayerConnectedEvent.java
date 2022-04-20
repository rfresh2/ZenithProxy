package com.zenith.event.proxy;

import com.collarmc.pounce.EventInfo;
import com.collarmc.pounce.Preference;

@EventInfo(preference = Preference.POOL)
public class ServerPlayerConnectedEvent {
    public final String playerName;
    public ServerPlayerConnectedEvent(final String playerName) {
        this.playerName = playerName;
    }
}
