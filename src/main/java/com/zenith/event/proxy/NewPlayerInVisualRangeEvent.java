package com.zenith.event.proxy;

import com.github.steveice10.mc.protocol.data.game.PlayerListEntry;
import com.zenith.cache.data.entity.EntityPlayer;

public record NewPlayerInVisualRangeEvent(PlayerListEntry playerEntry, EntityPlayer playerEntity) { }
