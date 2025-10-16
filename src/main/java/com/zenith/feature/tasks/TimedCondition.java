package com.zenith.feature.tasks;

import com.zenith.util.timer.Timer;
import com.zenith.util.timer.Timers;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.ApiStatus;

/**
 * A {@link Condition} that is met after a specified delay.
 */
@Data
@EqualsAndHashCode(exclude = "timer")
@ApiStatus.Experimental
public class TimedCondition implements Condition {
    private final Timer timer = Timers.timer();
    private final long delayMs;

    @Override
    public boolean isMet() {
        return timer.tick(delayMs);
    }
}
