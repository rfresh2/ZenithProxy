package com.zenith.feature.tasks;

import lombok.Data;
import org.jetbrains.annotations.ApiStatus;

import java.io.Closeable;

/**
 * A task that consists of an {@link Action}, a {@link Condition}, and a {@link Continuation}.
 * The task executes the {@link Action} when the {@link Condition} is met.
 * The {@link Continuation} decides when the task should stop executing.
 */
@Data
@ApiStatus.Experimental
public class Task implements Closeable {
    private final String id;
    private final Action action;
    private final Condition condition;
    private final Continuation continuation;

    /**
     * @return true if the task should continue executing, false otherwise
     */
    public boolean tick() {
        boolean executed = false;
        if (condition.isMet()) {
            action.execute();
            executed = true;
        }
        return continuation.shouldContinue(executed);
    }

    @Override
    public void close() {
        action.close();
        condition.close();
        continuation.close();
    }
}
