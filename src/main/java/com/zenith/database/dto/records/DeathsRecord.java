package com.zenith.database.dto.records;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.OffsetDateTime;
import java.util.UUID;


@Data
@AllArgsConstructor
@Accessors(chain = true)
public class DeathsRecord {
    private OffsetDateTime time;
    private String deathMessage;
    private String victimPlayerName;
    private UUID victimPlayerUuid;
    private String killerPlayerName;
    private UUID killerPlayerUuid;
    private String weaponName;
    private String killerMob;
}
