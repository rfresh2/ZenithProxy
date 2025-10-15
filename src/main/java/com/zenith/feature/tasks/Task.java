package com.zenith.feature.tasks;

import lombok.Data;
import org.jetbrains.annotations.ApiStatus;

@Data
@ApiStatus.Experimental
public class Task {
    private final String id;
    private final Action action;
    private final Condition condition;
    private final Continuation continuation;

    public boolean tick() {
        boolean executed = false;
        if (condition.isMet()) {
            action.execute();
            executed = true;
        }
        return continuation.shouldContinue(executed);
    }
}
