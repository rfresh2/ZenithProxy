package com.zenith.feature.tasks;

import lombok.Data;
import org.jetbrains.annotations.ApiStatus;

import java.time.Instant;

/**
 * A {@link Condition} that is met after a specific {@link Instant} in time.
 */
@Data
@ApiStatus.Experimental
public class InstantCondition implements Condition {
    private final Instant time;

    @Override
    public boolean isMet() {
        return Instant.now().isAfter(time);
    }
}
