package com.zenith.event.proxy;

import com.zenith.cache.data.entity.EntityPlayer;
import com.zenith.cache.data.tab.PlayerEntry;

public class NewPlayerInVisualRangeEvent {
    public final PlayerEntry playerEntry;
    public final EntityPlayer playerEntity;

    public NewPlayerInVisualRangeEvent(PlayerEntry playerEntry, EntityPlayer entity) {
        this.playerEntry = playerEntry;
        this.playerEntity = entity;
    }
}
