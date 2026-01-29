package com.zenith.via;

import com.viaversion.viaversion.api.configuration.RateLimitConfig;
import com.viaversion.viaversion.configuration.AbstractViaConfig;

import java.io.File;
import java.util.concurrent.TimeUnit;
import java.util.logging.LogManager;

public class ZenithViaConfig extends AbstractViaConfig {
    public ZenithViaConfig(final File configFile) {
        super(configFile, LogManager.getLogManager().getLogger("ViaVersion"));
    }

    private static final RateLimitConfig DEFAULT_RATE_LIMIT_CONFIG = new RateLimitConfig(false, -1, "", -1, 3, TimeUnit.SECONDS.toNanos(6), "", "");

    @Override
    public RateLimitConfig getPacketTrackerConfig() {
        return DEFAULT_RATE_LIMIT_CONFIG;
    }

    public RateLimitConfig getPacketSizeTrackerConfig() {
        return DEFAULT_RATE_LIMIT_CONFIG;
    }
}
