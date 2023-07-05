package com.zenith.feature.deathmessages;

import lombok.Data;

import java.util.Optional;

@Data
public final class DeathMessageParseResult {
    final String victim;
    final Optional<Killer> killer;
    final Optional<String> weapon;
    final DeathMessageSchemaInstance deathMessageSchemaInstance;
}
