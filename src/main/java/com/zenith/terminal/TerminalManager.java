package com.zenith.terminal;

import com.zenith.Proxy;
import com.zenith.command.CommandContext;
import com.zenith.command.CommandOutputHelper;
import com.zenith.command.CommandSource;
import com.zenith.terminal.logback.TerminalConsoleAppender;
import discord4j.core.spec.EmbedCreateSpec;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.reader.impl.completer.StringsCompleter;
import org.jline.terminal.Terminal;
import org.jline.terminal.impl.DumbTerminal;

import java.util.Optional;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.zenith.Shared.*;

public class TerminalManager {
    private LineReader lineReader;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private Optional<Future<?>> executorFuture = Optional.empty();

    public TerminalManager() {
    }

    public void start() {
        if (executorFuture.isEmpty() && isRunning.compareAndSet(false, true)) {
            Terminal terminal = TerminalConsoleAppender.getTerminal();
            if (terminal != null && !(terminal instanceof DumbTerminal)) {
                TERMINAL_LOG.info("Starting Interactive Terminal...");
                this.lineReader = LineReaderBuilder.builder()
                        .terminal(terminal)
                        .option(LineReader.Option.DISABLE_EVENT_EXPANSION, true)
                        .option(LineReader.Option.INSERT_TAB, false)
                        // todo: integrate brigadier arguments or suggestion system
                        .completer(new StringsCompleter(COMMAND_MANAGER.getCommands().stream()
                                .flatMap(command -> Stream.concat(Stream.of(command.commandUsage().getName()), command.commandUsage().getAliases().stream()))
                                .collect(Collectors.toList())))
                        .build();
                TerminalConsoleAppender.setReader(lineReader);
                executorFuture = Optional.of(SCHEDULED_EXECUTOR_SERVICE.submit(interactiveRunnable));
            } else {
                TERMINAL_LOG.warn("Unsupported Terminal. Interactive Terminal will not be started.");
            }
        }
    }

    public void stop() {
        if (executorFuture.isPresent()) {
            executorFuture.get().cancel(true);
            executorFuture = Optional.empty();
        }
        isRunning.set(false);
        TerminalConsoleAppender.setReader(null);
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
        CommandContext commandContext = CommandContext.create(command, CommandSource.TERMINAL);
        COMMAND_MANAGER.execute(commandContext);
        if (CONFIG.interactiveTerminal.logToDiscord && !commandContext.isSensitiveInput()) CommandOutputHelper.logInputToDiscord(command, CommandSource.TERMINAL);
        EmbedCreateSpec embed = commandContext.getEmbedBuilder().build();
        if (CONFIG.interactiveTerminal.logToDiscord && DISCORD_BOT.isRunning() && !commandContext.isSensitiveInput()) {
            CommandOutputHelper.logEmbedOutputToDiscord(embed);
            CommandOutputHelper.logMultiLineOutputToDiscord(commandContext);
        } else {
            CommandOutputHelper.logEmbedOutputToTerminal(embed);
            CommandOutputHelper.logMultiLineOutputToTerminal(commandContext);
        }
    }
}
