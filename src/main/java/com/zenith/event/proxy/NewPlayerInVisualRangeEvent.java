package com.zenith.event.proxy;

import com.collarmc.pounce.EventInfo;
import com.collarmc.pounce.Preference;
import com.zenith.cache.data.entity.Entity;
import com.zenith.cache.data.tab.PlayerEntry;

@EventInfo(preference = Preference.POOL)
public class NewPlayerInVisualRangeEvent {
    public final PlayerEntry playerEntry;
    public final Entity playerEntity;
    public NewPlayerInVisualRangeEvent(PlayerEntry playerEntry, Entity entity) {
        this.playerEntry = playerEntry;
        this.playerEntity = entity;
    }
}
