package com.zenith.command;

import discord4j.core.spec.EmbedCreateSpec;

import java.util.ArrayList;
import java.util.List;

public class CommandContext {
    private final String input;
    private final CommandSource source;
    private final EmbedCreateSpec.Builder embedBuilder;
    private final List<String> multiLineOutput;

    public CommandContext(String input, CommandSource source, EmbedCreateSpec.Builder embedBuilder, List<String> multiLineOutput) {
        this.input = input;
        this.source = source;
        this.embedBuilder = embedBuilder;
        this.multiLineOutput = multiLineOutput;
    }
    public static CommandContext create(final String input, final CommandSource source) {
        return new CommandContext(input, source, EmbedCreateSpec.builder(), new ArrayList<>());
    }

    public String getInput() {
        return input;
    }
    public CommandSource getCommandSource() {
        return source;
    }
    public EmbedCreateSpec.Builder getEmbedBuilder() {
        return embedBuilder;
    }
    public List<String> getMultiLineOutput() {
        return multiLineOutput;
    }
}
