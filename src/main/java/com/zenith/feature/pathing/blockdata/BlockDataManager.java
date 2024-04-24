package com.zenith.feature.pathing.blockdata;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.DoubleNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zenith.feature.pathing.CollisionBox;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static com.zenith.Shared.OBJECT_MAPPER;

public class BlockDataManager {
    private final Int2IntOpenHashMap blockStateIdToBlockId = new Int2IntOpenHashMap(26644);
    private final Int2ObjectOpenHashMap<Block> blockIdToBlockData = new Int2ObjectOpenHashMap<>(1058);
    private final Int2ObjectOpenHashMap<List<CollisionBox>> blockStateIdToCollisionBoxes = new Int2ObjectOpenHashMap<>(26644);

    public BlockDataManager() {
        init();
    }

    private void init() {
        final Object2IntOpenHashMap<String> blockNameToId = new Object2IntOpenHashMap<>(1003);
        try (JsonParser blocksParser = OBJECT_MAPPER.createParser(getClass().getResourceAsStream("/mcdata/blocks.json"))) {
            TreeNode node = blocksParser.getCodec().readTree(blocksParser);
            for (Iterator<JsonNode> it = ((ArrayNode) node).elements(); it.hasNext(); ) {
                final var e = it.next();
                int blockId = e.get("id").asInt();
                String blockName = e.get("name").asText();
                blockNameToId.put(blockName, blockId);
                int minStateId = e.get("minStateId").asInt();
                int maxStateId = e.get("maxStateId").asInt();
                String boundingBoxType = e.get("boundingBox").asText();
                boolean isBlock = boundingBoxType.equals("block"); // empty otherwise
                for (int i = minStateId; i <= maxStateId; i++) {
                    blockStateIdToBlockId.put(i, blockId);
                }
                blockIdToBlockData.put(blockId, new Block(blockId, blockName, isBlock, minStateId, maxStateId));
            }
        } catch (final Exception e) {
            throw new RuntimeException(e);
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
                int blockId = blockNameToId.getInt(blockName);
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

                Block blockData = blockIdToBlockData.get(blockId);
                for (int i = blockData.minStateId(); i <= blockData.maxStateId(); i++) {
                    int nextShapeId = shapeIds.getInt(0);
                    if (shapeIds.size() > 1)
                        nextShapeId = shapeIds.getInt(i - blockData.minStateId());
                    List<CollisionBox> collisionBoxes = shapeIdToCollisionBoxes.get(nextShapeId);
                    blockStateIdToCollisionBoxes.put(i, collisionBoxes);
                }
            }
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    public @Nullable Block getBlockDataFromBlockStateId(int blockStateId) {
        Block blockData2 = blockIdToBlockData.get(blockStateIdToBlockId.get(blockStateId));
        if (blockData2 == blockIdToBlockData.defaultReturnValue()) return null;
        return blockData2;
    }

    public @Nullable List<CollisionBox> getCollisionBoxesFromBlockStateId(int blockStateId) {
        List<CollisionBox> collisionBoxes = blockStateIdToCollisionBoxes.get(blockStateId);
        if (collisionBoxes == blockStateIdToCollisionBoxes.defaultReturnValue()) return null;
        return collisionBoxes;
    }

    // not efficient, avoid calling outside initialization logic
    public Block getBlockFromName(final String name) {
        return blockIdToBlockData.values().stream()
            .filter(block -> block.name().equals(name))
            .findAny()
            .orElseThrow();
    }

    public float getBlockSlipperiness(Block block) {
        float slippy = 0.6f;
        if (block.name().equals("ice")) slippy = 0.98f;
        if (block.name().equals("slime_block")) slippy = 0.8f;
        if (block.name().equals("packed_ice")) slippy = 0.98f;
        if (block.name().equals("frosted_ice")) slippy = 0.98f;
        if (block.name().equals("blue_ice")) slippy = 0.989f;
        return slippy;
    }
}
