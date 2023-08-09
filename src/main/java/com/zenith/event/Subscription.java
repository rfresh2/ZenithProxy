package com.zenith.event;

public class Subscription {
    private Runnable unsubscribeCallback;

    public Subscription(Runnable unsubscribeCallback) {
        this.unsubscribeCallback = unsubscribeCallback;
    }

    public void unsubscribe() {
        unsubscribeCallback.run();
    }
}
