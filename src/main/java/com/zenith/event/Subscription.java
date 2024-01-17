package com.zenith.event;

class Subscription {
    private final Runnable unsubscribeCallback;

    public Subscription(Runnable unsubscribeCallback) {
        this.unsubscribeCallback = unsubscribeCallback;
    }

    public void unsubscribe() {
        unsubscribeCallback.run();
    }
}
