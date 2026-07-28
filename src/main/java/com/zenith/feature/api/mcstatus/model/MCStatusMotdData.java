package com.zenith.feature.api.mcstatus.model;

public record MCStatusMotdData(
    String raw,
    String clean,
    String html
) {
}
