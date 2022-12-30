package com.zenith.util.deathmessages;

import lombok.Data;

import java.util.Optional;

@Data
public final class DeathMessageParseResult {
    final String victim;
    final Optional<String> killer;
    final Optional<String> weapon;
    final DeathMessageSchemaInstance deathMessageSchemaInstance;
}
