package com.zenith.event.discord;

import com.zenith.discord.Embed;
import org.jspecify.annotations.Nullable;

/**
 * Notification messages sent by {@link com.zenith.discord.NotificationEventListener}
 *
 * Other notifications or informational messages are sent by other modules and not covered.
 *
 * For full coverage of main channel messages, see {@link MainChannelMessageSentEvent}
 */
public record NotificationSentEvent(
    @Nullable Embed embed,
    @Nullable String message
) { }
