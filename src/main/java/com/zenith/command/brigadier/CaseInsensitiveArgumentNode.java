package com.zenith.command.brigadier;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.RedirectModifier;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

public class CaseInsensitiveArgumentNode<S, T> extends ArgumentCommandNode<S, T> {
    public CaseInsensitiveArgumentNode(final String name, final ArgumentType<T> type, final Command<S> command, final Predicate<S> requirement, final CommandNode<S> redirect, final RedirectModifier<S> modifier, final boolean forks, final SuggestionProvider<S> customSuggestions) {
        super(name, type, command, requirement, redirect, modifier, forks, customSuggestions);
    }

    @Override
    public Collection<? extends CommandNode<S>> getRelevantNodes(final StringReader input) {
        final StringReader stringReader = new StringReader(input.getString().toLowerCase());
        stringReader.setCursor(input.getCursor());
        return super.getRelevantNodes(stringReader);
    }

    @Override
    public CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) throws CommandSyntaxException {
        if (!builder.getRemaining().isEmpty()) {
            return super.listSuggestions(context, builder);
        }
        if (this.getCustomSuggestions() != null) {
            return getCustomSuggestions().getSuggestions(context, builder);
        }
        var builderCopy = new SuggestionsBuilder(builder.getInput(), builder.getInput(), builder.getStart());
        try {
            var typeSuggestions = this.getType().listSuggestions(context, builder).get();
            if (typeSuggestions != null && !typeSuggestions.isEmpty()) {
                return CompletableFuture.completedFuture(typeSuggestions);
            }
        } catch (Exception e) {
            // fall through to default suggestion
        }
        builderCopy.suggest("<" + this.getName() + ">");
        return builderCopy.buildFuture();
    }
}
