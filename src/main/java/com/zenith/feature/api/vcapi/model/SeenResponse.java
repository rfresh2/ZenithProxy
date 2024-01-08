package com.zenith.feature.api.vcapi.model;

import java.time.OffsetDateTime;

public record SeenResponse(OffsetDateTime firstSeen, OffsetDateTime lastSeen) { }
