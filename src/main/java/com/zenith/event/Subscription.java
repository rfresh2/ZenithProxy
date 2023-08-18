package com.zenith.event;

public class Subscription {
    private final Runnable unsubscribeCallback;

    public Subscription(Runnable unsubscribeCallback) {
        this.unsubscribeCallback = unsubscribeCallback;
    }

    public void unsubscribe() {
        unsubscribeCallback.run();
    }
}
