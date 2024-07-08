package com.zenith;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.zenith.util.Wait;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;

import static com.zenith.Shared.DEFAULT_LOG;

public class ConnectConcurrencyTest {

//    @Test
    public void worstCaseScenario() {
        var executor = Executors.newThreadPerTaskExecutor(
            new ThreadFactoryBuilder()
                .setUncaughtExceptionHandler((t, e) -> DEFAULT_LOG.info("Thread {} error", t, e))
                .build());
        var mainExecutor = Executors.newSingleThreadExecutor();
        mainExecutor.execute(Proxy::main);
        Wait.wait(20);
        for (int i = 0; i < 64; i++) {
            executor.execute(() -> {
                Proxy.getInstance().connectAndCatchExceptions();
                Proxy.getInstance().disconnect();
            });
            Wait.wait(ThreadLocalRandom.current().nextInt(0, 5));
        }
    }
}
