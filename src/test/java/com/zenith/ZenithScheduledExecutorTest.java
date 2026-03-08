package com.zenith;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.zenith.util.Wait;
import com.zenith.util.ZenithScheduledExecutor;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class ZenithScheduledExecutorTest {

    @Test
    public void testDefaultExceptionHandler() {
        AtomicBoolean defaultHandlerCalled = new AtomicBoolean(false);
        try (var executor = new ZenithScheduledExecutor(4, new ThreadFactoryBuilder()
            .setNameFormat("ZenithProxy Scheduled Executor - #%d")
            .setUncaughtExceptionHandler((thread, e) -> {
                defaultHandlerCalled.set(true);
            })
            .build())) {
            executor.execute(() -> {
                throw new RuntimeException("asdf");
            });
            assertTrue(Wait.waitUntil(defaultHandlerCalled::get, 3));
        }
    }
}
