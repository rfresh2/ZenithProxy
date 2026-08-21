package com.zenith.terminal;

import com.zenith.Proxy;
import com.zenith.command.api.CommandContext;
import com.zenith.command.api.CommandOutputHelper;
import com.zenith.command.api.CommandSources;
import com.zenith.event.console.ConsoleLogEvent;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.TerminalBuilder;
import org.jline.terminal.impl.DumbTerminal;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static com.zenith.Globals.*;

public class TerminalManager {
    private @Nullable LineReader lineReader; // lazy init

    public void start() {
        EVENT_BUS.subscribe(this, ConsoleLogEvent.class, this::writeTerminalOutput);
        try {
            if (CONFIG.interactiveTerminal.enable) startInteractiveTerminal();
        } catch (Throwable t) {
            TERMINAL_LOG.error("Failed to start interactive terminal", t);
        }
    }

    public void stop() {
        EVENT_BUS.unsubscribe(this);
        var lr = lineReader;
        lineReader = null;
        if (lr != null) {
            try {
                lr.getTerminal().close();
            } catch (IOException e) {
                TERMINAL_LOG.error("Failed to close terminal", e);
            }
        }
    }

    private void startInteractiveTerminal() throws IOException {
        var terminal = TerminalBuilder.builder()
            .encoding(StandardCharsets.UTF_8)
            .stdoutEncoding(StandardCharsets.UTF_8)
            .stderrEncoding(StandardCharsets.UTF_8)
            .systemOutput(TerminalBuilder.SystemOutput.SysOut)
            .color(true)
            .build();
        lineReader = LineReaderBuilder.builder()
            .terminal(terminal)
            .appName("ZenithProxy")
            .option(LineReader.Option.DISABLE_EVENT_EXPANSION, true)
            .option(LineReader.Option.CASE_INSENSITIVE, true)
            .option(LineReader.Option.INSERT_TAB, false)
            .option(LineReader.Option.EMPTY_WORD_OPTIONS, false)
            .completer(new TerminalCommandCompleter())
            .build();
        if (CONFIG.interactiveTerminal.alwaysOnCompletions) {
            new TerminalAutoCompletionWidget(lineReader);
        }
        var terminalThread = new Thread(this::readTerminalInput, "ZenithProxy Terminal");
        terminalThread.setDaemon(true);
        terminalThread.start();
        if (terminal instanceof DumbTerminal) {
            TERMINAL_LOG.info("Initialized dumb terminal");
        } else {
            TERMINAL_LOG.info("Initialized interactive terminal");
        }
    }

    private void writeTerminalOutput(ConsoleLogEvent event) {
        if (lineReader == null) {
            var str = event.ansi();
            // default case if we did not initialize an interactive terminal
            if (str.endsWith("\n") || str.endsWith("\n\033[m") || str.endsWith("\n\033[0m")) {
                System.out.print(str);
            } else {
                System.out.println(str);
            }
        } else {
            lineReader.printAbove(event.ansi());
        }
    }

    private void readTerminalInput() {
        int eofCount = 0;
        while (true) {
            try {
                String line = lineReader.readLine("> ");
                if (line == null || line.isBlank()) {
                    continue;
                }
                executeTerminalCommand(line);
                eofCount = 0;
            } catch (final EndOfFileException e) {
                if (eofCount++ > 20) {
                    TERMINAL_LOG.warn("Detected misconfigured terminal input, disabling interactive terminal");
                    return;
                }
            } catch (final UserInterruptException e) {
                TERMINAL_LOG.info("Exiting...");
                EXECUTOR.execute(() -> {
                    Proxy.getInstance().stop(false);
                });
                break;
            } catch (final Exception e) {
                TERMINAL_LOG.error("Error while reading terminal input", e);
            }
        }
    }

    private void executeTerminalCommand(final String command) {
        final var commandContext = CommandContext.create(command, CommandSources.TERMINAL);
        COMMAND.execute(commandContext);
        if (CONFIG.interactiveTerminal.logToDiscord && !commandContext.isSensitiveInput()) CommandOutputHelper.logInputToDiscord(command, CommandSources.TERMINAL, commandContext);
        var embed = commandContext.getEmbed();
        if (CONFIG.interactiveTerminal.logToDiscord && DISCORD.isRunning() && !commandContext.isSensitiveInput()) {
            CommandOutputHelper.logEmbedOutputToDiscord(embed);
            CommandOutputHelper.logMultiLineOutputToDiscord(commandContext.getMultiLineOutput());
        } else {
            CommandOutputHelper.logEmbedOutputToTerminal(embed);
            CommandOutputHelper.logMultiLineOutputToTerminal(commandContext.getMultiLineOutput());
        }
    }
}
