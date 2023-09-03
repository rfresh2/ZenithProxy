package com.zenith.cache.data.chunk;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class Dimension {
    final String dimensionName;
    final int dimensionId;
    final int height;
    final int minY;
}
