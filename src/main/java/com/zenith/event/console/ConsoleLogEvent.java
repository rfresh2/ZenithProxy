package com.zenith.event.console;

import ch.qos.logback.classic.spi.ILoggingEvent;
import lombok.Data;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.regex.Pattern;

/**
 * Console output as seen in the ZenithProxy interactive terminal
 *
 * Can be listened to and redirected to also output to other destinations
 *
 * Avoid slow blocking in event consumers, runs on logback's appender thread
 */
@Data
@Accessors(fluent = true)
public class ConsoleLogEvent {
    private static final Pattern ANSI_PATTERN = Pattern.compile("\\e\\[[\\d;]*[^\\d;]");
    private final ILoggingEvent logbackEvent;
    /**
     * ANSI encoded log output
     */
    private final String ansi;

    /**
     * Plain log output without ANSI codes
     */
    @Getter(lazy = true)
    private final String plain = ansiStrip();

    private String ansiStrip() {
        return ANSI_PATTERN.matcher(ansi).replaceAll("");
    }
}
