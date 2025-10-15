package com.zenith.feature.tasks;

import com.github.rfresh2.EventConsumer;
import lombok.Data;
import org.jetbrains.annotations.ApiStatus;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.zenith.Globals.EVENT_BUS;

@Data
@ApiStatus.Experimental
public class EventCondition implements Condition, Closeable {
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
    public void close() throws IOException {
        EVENT_BUS.unsubscribe(this);
    }
}
