package com.zenith.command;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import java.util.Arrays;
import java.util.Collection;

/**
 * Extra string argument types not included by default
 */
public class CustomStringArgumentType implements ArgumentType<String> {

    private final CustomStringArgumentType.StringType type;

    private CustomStringArgumentType(final CustomStringArgumentType.StringType type) {
        this.type = type;
    }

    public static CustomStringArgumentType wordWithChars() {
        return new CustomStringArgumentType(StringType.CHAR_WORD);
    }

    public static String getString(final CommandContext<?> context, final String name) {
        return context.getArgument(name, String.class);
    }

    public CustomStringArgumentType.StringType getType() {
        return type;
    }

    @Override
    public String parse(final StringReader reader) throws CommandSyntaxException {
        if (type == StringType.CHAR_WORD) {
            return readStringUntil(' ', reader);
        } else {
            return reader.readString();
        }
    }

    public String readStringUntil(char terminator, final StringReader reader) {
        final StringBuilder result = new StringBuilder();
        while (reader.canRead()) {
            final char c = reader.read();
            result.append(c);
            if (c == terminator) break;
        }
        return result.toString();
    }

    @Override
    public String toString() {
        return "string()";
    }

    @Override
    public Collection<String> getExamples() {
        return type.getExamples();
    }

    public enum StringType {
        CHAR_WORD("any character not a space");

        private final Collection<String> examples;

        StringType(final String... examples) {
            this.examples = Arrays.asList(examples);
        }

        public Collection<String> getExamples() {
            return examples;
        }
    }
}
