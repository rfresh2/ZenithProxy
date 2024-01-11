package com.zenith.database.dto.records;


import java.time.OffsetDateTime;
import java.util.UUID;

public record ChatsRecord(OffsetDateTime time, String chat, String playerName, UUID playerUuid) { }
