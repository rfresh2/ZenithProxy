package com.zenith.feature.tasks;

import org.jetbrains.annotations.ApiStatus;

import java.io.Closeable;

/**
 * A boolean condition that must be met for a task's action to be executed
 */
@ApiStatus.Experimental
public interface Condition extends Closeable {
    boolean isMet();
    default void close() {}
}
