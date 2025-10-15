package com.zenith.feature.tasks;

import lombok.Data;
import org.jetbrains.annotations.ApiStatus;

@Data
@ApiStatus.Experimental
public class ContinuationForever implements Continuation {
    @Override
    public boolean shouldContinue(boolean taskExecuted) {
        return true;
    }
}
