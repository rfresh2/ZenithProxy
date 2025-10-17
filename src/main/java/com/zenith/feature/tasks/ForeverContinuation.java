package com.zenith.feature.tasks;

import lombok.Data;
import org.jetbrains.annotations.ApiStatus;

/**
 * A {@link Continuation} that always returns true, meaning the task will continue indefinitely.
 */
@Data
@ApiStatus.Experimental
public class ForeverContinuation implements Continuation {
    @Override
    public boolean shouldContinue(boolean taskExecuted) {
        return true;
    }
}
