package com.zenith.mc.block;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.DoubleNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zenith.util.Maps;
import com.zenith.util.math.MathHelper;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import org.geysermc.mcprotocollib.protocol.data.game.chunk.DataPalette;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static com.zenith.Shared.OBJECT_MAPPER;

public class BlockDataManager {
    private final Int2ObjectOpenHashMap<Block> blockStateIdToBlock;
    private final Int2ObjectOpenHashMap<List<CollisionBox>> blockStateIdToCollisionBoxes;
    private final IntOpenHashSet waterloggedStateIds = new IntOpenHashSet(9182 + 1, Maps.MAX_LOAD_FACTOR);

    public BlockDataManager() {
        int blockStateIdCount = BlockRegistry.REGISTRY.getIdMap().int2ObjectEntrySet().stream()
            .map(Map.Entry::getValue)
            .map(Block::maxStateId)
            .max(Integer::compareTo)
            .orElseThrow() + 1;
        blockStateIdToBlock = new Int2ObjectOpenHashMap<>(blockStateIdCount, Maps.MAX_LOAD_FACTOR);
        blockStateIdToCollisionBoxes = new Int2ObjectOpenHashMap<>(blockStateIdCount, Maps.MAX_LOAD_FACTOR);
        init();
    }

    private void init() {
        for (Int2ObjectMap.Entry<Block> entry : BlockRegistry.REGISTRY.getIdMap().int2ObjectEntrySet()) {
            var block = entry.getValue();
            for (int i = block.minStateId(); i <= block.maxStateId(); i++) {
                blockStateIdToBlock.put(i, block);
            }
        }
        try (JsonParser shapesParser = OBJECT_MAPPER.createParser(getClass().getResourceAsStream(
            "/mcdata/blockCollisionShapes.json"))) {
            final Int2ObjectOpenHashMap<List<CollisionBox>> shapeIdToCollisionBoxes = new Int2ObjectOpenHashMap<>(100);
            TreeNode node = shapesParser.getCodec().readTree(shapesParser);
            ObjectNode shapesNode = (ObjectNode) node.get("shapes");
            for (Iterator<String> it = shapesNode.fieldNames(); it.hasNext(); ) {
                String shapeIdName = it.next();
                int shapeId = Integer.parseInt(shapeIdName);
                final List<CollisionBox> collisionBoxes = new ArrayList<>(2);
                ArrayNode outerCbArray = (ArrayNode) shapesNode.get(shapeIdName);
                for (Iterator<JsonNode> it2 = outerCbArray.elements(); it2.hasNext(); ) {
                    ArrayNode innerCbArray = (ArrayNode) it2.next();
                    double[] cbArr = new double[6];
                    int i = 0;
                    for (Iterator<JsonNode> it3 = innerCbArray.elements(); it3.hasNext(); ) {
                        DoubleNode doubleNode = (DoubleNode) it3.next();
                        cbArr[i++] = doubleNode.asDouble();
                    }
                    collisionBoxes.add(new CollisionBox(cbArr[0], cbArr[3], cbArr[1], cbArr[4], cbArr[2], cbArr[5]));
                }
                shapeIdToCollisionBoxes.put(shapeId, collisionBoxes);
            }

            ObjectNode blocksNode = (ObjectNode) node.get("blocks");
            for (Iterator<String> it = blocksNode.fieldNames(); it.hasNext(); ) {
                String blockName = it.next();
                int blockId = Integer.parseInt(blockName);
                JsonNode shapeNode = blocksNode.get(blockName);
                final IntArrayList shapeIds = new IntArrayList(2);
                if (shapeNode.isInt()) {
                    int shapeId = shapeNode.asInt();
                    shapeIds.add(shapeId);
                } else if (shapeNode.isArray()) {
                    ArrayNode shapeIdArray = (ArrayNode) shapeNode;
                    for (Iterator<JsonNode> it2 = shapeIdArray.elements(); it2.hasNext(); ) {
                        int shapeId = it2.next().asInt();
                        shapeIds.add(shapeId);
                    }
                } else throw new RuntimeException("Unexpected shape node type: " + shapeNode.getNodeType());

                Block blockData = BlockRegistry.REGISTRY.get(blockId);
                for (int i = blockData.minStateId(); i <= blockData.maxStateId(); i++) {
                    int nextShapeId = shapeIds.getInt(0);
                    if (shapeIds.size() > 1)
                        nextShapeId = shapeIds.getInt(i - blockData.minStateId());
                    List<CollisionBox> collisionBoxes = shapeIdToCollisionBoxes.get(nextShapeId);
                    blockStateIdToCollisionBoxes.put(i, collisionBoxes);
                }
            }
            try (JsonParser waterloggedParser = OBJECT_MAPPER.createParser(getClass().getResourceAsStream(
                "/mcdata/waterloggedBlockStateIds.json"))) {
                TreeNode waterloggedNode = waterloggedParser.getCodec().readTree(waterloggedParser);
                ArrayNode waterloggedArray = (ArrayNode) waterloggedNode;
                waterloggedArray.elements().forEachRemaining((stateId) -> {
                    waterloggedStateIds.add(stateId.asInt());
                });
            }
            DataPalette.GLOBAL_PALETTE_BITS_PER_ENTRY = MathHelper.log2Ceil(blockStateIdToBlock.size());
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    public @Nullable Block getBlockDataFromBlockStateId(int blockStateId) {
        Block blockData = blockStateIdToBlock.get(blockStateId);
        if (blockData == blockStateIdToBlock.defaultReturnValue()) return null;
        return blockData;
    }

    public @Nullable List<CollisionBox> getCollisionBoxesFromBlockStateId(int blockStateId) {
        List<CollisionBox> collisionBoxes = blockStateIdToCollisionBoxes.get(blockStateId);
        if (collisionBoxes == blockStateIdToCollisionBoxes.defaultReturnValue()) return null;
        return collisionBoxes;
    }

    public boolean isWaterLogged(int blockStateId) {
        return waterloggedStateIds.contains(blockStateId);
    }

    public float getBlockSlipperiness(Block block) {
        float slippy = 0.6f;
        if (block == BlockRegistry.ICE) slippy = 0.98f;
        if (block == BlockRegistry.SLIME_BLOCK) slippy = 0.8f;
        if (block == BlockRegistry.PACKED_ICE) slippy = 0.98f;
        if (block == BlockRegistry.FROSTED_ICE) slippy = 0.98f;
        if (block == BlockRegistry.BLUE_ICE) slippy = 0.989f;
        return slippy;
    }
}
