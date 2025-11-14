package com.zenith.terminal;

import com.mojang.brigadier.suggestion.Suggestion;
import com.zenith.command.api.CommandSources;
import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.zenith.Globals.COMMAND;
import static com.zenith.Globals.TERMINAL_LOG;

public class TerminalCommandCompleter implements Completer {
    @Override
    public void complete(final LineReader lineReader, final ParsedLine parsedLine, final List<Candidate> list) {
        final String line = parsedLine.line();
        try {
            var candidates = COMMAND.suggestions(line, CommandSources.TERMINAL)
                .thenApply(suggestions -> suggestions.getList().stream()
                    .map(Suggestion::getText)
                    .map(Candidate::new)
                    .limit(99)
                    .toList())
                .get(2, TimeUnit.SECONDS);
            list.addAll(candidates);
        } catch (Exception e) {
            TERMINAL_LOG.warn("Timed out getting command suggestions: {}", line);
        }
    }
}
