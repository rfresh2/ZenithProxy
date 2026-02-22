package com.zenith.mc.block;

import com.zenith.util.struct.Maps;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import lombok.SneakyThrows;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.zenith.mc.MCGlobals.OBJECT_MAPPER;

public class BlockDataManager {
    static final int blockStateIdCount = BlockRegistry.REGISTRY.getIdMap().int2ObjectEntrySet().stream()
        .map(Map.Entry::getValue)
        .map(Block::maxStateId)
        .max(Integer::compareTo)
        .orElseThrow();
    private static final Int2ObjectOpenHashMap<Block> blockStateIdToBlock = new Int2ObjectOpenHashMap<>(blockStateIdCount, Maps.FAST_LOAD_FACTOR);;
    private static final Int2ObjectOpenHashMap<List<CollisionBox>> blockStateIdToCollisionBoxes = new Int2ObjectOpenHashMap<>(blockStateIdCount, Maps.FAST_LOAD_FACTOR);
    private static final Int2ObjectOpenHashMap<List<CollisionBox>> blockStateIdToInteractionBoxes = new Int2ObjectOpenHashMap<>(blockStateIdCount, Maps.FAST_LOAD_FACTOR);
    private static final Int2ObjectOpenHashMap<FluidState> blockStateIdToFluidState = new Int2ObjectOpenHashMap<>(100, Maps.FAST_LOAD_FACTOR);
    private static final IntOpenHashSet pathfindableStateIds = new IntOpenHashSet();
    static {
        init();
    }

    @SneakyThrows
    private static void init() {
        for (Int2ObjectMap.Entry<Block> entry : BlockRegistry.REGISTRY.getIdMap().int2ObjectEntrySet()) {
            var block = entry.getValue();
            for (int i = block.minStateId(); i <= block.maxStateId(); i++) {
                blockStateIdToBlock.put(i, block);
            }
        }
        initShapeCache("blockCollisionShapes", blockStateIdToCollisionBoxes);
        initShapeCache("blockInteractionShapes", blockStateIdToInteractionBoxes);
        var fluidStatesNode = (ObjectNode) OBJECT_MAPPER.readTree(BlockDataManager.class.getResourceAsStream("/mcdata/fluidStates.smile"));
        for (var stateIdString : fluidStatesNode.propertyNames()) {
            int stateId = Integer.parseInt(stateIdString);
            ObjectNode fluidStateNode = (ObjectNode) fluidStatesNode.get(stateIdString);
            boolean water = fluidStateNode.get("water").asBoolean();
            boolean source = fluidStateNode.get("source").asBoolean();
            int amount = fluidStateNode.get("amount").asInt();
            boolean falling = fluidStateNode.get("falling").asBoolean();
            blockStateIdToFluidState.put(stateId, new FluidState(water, source, amount, falling));
        }
        var pathfindableArray = (ArrayNode) OBJECT_MAPPER.readTree(BlockDataManager.class.getResourceAsStream("/mcdata/pathfindable.smile"));
        pathfindableArray.elements().forEach((stateId) -> {
            pathfindableStateIds.add(stateId.asInt());
        });
    }

    @SneakyThrows
    private static void initShapeCache(String name, Int2ObjectOpenHashMap<List<CollisionBox>> output) {
        final Int2ObjectOpenHashMap<List<CollisionBox>> shapeIdToCollisionBoxes = new Int2ObjectOpenHashMap<>(100);
        final Int2ObjectOpenHashMap<CollisionBox> boxIdToBox = new Int2ObjectOpenHashMap<>(100);
        var node = (ObjectNode) OBJECT_MAPPER.readTree(BlockDataManager.class.getResourceAsStream("/mcdata/" + name + ".smile"));
        ObjectNode boxesNode = (ObjectNode) node.get("boxes");
        for (var boxIdName : boxesNode.propertyNames()) {
            int boxId = Integer.parseInt(boxIdName);
            ArrayNode cbArray = (ArrayNode) boxesNode.get(boxIdName);
            double[] cbArr = new double[6];
            int i = 0;
            for (var doubleNode : cbArray.elements()) {
                cbArr[i++] = doubleNode.asDouble();
            }
            CollisionBox collisionBox = new CollisionBox(cbArr[0], cbArr[1], cbArr[2], cbArr[3], cbArr[4], cbArr[5]);
            boxIdToBox.put(boxId, collisionBox);
        }

        ObjectNode shapesNode = (ObjectNode) node.get("shapes");
        for (var shapeIdName : shapesNode.propertyNames()) {
            int shapeId = Integer.parseInt(shapeIdName);
            ArrayNode outerCbArray = (ArrayNode) shapesNode.get(shapeIdName);
            List<CollisionBox> collisionBoxes;
            if (outerCbArray.isEmpty()) {
                collisionBoxes = Collections.emptyList();
            } else {
                collisionBoxes = new ArrayList<>(outerCbArray.size());
                for (var boxIdNode : outerCbArray.elements()) {
                    CollisionBox box = boxIdToBox.get(boxIdNode.asInt());
                    collisionBoxes.add(box);
                }
            }
            shapeIdToCollisionBoxes.put(shapeId, collisionBoxes);
        }

        ObjectNode blocksNode = (ObjectNode) node.get("blocks");
        for (var blockName : blocksNode.propertyNames()) {
            int blockId = Integer.parseInt(blockName);
            JsonNode shapeNode = blocksNode.get(blockName);
            final IntArrayList shapeIds = new IntArrayList(2);
            if (shapeNode.isInt()) {
                int shapeId = shapeNode.asInt();
                shapeIds.add(shapeId);
            } else if (shapeNode.isArray()) {
                ArrayNode shapeIdArray = (ArrayNode) shapeNode;
                for (var shadeIdNode : shapeIdArray.elements()) {
                    int shapeId = shadeIdNode.asInt();
                    shapeIds.add(shapeId);
                }
            } else throw new RuntimeException("Unexpected shape node type: " + shapeNode.getNodeType());

            Block blockData = BlockRegistry.REGISTRY.get(blockId);
            for (int i = blockData.minStateId(); i <= blockData.maxStateId(); i++) {
                int nextShapeId = shapeIds.getInt(0);
                if (shapeIds.size() > 1)
                    nextShapeId = shapeIds.getInt(i - blockData.minStateId());
                List<CollisionBox> collisionBoxes = shapeIdToCollisionBoxes.get(nextShapeId);
                output.put(i, collisionBoxes);
            }
        }
    }

    public @Nullable Block getBlockDataFromBlockStateId(int blockStateId) {
        Block blockData = blockStateIdToBlock.get(blockStateId);
        if (blockData == blockStateIdToBlock.defaultReturnValue()) return null;
        return blockData;
    }

    public List<CollisionBox> getCollisionBoxesFromBlockStateId(int blockStateId) {
        List<CollisionBox> collisionBoxes = blockStateIdToCollisionBoxes.get(blockStateId);
        if (collisionBoxes == blockStateIdToCollisionBoxes.defaultReturnValue()) return Collections.emptyList();
        return collisionBoxes;
    }

    public List<CollisionBox> getInteractionBoxesFromBlockStateId(int blockStateId) {
        List<CollisionBox> collisionBoxes = blockStateIdToInteractionBoxes.get(blockStateId);
        if (collisionBoxes == blockStateIdToInteractionBoxes.defaultReturnValue()) return Collections.emptyList();
        return collisionBoxes;
    }

    public List<LocalizedCollisionBox> localizeCollisionBoxes(List<CollisionBox> collisionBoxes, Block block, int x, int y, int z) {
        var offsetVec = block.offsetType().getOffsetFunction().offset(block, x, y, z);
        final List<LocalizedCollisionBox> localizedCollisionBoxes = new ArrayList<>(collisionBoxes.size());
        for (int i = 0; i < collisionBoxes.size(); i++) {
            var collisionBox = collisionBoxes.get(i);
            localizedCollisionBoxes.add(new LocalizedCollisionBox(
                collisionBox.minX() + offsetVec.getX() + x,
                collisionBox.maxX() + offsetVec.getX() + x,
                collisionBox.minY() + offsetVec.getY() + y,
                collisionBox.maxY() + offsetVec.getY() + y,
                collisionBox.minZ() + offsetVec.getZ() + z,
                collisionBox.maxZ() + offsetVec.getZ() + z,
                x, y, z
            ));
        }
        return localizedCollisionBoxes;
    }

    @Nullable
    public FluidState getFluidState(int blockStateId) {
        return blockStateIdToFluidState.get(blockStateId);
    }

    public boolean isPathfindable(int blockStateId) {
        return pathfindableStateIds.contains(blockStateId);
    }

    /**
     * @deprecated Use {@link Block#replaceable()} instead
     */
    @Deprecated
    public boolean isReplaceable(int blockStateId) {
        return getBlockDataFromBlockStateId(blockStateId).replaceable();
    }

    /**
     * @deprecated Use {@link Block#isAir()} instead
     */
    @Deprecated
    public boolean isAir(Block block) {
        return block.isAir();
    }

    public boolean isShapeFullBlock(int blockStateId) {
        List<CollisionBox> collisionBoxes = getCollisionBoxesFromBlockStateId(blockStateId);
        if (collisionBoxes.size() != 1) {
            return false;
        }
        var cb = collisionBoxes.getFirst();
        return cb.isFullBlock();
    }

    public int blockStateRegistrySize() {
        return blockStateIdToBlock.size();
    }
}
