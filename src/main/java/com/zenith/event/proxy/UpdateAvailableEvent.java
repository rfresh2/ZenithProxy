package com.zenith.event.proxy;

import java.util.Optional;

public record UpdateAvailableEvent(String version) {
    public Optional<String> getVersion() {
        return Optional.ofNullable(version);
    }
}
