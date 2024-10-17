package com.zenith.feature.queue.mcping.rawData;

import java.util.List;

public record Players(
    int max,
    int online,
    List<Player> sample) { }
