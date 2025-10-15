package com.zenith.feature.tasks;

import lombok.Data;
import org.jetbrains.annotations.ApiStatus;

@Data
@ApiStatus.Experimental
public class DurationContinuation implements Continuation {
    private final long endTimeEpochMs;

    @Override
    public boolean shouldContinue(final boolean taskExecuted) {
        return System.currentTimeMillis() < endTimeEpochMs;
    }
}
