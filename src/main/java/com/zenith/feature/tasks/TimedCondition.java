package com.zenith.feature.tasks;

import com.zenith.util.timer.Timer;
import com.zenith.util.timer.Timers;
import lombok.Data;
import org.jetbrains.annotations.ApiStatus;

@Data
@ApiStatus.Experimental
public class TimedCondition implements Condition {
    private final Timer timer;
    private final long delayMs;

    public TimedCondition(long delayMs) {
        this.timer = Timers.timer();
        this.delayMs = delayMs;
    }

    @Override
    public boolean isMet() {
        return timer.tick(delayMs);
    }
}
