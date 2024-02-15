package com.zenith.network.server.handler.player.outgoing;

import com.github.steveice10.mc.protocol.data.game.command.CommandNode;
import com.github.steveice10.mc.protocol.data.game.command.CommandType;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundCommandsPacket;
import com.zenith.network.registry.PacketHandler;
import com.zenith.network.server.ServerConnection;

import java.util.OptionalInt;

import static com.zenith.Shared.*;

public class ClientCommandsOutgoingHandler implements PacketHandler<ClientboundCommandsPacket, ServerConnection> {
    @Override
    public ClientboundCommandsPacket apply(final ClientboundCommandsPacket packet, final ServerConnection session) {
        if (CONFIG.inGameCommands.enable && CONFIG.inGameCommands.slashCommands) {
            CommandNode[] zenithCommandNodes = COMMAND_MANAGER.getMCProtocolLibCommandNodesSupplier().get();
            if (CONFIG.inGameCommands.slashCommandsReplacesServerCommands) {
                return new ClientboundCommandsPacket(
                    zenithCommandNodes,
                    0
                );
            }
            if (packet.getFirstNodeIndex() != 0) {
                SERVER_LOG.warn("Unexpected root index on server command nodes: {}", packet.getFirstNodeIndex());
                SERVER_LOG.warn("Skipping nodes combination.");
                return new ClientboundCommandsPacket(
                    zenithCommandNodes,
                    0
                );
            }
            return new ClientboundCommandsPacket(
                combineCommandNodes(zenithCommandNodes, packet.getNodes()),
                0
            );
        }
        return packet;
    }

    // todo: check what happens if there is a duplicate command node name
    private static CommandNode[] combineCommandNodes(
        final CommandNode[] nodesA, // these nodes are essentially left as-is, except for the root node
        final CommandNode[] nodesB // these nodes are added to the end of the nodesA array, with indices offset
    ) {
        final CommandNode[] combinedNodes = new CommandNode[nodesA.length + nodesB.length - 1];
        final int offset = nodesA.length - 1;
        System.arraycopy(nodesA, 0, combinedNodes, 0, nodesA.length);
        combinedNodes[0] = combineRootCommandNodes(offset, nodesA[0], nodesB[0]);
        int nextIndex = nodesA.length;
        for (int i = 1; i < nodesB.length; i++) {
            combinedNodes[nextIndex++] = offsetCommandNode(offset, nodesB[i]);
        }
        return combinedNodes;
    }

    private static CommandNode combineRootCommandNodes(
        final int offset,
        final CommandNode rootNodeA,
        final CommandNode rootNodeB
    ) {
        final int[] combinedRootChildIndices = new int[rootNodeA.getChildIndices().length + rootNodeB.getChildIndices().length];
        System.arraycopy(rootNodeA.getChildIndices(), 0, combinedRootChildIndices, 0, rootNodeA.getChildIndices().length);
        System.arraycopy(rootNodeB.getChildIndices(), 0, combinedRootChildIndices, rootNodeA.getChildIndices().length, rootNodeB.getChildIndices().length);
        for (int i = rootNodeA.getChildIndices().length; i < combinedRootChildIndices.length; i++) {
            combinedRootChildIndices[i] += offset;
        }
        return new CommandNode(
            CommandType.ROOT,
            rootNodeA.isExecutable(),
            combinedRootChildIndices,
            rootNodeA.getRedirectIndex(),
            rootNodeA.getName(),
            rootNodeA.getParser(),
            rootNodeA.getProperties(),
            rootNodeA.getSuggestionType()
        );
    }

    private static CommandNode offsetCommandNode(final int offsetIndex, final CommandNode node) {
        final int[] childIndices = new int[node.getChildIndices().length];
        for (int j = 0; j < childIndices.length; j++) {
            childIndices[j] = node.getChildIndices()[j] + offsetIndex;
        }
        OptionalInt redirectIndex = node.getRedirectIndex().isPresent()
            ? OptionalInt.of(node.getRedirectIndex().getAsInt() + offsetIndex)
            : OptionalInt.empty();
        return new CommandNode(
            node.getType(),
            node.isExecutable(),
            childIndices,
            redirectIndex,
            node.getName(),
            node.getParser(),
            node.getProperties(),
            node.getSuggestionType()
        );
    }
}
