package com.zenith.feature.deathmessages;

import java.util.Optional;

public record DeathMessageParseResult(
    String victim,
    Optional<Killer> killer,
    Optional<String> weapon,
    DeathMessageSchemaInstance deathMessageSchemaInstance
) {
}
