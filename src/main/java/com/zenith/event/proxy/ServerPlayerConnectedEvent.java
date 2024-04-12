package com.zenith.event.proxy;

import org.geysermc.mcprotocollib.protocol.data.game.PlayerListEntry;

public record ServerPlayerConnectedEvent(PlayerListEntry playerEntry) { }
