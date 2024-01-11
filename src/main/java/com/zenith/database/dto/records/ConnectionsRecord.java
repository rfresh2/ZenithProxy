package com.zenith.database.dto.records;


import com.zenith.database.dto.enums.Connectiontype;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ConnectionsRecord(OffsetDateTime time, Connectiontype connection, String playerName, UUID playerUuid) { }
