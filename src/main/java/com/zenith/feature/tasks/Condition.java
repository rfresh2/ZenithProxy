package com.zenith.feature.tasks;

import org.jetbrains.annotations.ApiStatus;

/**
 * A boolean condition that must be met for a task's action to be executed
 */
@FunctionalInterface
@ApiStatus.Experimental
public interface Condition {
    boolean isMet();
}
