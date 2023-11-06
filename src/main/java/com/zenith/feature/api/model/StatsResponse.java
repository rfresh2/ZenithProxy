package com.zenith.feature.api.model;

import java.time.OffsetDateTime;

public record StatsResponse(
    int joinCount,
    int leaveCount,
    int deathCount,
    int killCount,
    OffsetDateTime firstSeen,
    OffsetDateTime lastSeen,
    int playtimeSeconds,
    int playtimeSecondsMonth,
    int chatsCount
    ) {
}
