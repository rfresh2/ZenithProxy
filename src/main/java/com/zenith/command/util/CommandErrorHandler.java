package com.zenith.command.util;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.CommandNode;
import com.zenith.command.brigadier.CommandContext;

import java.util.Map;

@FunctionalInterface
public interface CommandErrorHandler {
    void handle(Map<CommandNode<CommandContext>, CommandSyntaxException> exceptions, CommandContext context);
}
