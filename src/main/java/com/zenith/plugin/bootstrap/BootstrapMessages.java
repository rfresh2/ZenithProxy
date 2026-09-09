package com.zenith.plugin.bootstrap;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Bootstrap-side
 */
public final class BootstrapMessages {
    private static final Queue<Message> LOGS = new ConcurrentLinkedQueue<>();
    private static final Queue<Message> NOTIFICATIONS = new ConcurrentLinkedQueue<>();

    private BootstrapMessages() {}

    public static void report(String text, Throwable failure, boolean notifyDiscord) {
        var message = new Message(text, failure);
        LOGS.add(message);
        if (notifyDiscord) NOTIFICATIONS.add(message);
    }

    public static Message pollLog() {
        return LOGS.poll();
    }

    public static Message pollNotification() {
        return NOTIFICATIONS.poll();
    }

    public record Message(String text, Throwable failure) {}
}
