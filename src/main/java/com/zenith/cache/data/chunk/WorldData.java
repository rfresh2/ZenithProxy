package com.zenith.cache.data.chunk;

public record WorldData(String dimensionType, String worldName, long hashedSeed, boolean debug, boolean flat) { }
