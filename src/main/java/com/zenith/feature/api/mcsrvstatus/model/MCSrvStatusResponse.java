package com.zenith.feature.api.mcsrvstatus.model;

import org.jspecify.annotations.Nullable;

public record MCSrvStatusResponse(
    boolean online,
    String ip,
    int port,
    @Nullable MCSrvStatusMotdData motd
) {
}
