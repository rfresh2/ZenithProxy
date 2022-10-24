package com.zenith.discord.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

@FunctionalInterface
public interface IExecutes<S> {
    void execute(CommandContext<S> context) throws CommandSyntaxException;
}
