package com.zenith.feature.tasks;

import com.github.rfresh2.EventConsumer;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.ApiStatus;

import java.util.concurrent.atomic.AtomicBoolean;

import static com.zenith.Globals.EVENT_BUS;

/**
 * A condition that is met when a specific event is fired.
 */
@Data
@EqualsAndHashCode(exclude = "triggered")
@ApiStatus.Experimental
public class EventCondition implements Condition {
    private AtomicBoolean triggered = new AtomicBoolean(false);
    private final Class<?> event;

    public EventCondition(Class<?> event) {
        this.event = event;
        EVENT_BUS.subscribe(this, EventConsumer.of(event, ev -> trigger()));
    }

    public void trigger() {
        this.triggered.set(true);
    }

    @Override
    public boolean isMet() {
        return triggered.compareAndSet(true, false);
    }

    @Override
    public void close() {
        EVENT_BUS.unsubscribe(this);
    }
}
