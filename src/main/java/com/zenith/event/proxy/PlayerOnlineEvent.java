package com.zenith.event.proxy;

import java.time.Duration;
import java.util.Optional;

public record PlayerOnlineEvent(Optional<Duration> queueWait) {

    public PlayerOnlineEvent() {
        this(Optional.empty());
    }
}
