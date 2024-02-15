package com.zenith.feature.api.vcapi.model;

import java.time.OffsetDateTime;

public record QueueResponse(int prio, int regular, OffsetDateTime time) { }
