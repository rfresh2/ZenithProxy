package com.zenith.event.proxy;

import com.zenith.cache.data.entity.EntityPlayer;
import org.geysermc.mcprotocollib.protocol.data.game.PlayerListEntry;

public record PlayerLogoutInVisualRangeEvent(PlayerListEntry playerEntry, EntityPlayer playerEntity) { }
