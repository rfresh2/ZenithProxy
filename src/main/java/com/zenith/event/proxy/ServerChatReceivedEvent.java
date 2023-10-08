package com.zenith.event.proxy;

import com.github.steveice10.mc.protocol.data.game.PlayerListEntry;
import com.zenith.feature.deathmessages.DeathMessageParseResult;

import java.util.Optional;

import static com.zenith.Shared.CONFIG;

public record ServerChatReceivedEvent(Optional<PlayerListEntry> sender, String message, Optional<PlayerListEntry> whisperTarget, Optional<DeathMessageParseResult> deathMessage) {

    public boolean isDeathMessage() {
        return deathMessage.isPresent();
    }

    public boolean isPublicChat() {
        return !isWhisper() && !isDeathMessage() && sender.isPresent() && message.startsWith("<" + sender.get().getName() + ">");
    }

    public boolean isWhisper() {
        return whisperTarget.isPresent();
    }

    public boolean isOutgoingWhisper() {
        return isWhisper() && sender.isPresent() && sender.get().getName().equalsIgnoreCase(CONFIG.authentication.username);
    }

    public boolean isIncomingWhisper() {
        return isWhisper() && sender.isPresent() && !sender.get().getName().equalsIgnoreCase(CONFIG.authentication.username);
    }
}
