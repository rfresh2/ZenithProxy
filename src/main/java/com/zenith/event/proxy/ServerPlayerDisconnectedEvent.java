package com.zenith.event.proxy;

import com.collarmc.pounce.EventInfo;
import com.collarmc.pounce.Preference;
import com.zenith.util.cache.data.tab.PlayerEntry;

@EventInfo(preference = Preference.POOL)
public class ServerPlayerDisconnectedEvent {
    public final PlayerEntry playerEntry;
    public ServerPlayerDisconnectedEvent(final PlayerEntry playerEntry) {
        this.playerEntry = playerEntry;
    }
}
