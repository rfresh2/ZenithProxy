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

    public NContinuation(int n) {
        if (n <= 0) {
            throw new IllegalArgumentException("n must be greater than 0");
        }
        this.n = n;
    }

    public NContinuation(int n, int count) {
        this(n);
        this.count = count;
    }

    @Override
    public boolean shouldContinue(boolean taskExecuted) {
        if (taskExecuted) {
            count++;
        }
        return count < n;
    }
}
