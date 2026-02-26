package com.zenith;

import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class TestLogCaptureJunitExtension implements BeforeEachCallback, TestWatcher {
    private static final ConcurrentMap<String, Long> CHECKPOINTS = new ConcurrentHashMap<>();

    @Override
    public void beforeEach(final ExtensionContext context) {
        CHECKPOINTS.put(context.getUniqueId(), TestLogCapture.checkpoint());
    }

    @Override
    public void testFailed(final ExtensionContext context, final Throwable cause) {
        var checkpoint = CHECKPOINTS.remove(context.getUniqueId());
        if (checkpoint == null)
            return;

        var testName = context.getDisplayName();
        var logs = TestLogCapture.dumpSince(checkpoint);
        System.err.println("\n=== test failed: " + testName + " ===");
        System.err.println(logs);
        System.err.println("===\n");
    }

    @Override
    public void testSuccessful(final ExtensionContext context) {
        CHECKPOINTS.remove(context.getUniqueId());
    }

    @Override
    public void testAborted(final ExtensionContext context, final Throwable cause) {
        CHECKPOINTS.remove(context.getUniqueId());
    }
}
