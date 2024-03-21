package com.zenith.command.brigadier;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.zenith.command.util.CommandErrorHandler;
import com.zenith.command.util.CommandSuccessHandler;
import com.zenith.command.util.IExecutes;

import java.util.function.Predicate;

public class CaseInsensitiveLiteralArgumentBuilder<S> extends LiteralArgumentBuilder<S> {
    private CommandErrorHandler errorHandler;
    private CommandSuccessHandler successHandler;

    protected CaseInsensitiveLiteralArgumentBuilder(String literal) {
        super(literal);
    }

    public static <S> CaseInsensitiveLiteralArgumentBuilder<S> literal(final String name) {
        return new CaseInsensitiveLiteralArgumentBuilder<>(name);
    }

    public static <S> CaseInsensitiveLiteralArgumentBuilder<S> literal(final String name, CommandErrorHandler errorHandler) {
        final CaseInsensitiveLiteralArgumentBuilder<S> builder = literal(name);
        return builder.withErrorHandler(errorHandler);
    }

    public CaseInsensitiveLiteralArgumentBuilder<S> withErrorHandler(CommandErrorHandler errorHandler) {
        this.errorHandler = errorHandler;
        return this;
    }

    public CaseInsensitiveLiteralArgumentBuilder<S> withSuccesshandler(CommandSuccessHandler successHandler) {
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
