package com.zenith.terminal;

import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;

import java.util.List;

import static com.zenith.Shared.COMMAND;

public class TerminalCommandCompleter implements Completer {
    @Override
    public void complete(final LineReader lineReader, final ParsedLine parsedLine, final List<Candidate> list) {
        final String line = parsedLine.line();
        COMMAND.getCommandCompletions(line).stream()
            .map(Candidate::new)
            .forEach(list::add);
    }
}
