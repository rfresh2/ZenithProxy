package com.zenith.util;

import net.raphimc.minecraftauth.util.logging.ILogger;

import static com.zenith.Shared.AUTH_LOG;

public class MCAuthLoggerBridge implements ILogger {
    @Override
    public void info(final String message) {
        AUTH_LOG.info(message);
    }

    @Override
    public void warn(final String message) {
        AUTH_LOG.warn(message);
    }

    @Override
    public void error(final String message) {
        AUTH_LOG.error(message);
    }
}
