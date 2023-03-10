package com.zenith.event.proxy;

import com.collarmc.pounce.EventInfo;
import com.collarmc.pounce.Preference;
import com.zenith.util.deathmessages.DeathMessageParseResult;

@EventInfo(preference = Preference.POOL)
public class DeathMessageEvent {
    public final DeathMessageParseResult deathMessageParseResult;
    public final String deathMessageRaw;

    public DeathMessageEvent(final DeathMessageParseResult deathMessageParseResult, final String deathMessageRaw) {
        this.deathMessageParseResult = deathMessageParseResult;
        this.deathMessageRaw = deathMessageRaw;
    }
}
