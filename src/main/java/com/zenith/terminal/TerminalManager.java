package com.zenith.terminal;

import com.zenith.Proxy;
import com.zenith.command.brigadier.CommandContext;
import com.zenith.command.brigadier.CommandSource;
import com.zenith.command.util.CommandOutputHelper;
import com.zenith.terminal.logback.TerminalConsoleAppender;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Terminal;
import org.jline.terminal.impl.DumbTerminal;

import java.util.concurrent.atomic.AtomicBoolean;

import static com.zenith.Shared.*;

public class TerminalManager {
    private LineReader lineReader;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    public void start() {
        if (isRunning.compareAndSet(false, true)) {
            Terminal terminal = TerminalConsoleAppender.getTerminal();
            if (terminal == null) {
                TERMINAL_LOG.warn("Unable to initialize interactive terminal");
                return;
            }
            if (terminal instanceof DumbTerminal && !CONFIG.interactiveTerminal.allowDumbTerminal) {
                TERMINAL_LOG.warn("Dumb terminal initialized but is disabled by config");
                return;
            }
            TERMINAL_LOG.info("Starting Interactive Terminal...");
            this.lineReader = LineReaderBuilder.builder()
                .terminal(terminal)
                .option(LineReader.Option.DISABLE_EVENT_EXPANSION, true)
                .option(LineReader.Option.CASE_INSENSITIVE, true)
                .option(LineReader.Option.INSERT_TAB, false)
                .completer(new TerminalCommandCompleter())
                .build();
            TerminalConsoleAppender.setReader(lineReader);
            var terminalThread = new Thread(interactiveRunnable, "ZenithProxy Terminal");
            terminalThread.setDaemon(true);
            terminal.handle(Terminal.Signal.INT, signal -> terminalThread.interrupt());
            terminalThread.start();
        }
    }

    private final Runnable interactiveRunnable = () -> {
        while (true) {
            try {
                String line;
                try {
                    line = lineReader.readLine("> ");
                } catch (final EndOfFileException e) {
                    continue;
                }
                if (line == null) {
                    break;
                }
                handleTerminalCommand(line);
            } catch (final UserInterruptException e) {
                // ignore. terminal is closing
                TERMINAL_LOG.info("Exiting...");
                Proxy.getInstance().stop();
                break;
            } catch (final Exception e) {
                TERMINAL_LOG.error("Error while reading terminal input", e);
            }
        }
    };

    private void handleTerminalCommand(final String command) {
        switch (command) {
            case "exit":
                TERMINAL_LOG.info("Exiting...");
                Proxy.getInstance().stop();
                break;
            default:
                executeDiscordCommand(command);
        }
    }

    private void executeDiscordCommand(final String command) {
        final var commandContext = CommandContext.create(command, CommandSource.TERMINAL);
        COMMAND.execute(commandContext);
        if (CONFIG.interactiveTerminal.logToDiscord && !commandContext.isSensitiveInput()) CommandOutputHelper.logInputToDiscord(command, CommandSource.TERMINAL);
        var embed = commandContext.getEmbed();
        if (CONFIG.interactiveTerminal.logToDiscord && DISCORD.isRunning() && !commandContext.isSensitiveInput()) {
            CommandOutputHelper.logEmbedOutputToDiscord(embed);
            CommandOutputHelper.logMultiLineOutputToDiscord(commandContext);
        } else {
            CommandOutputHelper.logEmbedOutputToTerminal(embed);
            CommandOutputHelper.logMultiLineOutputToTerminal(commandContext);
        }
    }
}
