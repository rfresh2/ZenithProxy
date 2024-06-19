package com.zenith.feature.api.mcstatus.model;

import org.jetbrains.annotations.Nullable;

public record MCStatusResponse(
    boolean online,
    String host,
    int port,
    @Nullable String ip_address
) {
}
