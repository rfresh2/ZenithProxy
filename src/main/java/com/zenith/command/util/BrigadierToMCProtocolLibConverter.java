package com.zenith.command.util;

import com.github.steveice10.mc.protocol.data.game.command.CommandNode;
import com.github.steveice10.mc.protocol.data.game.command.CommandParser;
import com.github.steveice10.mc.protocol.data.game.command.CommandType;
import com.github.steveice10.mc.protocol.data.game.command.properties.*;
import com.google.common.collect.Queues;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.*;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import com.zenith.command.brigadier.CaseInsensitiveLiteralCommandNode;
import com.zenith.command.brigadier.CommandContext;
import com.zenith.command.brigadier.CustomStringArgumentType;
import com.zenith.command.brigadier.ToggleArgumentType;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import lombok.experimental.UtilityClass;

import java.util.List;
import java.util.OptionalInt;
import java.util.Queue;

/**
 * Converts between Zenith's native Brigadier commands and the MCProtocolLib intermediary packet format
 */
@UtilityClass
public class BrigadierToMCProtocolLibConverter {
    public CommandNode[] convertNodesToMCProtocolLibNodes(CommandDispatcher<CommandContext> dispatcher) {
        final RootCommandNode<CommandContext> rootCommandNode = dispatcher.getRoot();
        final Object2IntMap<com.mojang.brigadier.tree.CommandNode<CommandContext>> object2IntMap = enumerateNodes(rootCommandNode);
        final List<CommandNode> entries = createEntries(object2IntMap);
        // root index should always be 0
        return entries.toArray(new CommandNode[0]);
    }

    private CommandType convertCommandType(com.mojang.brigadier.tree.CommandNode node) {
        if (node instanceof RootCommandNode) {
            return CommandType.ROOT;
        } else if (node instanceof LiteralCommandNode) {
            return CommandType.LITERAL;
        } else if (node instanceof ArgumentCommandNode) {
            return CommandType.ARGUMENT;
        }
        throw new RuntimeException("No valid command type found for node: " + node.getName());
    }

    private Object2IntMap<com.mojang.brigadier.tree.CommandNode<CommandContext>> enumerateNodes(RootCommandNode<CommandContext> rootCommandNode) {
        Object2IntMap<com.mojang.brigadier.tree.CommandNode<CommandContext>> object2IntMap = new Object2IntOpenHashMap<>();
        Queue<com.mojang.brigadier.tree.CommandNode<CommandContext>> queue = Queues.newArrayDeque();
        queue.add(rootCommandNode);

        com.mojang.brigadier.tree.CommandNode<CommandContext> commandNode;
        while((commandNode = queue.poll()) != null) {
            if (!object2IntMap.containsKey(commandNode)) {
                object2IntMap.put(commandNode, object2IntMap.size());
                queue.addAll(commandNode.getChildren());
                if (commandNode.getRedirect() != null) {
                    queue.add(commandNode.getRedirect());
                }
            }
        }
        return object2IntMap;
    }

    private List<CommandNode> createEntries(final Object2IntMap<com.mojang.brigadier.tree.CommandNode<CommandContext>> nodes) {
        ObjectArrayList<CommandNode> objectArrayList = new ObjectArrayList<>(nodes.size());
        objectArrayList.size(nodes.size());
        for (var entry : nodes.object2IntEntrySet()) {
            objectArrayList.set(entry.getIntValue(), createEntry(entry.getKey(), nodes));
        }
        return objectArrayList;
    }

    private CommandNode createEntry(
        com.mojang.brigadier.tree.CommandNode<CommandContext> node, Object2IntMap<com.mojang.brigadier.tree.CommandNode<CommandContext>> nodes
    ) {
        var commandType = convertCommandType(node);
        var isExecutable = node.getCommand() != null;
        var childrenIndeces = node.getChildren().stream().mapToInt(nodes::getInt).toArray();
        final OptionalInt redirectIndex = node.getRedirect() == null ? OptionalInt.empty() : OptionalInt.of(nodes.getInt(node.getRedirect()));
        String name;
        if (node instanceof CaseInsensitiveLiteralCommandNode<CommandContext> ci) {
            name = ci.getLiteralOriginalCase();
        } else {
            name = node.getName();
        }
        CommandParser parser = null;
        CommandProperties properties = null;

        if (node instanceof ArgumentCommandNode<CommandContext,?> argumentNode) {
            ArgumentType<?> type = argumentNode.getType();
            if (type instanceof CustomStringArgumentType) {
                parser = CommandParser.STRING;
                properties = StringProperties.SINGLE_WORD; // doesn't match exactly to our char word definition but whatev
            } else if (type instanceof StringArgumentType sa) {
                parser = CommandParser.STRING;
                properties = switch (sa.getType()) {
                    case StringArgumentType.StringType.SINGLE_WORD -> StringProperties.SINGLE_WORD;
                    case StringArgumentType.StringType.GREEDY_PHRASE -> StringProperties.GREEDY_PHRASE;
                    case StringArgumentType.StringType.QUOTABLE_PHRASE -> StringProperties.QUOTABLE_PHRASE;
                };
            } else if (type instanceof ToggleArgumentType || type instanceof BoolArgumentType) {
                parser = CommandParser.BOOL;
            } else if (type instanceof DoubleArgumentType dt) {
                parser = CommandParser.DOUBLE;
                properties = new DoubleProperties(dt.getMinimum(), dt.getMaximum());
            } else if (type instanceof FloatArgumentType ft) {
                parser = CommandParser.FLOAT;
                properties = new FloatProperties(ft.getMinimum(), ft.getMaximum());
            } else if (type instanceof LongArgumentType lt) {
                parser = CommandParser.LONG;
                properties = new LongProperties(lt.getMinimum(), lt.getMaximum());
            } else if (type instanceof IntegerArgumentType it) {
                parser = CommandParser.INTEGER;
                properties = new IntegerProperties(it.getMinimum(), it.getMaximum());
            }
        }
        return new CommandNode(
            commandType,
            isExecutable,
            childrenIndeces,
            redirectIndex,
            name,
            parser,
            properties,
            null // means the client should never ask for suggestions from the server
        );
    }
}
