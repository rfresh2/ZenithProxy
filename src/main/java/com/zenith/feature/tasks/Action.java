package com.zenith.feature.tasks;

import org.jetbrains.annotations.ApiStatus;

/**
 * A task's action to be executed once a {@link Condition} is met
 */
@FunctionalInterface
@ApiStatus.Experimental
public interface Action {
    void execute();
}
