package com.zenith.feature.tasks;

import lombok.Data;
import org.jetbrains.annotations.ApiStatus;

@Data
@ApiStatus.Experimental
public class ContinuationN implements Continuation {
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
