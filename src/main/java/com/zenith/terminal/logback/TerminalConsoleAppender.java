package com.zenith.terminal.logback;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import com.zenith.event.console.ConsoleLogEvent;

import static com.zenith.Globals.EVENT_BUS;

public class TerminalConsoleAppender extends ConsoleAppender<ILoggingEvent> {
    private static boolean printStdout = true;
    public static void setPrintStdout(boolean printStdout) {
        TerminalConsoleAppender.printStdout = printStdout;
    }

    @Override
    protected void append(ILoggingEvent event) {
        var ansi = new String(this.encoder.encode(event));
        if (printStdout) {
            super.append(event);
        }
        EVENT_BUS.post(new ConsoleLogEvent(event, ansi));
    }
}
