package com.zenith.feature.tasks;

import org.jetbrains.annotations.ApiStatus;

@FunctionalInterface
@ApiStatus.Experimental
public interface Continuation {
    boolean shouldContinue(boolean taskExecuted);
}
