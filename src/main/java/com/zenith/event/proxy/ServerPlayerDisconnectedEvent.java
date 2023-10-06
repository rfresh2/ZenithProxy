package com.zenith.event.proxy;

import com.github.steveice10.mc.protocol.data.game.PlayerListEntry;

public record ServerPlayerDisconnectedEvent(PlayerListEntry playerEntry) { }
