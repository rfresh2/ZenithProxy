package com.zenith.feature.queue;

public record QueueStatus(
        int prio,
        int regular,
        long epochSecond
) {
    public int total() {
        return prio + regular;
    }
}
