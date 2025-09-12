package com.zenith.command.brigadier;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.RedirectModifier;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;

import java.util.Collection;
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
}
