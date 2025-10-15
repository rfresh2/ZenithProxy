package com.zenith.feature.tasks;

import org.jetbrains.annotations.ApiStatus;

import java.io.Closeable;

/**
 * Decides whether a task should continue executing. Checked each tick.
 */
@ApiStatus.Experimental
public interface Continuation extends Closeable {
    boolean shouldContinue(boolean taskExecuted);
    default void close() {}
}
