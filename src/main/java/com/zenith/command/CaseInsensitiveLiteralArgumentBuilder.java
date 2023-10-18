package com.zenith.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;

import java.util.function.Function;
import java.util.function.Predicate;

public class CaseInsensitiveLiteralArgumentBuilder<S> extends LiteralArgumentBuilder<S> {
    private Function<CommandContext, Void> errorHandler;
    private Function<CommandContext, Void> successHandler;

    protected CaseInsensitiveLiteralArgumentBuilder(String literal) {
        super(literal);
    }

    public static <S> CaseInsensitiveLiteralArgumentBuilder<S> literal(final String name) {
        return new CaseInsensitiveLiteralArgumentBuilder<>(name);
    }

    public static <S> CaseInsensitiveLiteralArgumentBuilder<S> literal(final String name, Function<CommandContext, Void> errorHandler) {
        final CaseInsensitiveLiteralArgumentBuilder<S> builder = literal(name);
        return builder.withErrorHandler(errorHandler);
    }

    public CaseInsensitiveLiteralArgumentBuilder<S> withErrorHandler(Function<CommandContext, Void> errorHandler) {
        this.errorHandler = errorHandler;
        return this;
    }

    public CaseInsensitiveLiteralArgumentBuilder<S> withSuccesshandler(Function<CommandContext, Void> successHandler) {
        this.successHandler = successHandler;
        return this;
    }

    public CaseInsensitiveLiteralArgumentBuilder<S> requires(final Predicate<S> requirement) {
        super.requires(requirement);
        return this;
    }

    public LiteralArgumentBuilder<S> executes(final IExecutes<S> command) {
        return this.executes((context) -> {
            command.execute(context);
            return 1;
        });
    }

    @Override
    public LiteralCommandNode<S> build() {
        final LiteralCommandNode<S> result = new CaseInsensitiveLiteralCommandNode<>(getLiteral(),
                                                                                     getCommand(),
                                                                                     getRequirement(),
                                                                                     getRedirect(),
                                                                                     getRedirectModifier(),
                                                                                     isFork(),
                                                                                     errorHandler,
                                                                                     successHandler);

        for (final CommandNode<S> argument : getArguments()) {
            result.addChild(argument);
        }

        return result;
    }
}
