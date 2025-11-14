package com.zenith.command.brigadier;

import com.google.common.collect.Queues;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.*;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import com.zenith.command.api.CommandContext;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import org.geysermc.mcprotocollib.protocol.data.game.command.CommandNode;
import org.geysermc.mcprotocollib.protocol.data.game.command.CommandParser;
import org.geysermc.mcprotocollib.protocol.data.game.command.CommandType;
import org.geysermc.mcprotocollib.protocol.data.game.command.properties.*;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
import java.util.Queue;

import static com.zenith.Globals.CLIENT_LOG;
import static com.zenith.Globals.SERVER_LOG;

public class McplBrigadierConverter {
    private McplBrigadierConverter() {}

    public static CommandNode[] toMcpl(CommandDispatcher<CommandContext> dispatcher) {
        final RootCommandNode<CommandContext> rootCommandNode = dispatcher.getRoot();
        final Object2IntMap<com.mojang.brigadier.tree.CommandNode<CommandContext>> object2IntMap = enumerateNodes(rootCommandNode);
        final List<CommandNode> entries = createEntries(object2IntMap);
        // root index should always be 0
        return entries.toArray(new CommandNode[0]);
    }

    public static @Nullable CommandDispatcher<CommandContext> toBrigadier(CommandNode[] nodes) {
        var rootNode = nodes[0];
        if (rootNode.getType() != CommandType.ROOT) {
            CLIENT_LOG.warn("Failed to convert command nodes to brigadier: First command node is not a root node");
            return null;
        }
        try {
            return convertTopLevelMcplNodes(nodes, rootNode);
        } catch (Exception e) {
            CLIENT_LOG.warn("Failed to convert command nodes to brigadier", e);
            return null;
        }
    }

    /** To MCPL **/

    private static CommandType convertCommandType(com.mojang.brigadier.tree.CommandNode node) {
        return switch (node) {
            case RootCommandNode n -> CommandType.ROOT;
            case LiteralCommandNode n -> CommandType.LITERAL;
            case ArgumentCommandNode n -> CommandType.ARGUMENT;
            case null, default -> throw new RuntimeException("No valid command type found for node: " + (node == null ? "?" : node.getName()));
        };
    }

    private static Object2IntMap<com.mojang.brigadier.tree.CommandNode<CommandContext>> enumerateNodes(RootCommandNode<CommandContext> rootCommandNode) {
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

    private static List<CommandNode> createEntries(final Object2IntMap<com.mojang.brigadier.tree.CommandNode<CommandContext>> nodes) {
        ArrayList<CommandNode> nodeList = new ArrayList<>(nodes.size());
        for (int i = 0; i < nodes.size(); i++) nodeList.add(null);
        for (var entry : nodes.object2IntEntrySet()) {
            nodeList.set(entry.getIntValue(), createEntry(entry.getKey(), nodes));
        }
        return nodeList;
    }

    private static CommandNode createEntry(
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
        String suggestionType = null;
        if (node instanceof ArgumentCommandNode<CommandContext,?> argumentNode) {
            switch (argumentNode.getType()) {
                case SerializableArgumentType t -> {
                    parser = t.commandParser();
                    properties = t.commandProperties();
                    if (t.askServerForSuggestions()) {
                        suggestionType = "minecraft:ask_server";
                    }
                }
                case BoolArgumentType t -> {
                    parser = CommandParser.BOOL;
                }
                case DoubleArgumentType t -> {
                    parser = CommandParser.DOUBLE;
                    properties = new DoubleProperties(t.getMinimum(), t.getMaximum());
                }
                case FloatArgumentType t -> {
                    parser = CommandParser.FLOAT;
                    properties = new FloatProperties(t.getMinimum(), t.getMaximum());
                }
                case LongArgumentType t -> {
                    parser = CommandParser.LONG;
                    properties = new LongProperties(t.getMinimum(), t.getMaximum());
                }
                case IntegerArgumentType t -> {
                    parser = CommandParser.INTEGER;
                    properties = new IntegerProperties(t.getMinimum(), t.getMaximum());
                }
                case StringArgumentType t -> {
                    parser = CommandParser.STRING;
                    properties = switch (t.getType()) {
                        case StringArgumentType.StringType.SINGLE_WORD -> StringProperties.SINGLE_WORD;
                        case StringArgumentType.StringType.GREEDY_PHRASE -> StringProperties.GREEDY_PHRASE;
                        case StringArgumentType.StringType.QUOTABLE_PHRASE -> StringProperties.QUOTABLE_PHRASE;
                    };
                }
                default -> {
                    SERVER_LOG.error("Unable to serialize unknown command argument type: {} : {}", argumentNode.getType(), name);
                }
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
            suggestionType // if null, the client should never ask for suggestions from the server
        );
    }

    /** To Brigadier **/

    // todo: fix redirects

    private static @NotNull CommandDispatcher<CommandContext> convertTopLevelMcplNodes(final CommandNode[] nodes, final CommandNode rootNode) {
        var nodeMap = new Int2ObjectOpenHashMap<com.mojang.brigadier.tree.CommandNode>();
        var dispatcher = new CommandDispatcher<CommandContext>();
        nodeMap.put(0, dispatcher.getRoot());
        for (var childIndex : rootNode.getChildIndices()) {
            var childNode = nodes[childIndex];
            if (childNode.getType() != CommandType.LITERAL) {
                continue;
            }
            if (childNode.getRedirectIndex().isPresent()) {
                continue;
            }
            var child = LiteralArgumentBuilder.<CommandContext>literal(childNode.getName());
            for (var childChildIndex : childNode.getChildIndices()) {
                var childChildNode = convertChildMcplNode(nodes, nodeMap, childChildIndex);
                if (childChildNode != null) {
                    child.then(childChildNode);
                }
            }
            var build = dispatcher.register(child);
            nodeMap.put(childIndex, build);
        }
        for (var childIndex : rootNode.getChildIndices()) {
            var childNode = nodes[childIndex];
            if (childNode.getType() != CommandType.LITERAL) {
                continue;
            }
            if (childNode.getRedirectIndex().isPresent()) {
                var redirectNode = nodeMap.get(childNode.getRedirectIndex().getAsInt());
                var child = LiteralArgumentBuilder.<CommandContext>literal(childNode.getName());
                child.redirect(redirectNode);
                var build = dispatcher.register(child);
                nodeMap.put(childIndex, build);
            }
        }
        return dispatcher;
    }

    private static com.mojang.brigadier.tree.CommandNode<CommandContext> convertChildMcplNode(CommandNode[] nodes, Int2ObjectMap<com.mojang.brigadier.tree.CommandNode> nodeMap, int nodeIndex) {
        CommandNode node = nodes[nodeIndex];
        ArgumentBuilder<CommandContext, ?> argBuilder = switch (node.getType()) {
            case ROOT -> {
                // shouldn't get here
                CLIENT_LOG.debug("Unexpected ROOT node in non-top-level position: {}", node);
                yield null;
            }
            case LITERAL -> LiteralArgumentBuilder.literal(node.getName());
            case ARGUMENT -> {
                ArgumentType<?> argType = switch (node.getParser()) {
                    case BOOL -> BoolArgumentType.bool();
                    case FLOAT -> {
                        var props = (FloatProperties) node.getProperties();
                        yield FloatArgumentType.floatArg(props.getMin(), props.getMax());
                    }
                    case DOUBLE -> {
                        var props = (DoubleProperties) node.getProperties();
                        yield DoubleArgumentType.doubleArg(props.getMin(), props.getMax());
                    }
                    case INTEGER -> {
                        var props = (IntegerProperties) node.getProperties();
                        yield IntegerArgumentType.integer(props.getMin(), props.getMax());
                    }
                    case LONG -> {
                        var props = (LongProperties) node.getProperties();
                        yield LongArgumentType.longArg(props.getMin(), props.getMax());
                    }
                    case STRING -> {
                        var props = (StringProperties) node.getProperties();
                        yield switch (props) {
                            case SINGLE_WORD -> StringArgumentType.word();
                            case QUOTABLE_PHRASE -> StringArgumentType.string();
                            case GREEDY_PHRASE -> StringArgumentType.greedyString();
                        };
                    }
                    case BLOCK_POS -> BlockPosArgument.blockPos();
                    case VEC3 -> Vec3Argument.vec3();
                    case VEC2 -> Vec2Argument.vec2();
                    case BLOCK_STATE -> BlockArgument.block();
                    case ITEM_STACK -> ItemArgument.item();
                    case MESSAGE -> MessageArgument.message(); // todo: implement
                    case ROTATION -> RotationArgument.rotation();
                    case RESOURCE_KEY -> {
                        var props = (ResourceProperties) node.getProperties();
                        var key = props.getRegistryKey();
                        if ("entity_type".equals(key)) {
                            yield RegistryDataArgument.entity();
                        } else if ("enchantment_type".equals(key)) {
                            yield RegistryDataArgument.enchantment();
                        } else if ("worldgen/biome".equals(key)) {
                            yield RegistryDataArgument.biome();
                        } else {
                            // todo: not correct, but its a fine approximation for most arg types
                            yield CustomStringArgumentType.wordWithChars();
                        }
                    }
                    case RESOURCE_LOCATION -> ResourceLocationArgument.id();
                    case DIMENSION -> DimensionArgument.dimension();
                    case TIME -> TimeArgument.time();
                    default ->
                        // todo: not correct, but its a fine approximation for most arg types
                        CustomStringArgumentType.wordWithChars();
                };
                yield RequiredArgumentBuilder.argument(node.getName(), argType);
            }
        };
        if (argBuilder == null) {
            return null;
        }
        for (var childIndex : node.getChildIndices()) {
            var child = convertChildMcplNode(nodes, nodeMap, childIndex);
            if (child != null) {
                argBuilder.then(child);
            }
        }
        if (node.isExecutable()) {
            argBuilder.executes(context -> 0);
        }
        if (node.getRedirectIndex().isPresent()) {
            // todo: not sure if this works tbh
            //  requires the redirect node to already be built and in the map
            //  so ordering is very strict
            //  vanilla mc uses an intermediary stub tree to solve this
            var redirectNode = nodeMap.get(node.getRedirectIndex().getAsInt());
            if (redirectNode != null) {
                argBuilder.redirect(redirectNode);
            } else {
//                CLIENT_LOG.debug("Failed to find redirect node at index {} for node {}", node.getRedirectIndex().getAsInt(), node);
            }
        }
        var build = argBuilder.build();
        nodeMap.put(nodeIndex, build);

        return build;
    }
}
