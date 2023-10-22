package com.zenith.event;

import lombok.Data;

@Data
public abstract class CancellableEvent {
    private boolean cancelled = false;
    public void cancel() {
        cancelled = true;
    }
}
