package com.zenith.feature.api.mcsrvstatus.model;

public record MCSrvStatusResponse(
    boolean online,
    String ip,
    int port
) {
}
