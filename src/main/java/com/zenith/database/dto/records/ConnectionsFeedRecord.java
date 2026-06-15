package com.zenith.database.dto.records;


import com.zenith.database.dto.enums.ConnectionType;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ConnectionsFeedRecord(
    OffsetDateTime time,
    ConnectionType connection,
    String playerName,
    UUID playerUuid
) { }
