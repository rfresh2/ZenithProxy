package com.zenith.event.discord;

import com.zenith.discord.Embed;
import org.jspecify.annotations.Nullable;

/**
 * Any messages normally sent to the main discord channel.
 * Event still posts even if the discord bot is disabled
 */
public record MainChannelMessageSentEvent(
    @Nullable Embed embed,
    @Nullable String message
) {
}
