package com.zenith.command;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.CommandNode;

import java.util.Map;

@FunctionalInterface
public interface CommandErrorHandler {
    void handle(Map<CommandNode<CommandContext>, CommandSyntaxException> exceptions, CommandContext context);
}
