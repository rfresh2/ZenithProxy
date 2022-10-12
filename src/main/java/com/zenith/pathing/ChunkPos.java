package com.zenith.pathing;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class ChunkPos {
    private final int x;
    private final int z;
    private final int y;
}
