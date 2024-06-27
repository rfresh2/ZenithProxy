package com.zenith.terminal.logback;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;

import java.util.function.BooleanSupplier;

import static com.zenith.Shared.CONFIG;

public class DebugLogConfigurationFilter extends Filter<ILoggingEvent> {

    private final BooleanSupplier configuration;

    public DebugLogConfigurationFilter() {
        this.configuration = () -> {
            try {
                return CONFIG.debug.debugLogs;
            } catch (final NullPointerException e) {
                return false; // can occur during initialization before config is loaded
            }
        };
    }

    @Override
    public FilterReply decide(final ILoggingEvent event) {
        return configuration.getAsBoolean() ? FilterReply.NEUTRAL : FilterReply.DENY;
    }
}
