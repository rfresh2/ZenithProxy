package com.zenith.database.dto.records;


import java.time.OffsetDateTime;
import java.util.UUID;


public record DeathsFeedRecord(
    OffsetDateTime time,
    String deathMessage,
    String victimPlayerName,
    UUID victimPlayerUuid,
    String killerPlayerName,
    UUID killerPlayerUuid,
    String weaponName,
    String killerMob,
    String component
) { }
