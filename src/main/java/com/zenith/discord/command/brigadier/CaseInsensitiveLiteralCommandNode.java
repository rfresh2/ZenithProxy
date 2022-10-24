package com.zenith.discord.command.brigadier;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.RedirectModifier;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.context.CommandContextBuilder;
import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;

import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

public class CaseInsensitiveLiteralCommandNode<S> extends LiteralCommandNode<S> {
    private final Function<CommandContext, Void> errorHandler;

    public CaseInsensitiveLiteralCommandNode(String literal, Command<S> command, Predicate<S> requirement, CommandNode<S> redirect, RedirectModifier<S> modifier, boolean forks, Function<CommandContext, Void> errorHandler) {
        super(literal, command, requirement, redirect, modifier, forks);
        this.errorHandler = errorHandler;
    }

    public Optional<Function<CommandContext, Void>> getErrorHandler() {
        return Optional.ofNullable(errorHandler);
    }

    @Override
    public void parse(final StringReader reader, final CommandContextBuilder<S> contextBuilder) throws CommandSyntaxException {
        final int start = reader.getCursor();
        final int end = parse(reader);
        if (end > -1) {
            contextBuilder.withNode(this, StringRange.between(start, end));
            return;
        }

        throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.literalIncorrect().createWithContext(reader, getLiteral());
    }

    private int parse(final StringReader reader) {
        final int start = reader.getCursor();
        if (reader.canRead(getLiteral().length())) {
            final int end = start + getLiteral().length();
            if (reader.getString().substring(start, end).equalsIgnoreCase(getLiteral())) {
                reader.setCursor(end);
                if (!reader.canRead() || reader.peek() == ' ') {
                    return end;
                } else {
                    reader.setCursor(start);
                }
            }
        }
        return -1;
    }

    @Override
    public Collection<? extends CommandNode<S>> getRelevantNodes(final StringReader input) {
        final StringReader stringReader = new StringReader(input.getString().toLowerCase());
        stringReader.setCursor(input.getCursor());
        return super.getRelevantNodes(stringReader);
    }
}
