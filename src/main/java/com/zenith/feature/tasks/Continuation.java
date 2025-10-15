package com.zenith.feature.tasks;

import org.jetbrains.annotations.ApiStatus;

/**
 * Decides whether a task should continue executing. Checked each tick.
 */
@FunctionalInterface
@ApiStatus.Experimental
public interface Continuation {
    boolean shouldContinue(boolean taskExecuted);
}
