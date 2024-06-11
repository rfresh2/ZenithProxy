package com.zenith.terminal.logback;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.status.ErrorStatus;
import org.jline.reader.LineReader;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.terminal.impl.DumbTerminal;

import javax.annotation.Nullable;
import java.io.IOException;

public class TerminalConsoleAppender extends ConsoleAppender<ILoggingEvent> {
    private static boolean initialized = false;
    @Nullable
    private static Terminal terminal;
    @Nullable
    private static LineReader reader;
    @Nullable
    public static synchronized Terminal getTerminal() {
        initializeTerminal();
        return terminal;
    }

    @Nullable
    public static synchronized LineReader getReader() {
        return reader;
    }

    public static synchronized void setReader(@Nullable LineReader newReader) {
        if (newReader != null && newReader.getTerminal() != terminal) {
            throw new IllegalArgumentException("Reader was not created with TerminalConsoleAppender.getTerminal()");
        } else {
            reader = newReader;
        }
    }

    public TerminalConsoleAppender() {
        super();
    }

    private static synchronized void initializeTerminal() {
        if (!initialized) {
            initialized = true;
            try {
                terminal = TerminalBuilder.builder()
                    .jansi(true)
                    .systemOutput(TerminalBuilder.SystemOutput.SysOut)
                    .color(true)
                    .build();

            } catch (IllegalStateException | IOException e) {
                System.out.println("Failed to initialize terminal. Falling back to standard output");
            }
        }
    }

    @Override
    protected void append(ILoggingEvent event) {
        if (terminal != null && !(terminal instanceof DumbTerminal)) {
            final String message = new String(this.encoder.encode(event));
            if (reader != null) {
                reader.printAbove(message);
            } else {
                terminal.writer().write(message);
                terminal.writer().flush();
            }
        } else {
            super.append(event);
        }
    }

    @Override
    public void stop() {
        if (initialized) {
            initialized = false;
            reader = null;
            if (terminal != null) {
                try {
                    terminal.close();
                } catch (IOException e) {
                    addStatus(new ErrorStatus("Failed to close terminal", this, e));
                }
            }
        }
        super.stop();
    }

}
