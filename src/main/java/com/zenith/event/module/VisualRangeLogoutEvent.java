package com.zenith.event.module;

import com.github.steveice10.mc.protocol.data.game.PlayerListEntry;
import com.zenith.cache.data.entity.EntityPlayer;

public record VisualRangeLogoutEvent(PlayerListEntry playerEntry, EntityPlayer playerEntity, boolean isFriend) { }
