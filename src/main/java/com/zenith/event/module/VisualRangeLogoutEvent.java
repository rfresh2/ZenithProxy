package com.zenith.event.module;

import com.zenith.cache.data.entity.EntityPlayer;
import org.geysermc.mcprotocollib.protocol.data.game.PlayerListEntry;

public record VisualRangeLogoutEvent(PlayerListEntry playerEntry, EntityPlayer playerEntity, boolean isFriend) { }
