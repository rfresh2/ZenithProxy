package com.zenith.event.proxy;

import com.collarmc.pounce.EventInfo;
import com.collarmc.pounce.Preference;
import com.zenith.cache.data.entity.EntityPlayer;
import com.zenith.cache.data.tab.PlayerEntry;

@EventInfo(preference = Preference.POOL)
public class NewPlayerInVisualRangeEvent {
    public final PlayerEntry playerEntry;
    public final EntityPlayer playerEntity;

    public NewPlayerInVisualRangeEvent(PlayerEntry playerEntry, EntityPlayer entity) {
        this.playerEntry = playerEntry;
        this.playerEntity = entity;
    }
}
