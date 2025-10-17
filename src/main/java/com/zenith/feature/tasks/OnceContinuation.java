package com.zenith.feature.tasks;

import lombok.Data;
import org.jetbrains.annotations.ApiStatus;

/**
 * A {@link Continuation} that allows a task to execute only once.
 */
@Data
@ApiStatus.Experimental
public class OnceContinuation implements Continuation {
    @Override
    public boolean shouldContinue(boolean taskExecuted) {
        return !taskExecuted;
    }
}
