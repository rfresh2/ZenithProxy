package com.zenith.event.module;

import org.jetbrains.annotations.Nullable;

import java.io.File;

public record ReplayStoppedEvent(@Nullable File replayFile) { }
