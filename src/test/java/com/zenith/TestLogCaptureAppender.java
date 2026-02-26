package com.zenith;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;

public class TestLogCaptureAppender extends AppenderBase<ILoggingEvent> {
    @Override
    protected void append(final ILoggingEvent eventObject) {
        TestLogCapture.append(eventObject);
    }
}
