package com.zenith.event.proxy;

import com.zenith.feature.deathmessages.DeathMessageParseResult;

public record DeathMessageEvent(DeathMessageParseResult deathMessageParseResult, String deathMessageRaw) { }
