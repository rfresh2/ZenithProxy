package com.zenith.event.proxy;

import com.zenith.cache.data.tab.PlayerEntry;

public class ServerPlayerDisconnectedEvent {
    public final PlayerEntry playerEntry;
    public ServerPlayerDisconnectedEvent(final PlayerEntry playerEntry) {
        this.playerEntry = playerEntry;
    }
}
