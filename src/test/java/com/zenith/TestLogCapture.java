package com.zenith;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;

public final class TestLogCapture {
    private static final int MAX_ENTRIES = 20_000;
    private static final DateTimeFormatter TS_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneOffset.UTC);
    private static final AtomicLong SEQUENCE = new AtomicLong();
    private static final ConcurrentLinkedDeque<Entry> ENTRIES = new ConcurrentLinkedDeque<>();
    private static final Object TRIM_LOCK = new Object();

    private TestLogCapture() {}

    public static long checkpoint() {
        return SEQUENCE.get();
    }

    public static String dumpSince(final long checkpoint) {
        var builder = new StringBuilder();
        for (var entry : ENTRIES) {
            if (entry.sequence <= checkpoint) continue;
            builder.append(entry.text);
        }
        return builder.isEmpty() ? "<no Zenith logs captured>" : builder.toString();
    }

    static void append(final ILoggingEvent event) {
        var sequence = SEQUENCE.incrementAndGet();
        var text = format(event);
        ENTRIES.addLast(new Entry(sequence, text));

        if (ENTRIES.size() <= MAX_ENTRIES) return;
        synchronized (TRIM_LOCK) {
            while (ENTRIES.size() > MAX_ENTRIES) {
                ENTRIES.pollFirst();
            }
        }
    }

    private static String format(final ILoggingEvent event) {
        var timestamp = TS_FORMATTER.format(Instant.ofEpochMilli(event.getTimeStamp()));
        var builder = new StringBuilder(256)
            .append("[")
            .append(timestamp)
            .append("] [")
            .append(event.getLoggerName())
            .append("] [")
            .append(event.getLevel())
            .append("] [")
            .append(event.getThreadName())
            .append("] ")
            .append(event.getFormattedMessage())
            .append('\n');
        var throwableProxy = event.getThrowableProxy();
        if (throwableProxy != null) {
            builder.append(ThrowableProxyUtil.asString(throwableProxy));
        }
        return builder.toString();
    }

    private record Entry(long sequence, String text) {}
}
