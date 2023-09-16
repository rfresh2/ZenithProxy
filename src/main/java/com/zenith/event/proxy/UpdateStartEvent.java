package com.zenith.event.proxy;

import java.util.Optional;

public record UpdateStartEvent(Optional<String> newVersion) { }
