package com.zenith.event.proxy;

public class QueuePositionUpdateEvent {
    public final int position;

    public QueuePositionUpdateEvent(int position) {
        this.position = position;
    }
}
