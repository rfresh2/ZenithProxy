package com.zenith.event.proxy;

import com.github.steveice10.mc.protocol.data.game.PlayerListEntry;

import java.util.Optional;

public record ServerChatReceivedEvent(Optional<PlayerListEntry> sender, String message, boolean isWhisper) { }
