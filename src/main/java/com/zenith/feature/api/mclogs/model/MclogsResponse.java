package com.zenith.feature.api.mclogs.model;

import org.jspecify.annotations.Nullable;

public record MclogsResponse(
    boolean success,
    @Nullable String error,
    @Nullable String id,
    @Nullable String url
) { }
