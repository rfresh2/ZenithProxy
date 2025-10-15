package com.zenith.feature.tasks;

import org.jetbrains.annotations.ApiStatus;

import java.io.Closeable;

/**
 * A task's action to be executed once a {@link Condition} is met
 */
@ApiStatus.Experimental
public interface Action extends Closeable {
    void execute();
    default void close() {}
}
