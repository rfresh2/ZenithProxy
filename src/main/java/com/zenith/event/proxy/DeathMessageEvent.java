package com.zenith.event.proxy;

import com.zenith.feature.deathmessages.DeathMessageParseResult;

public class DeathMessageEvent {
    public final DeathMessageParseResult deathMessageParseResult;
    public final String deathMessageRaw;

    public DeathMessageEvent(final DeathMessageParseResult deathMessageParseResult, final String deathMessageRaw) {
        this.deathMessageParseResult = deathMessageParseResult;
        this.deathMessageRaw = deathMessageRaw;
    }
}
