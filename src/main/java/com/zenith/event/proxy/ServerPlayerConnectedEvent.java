package com.zenith.event.proxy;

import com.zenith.cache.data.tab.PlayerEntry;

public class ServerPlayerConnectedEvent {
    public final PlayerEntry playerEntry;
    public ServerPlayerConnectedEvent(final PlayerEntry playerEntry) {
        this.playerEntry = playerEntry;
    }
}
