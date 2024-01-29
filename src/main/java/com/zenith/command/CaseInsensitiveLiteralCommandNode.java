package com.zenith.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.RedirectModifier;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.CommandContextBuilder;
import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import lombok.Getter;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

public class CaseInsensitiveLiteralCommandNode<S> extends LiteralCommandNode<S> {
    @Getter
    private final String literalOriginalCase;
    private final String literalLowercase;
    private final CommandErrorHandler errorHandler;
    private final CommandSuccessHandler successHandler;

    public CaseInsensitiveLiteralCommandNode(String literal,
                                             Command<S> command,
                                             Predicate<S> requirement,
                                             CommandNode<S> redirect,
                                             RedirectModifier<S> modifier,
                                             boolean forks,
                                             CommandErrorHandler errorHandler,
                                             CommandSuccessHandler successHandler) {
        super(literal.toLowerCase(), command, requirement, redirect, modifier, forks);
        this.literalOriginalCase = literal;
        this.literalLowercase = literal.toLowerCase();
        this.errorHandler = errorHandler;
        this.successHandler = successHandler;
    }

    public Optional<CommandErrorHandler> getErrorHandler() {
        return Optional.ofNullable(errorHandler);
    }

    public Optional<CommandSuccessHandler> getSuccessHandler() {
        return Optional.ofNullable(successHandler);
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

    @Override
    public CompletableFuture<Suggestions> listSuggestions(final CommandContext<S> context, final SuggestionsBuilder builder) {
        if (this.literalLowercase.startsWith(builder.getRemainingLowerCase())) {
            return builder.suggest(literalOriginalCase).buildFuture();
        } else {
            return Suggestions.empty();
        }
    }
}
