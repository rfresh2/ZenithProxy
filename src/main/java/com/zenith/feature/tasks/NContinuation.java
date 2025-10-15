package com.zenith.feature.tasks;

import lombok.Data;
import org.jetbrains.annotations.ApiStatus;

/**
 * A {@link Continuation} that allows a task to continue executing a specified number of times.
 */
@Data
@ApiStatus.Experimental
public class NContinuation implements Continuation {
    private final int n;
    private int count = 0;

    @Override
    public boolean shouldContinue(boolean taskExecuted) {
        if (taskExecuted) {
            count++;
        }
        return count < n;
    }
}
