package com.zenith.event.proxy;

import com.collarmc.pounce.EventInfo;
import com.collarmc.pounce.Preference;
import com.zenith.cache.data.tab.PlayerEntry;

@EventInfo(preference = Preference.POOL)
public class ServerPlayerConnectedEvent {
    public final PlayerEntry playerEntry;
    public ServerPlayerConnectedEvent(final PlayerEntry playerEntry) {
        this.playerEntry = playerEntry;
    }
}
