package com.zenith.command.brigadier;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import java.util.concurrent.CompletableFuture;

public class ToggleArgumentType implements ArgumentType<Boolean> {

    private static final SimpleCommandExceptionType READER_EXPECTED_ON_OFF = new SimpleCommandExceptionType(new LiteralMessage("Expected on/off"));
    private static final DynamicCommandExceptionType READER_INVALID_ON_OFF = new DynamicCommandExceptionType(value -> new LiteralMessage("Invalid on/off, found '" + value + "'"));

    public static ToggleArgumentType toggle() {
        return new ToggleArgumentType();
    }

    public static boolean getToggle(final CommandContext<?> context, final String name) {
        return context.getArgument(name, Boolean.class);
    }

    @Override
    public Boolean parse(final StringReader reader) throws CommandSyntaxException {
        final int start = reader.getCursor();
        final String value = reader.readString();
        if (value.isEmpty()) {
            throw READER_EXPECTED_ON_OFF.createWithContext(reader);
        }

        if (value.equalsIgnoreCase("on") || value.equalsIgnoreCase("true")) {
            return true;
        } else if (value.equalsIgnoreCase("off") || value.equalsIgnoreCase("false")) {
            return false;
        } else {
            reader.setCursor(start);
            throw READER_INVALID_ON_OFF.createWithContext(reader, value);
        }
    }

    @Override
    public CompletableFuture<Suggestions> listSuggestions(final CommandContext context, final SuggestionsBuilder builder) {
        if ("on".startsWith(builder.getRemainingLowerCase())) {
            builder.suggest("on");
        }
        if ("off".startsWith(builder.getRemainingLowerCase())) {
            builder.suggest("off");
        }
        return builder.buildFuture();
    }
}
