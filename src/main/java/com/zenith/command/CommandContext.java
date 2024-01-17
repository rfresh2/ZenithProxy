package com.zenith.command;

import com.zenith.discord.Embed;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CommandContext {
    private final String input;
    private final CommandSource source;
    private final Embed embed;
    private final List<String> multiLineOutput;
    // don't log sensitive input like passwords to discord
    private boolean sensitiveInput = false;

    public CommandContext(String input, CommandSource source, Embed embed, List<String> multiLineOutput) {
        this.input = input;
        this.source = source;
        this.embed = embed;
        this.multiLineOutput = multiLineOutput;
    }

    public static CommandContext create(final String input, final CommandSource source) {
        return new CommandContext(input, source, new Embed(), new ArrayList<>());
    }
}
