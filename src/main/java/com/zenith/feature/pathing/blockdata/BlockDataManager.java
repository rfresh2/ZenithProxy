package com.zenith.feature.pathing.blockdata;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zenith.cache.data.chunk.ChunkCache;
import com.zenith.feature.pathing.CollisionBox;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.zenith.Shared.DEFAULT_LOG;

@Getter
@Setter
public class BlockDataManager {
    private final ObjectMapper objectMapper;
    private int maxStates;
    private int blockBitsPerEntry;
    private Int2ObjectMap<Block> blockStateIdToBlock = new Int2ObjectOpenHashMap<>();

    public BlockDataManager() {
        this.objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        List<BlockData> blockDataList = getBlockData();
        BlockCollisionShapes blockCollisionShapes = getBlockCollisionShapes();
        for (BlockData data : blockDataList) {
            Integer minStateId = data.getMinStateId();
            Integer maxStateId = data.getMaxStateId();
            Block block = new Block(data.getId(), data.getDisplayName(), data.getName(), data.getBoundingBox(), getBlockStateCollisionBoxes(blockCollisionShapes, data, minStateId, maxStateId));
            for (int i = minStateId; i <= maxStateId; i++) {
                this.blockStateIdToBlock.put(i, block);
            }
        }
        this.maxStates = blockStateIdToBlock.size();
        this.blockBitsPerEntry = ChunkCache.log2RoundUp(this.maxStates);
    }

    public Optional<Block> getBlockFromBlockStateId(int blockStateId) {
        if (this.blockStateIdToBlock.containsKey(blockStateId)) {
            return Optional.of(this.blockStateIdToBlock.get(blockStateId));
        } else {
            return Optional.empty();
        }
    }

    private List<BlockData> getBlockData() {
        try {
            return objectMapper.readValue(getClass().getResourceAsStream("/pc/1.20/blocks.json"), new TypeReference<List<BlockData>>() {
            });
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    private BlockCollisionShapes getBlockCollisionShapes() {
        try {
            return objectMapper.readValue(getClass().getResourceAsStream("/pc/1.20/blockCollisionShapes.json"), BlockCollisionShapes.class);
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Int2ObjectMap<List<CollisionBox>> getBlockStateCollisionBoxes(BlockCollisionShapes blockCollisionShapes, BlockData blockData, final int minStateId, final int maxStateId) {
        final Int2ObjectMap<List<CollisionBox>> map = new Int2ObjectOpenHashMap<>();
        for (int i = 0; i <= maxStateId - minStateId; i++) {
            final int currentStateId = minStateId + i;
            getCollisionBoxesFromBlockState(blockCollisionShapes, blockData, i)
                .ifPresent(collisionBoxes -> map.put(currentStateId, collisionBoxes));
        }
        return map;
    }

    private Optional<List<CollisionBox>> getCollisionBoxesFromBlockState(BlockCollisionShapes blockCollisionShapes,
                                                                         final BlockData block,
                                                                         final int stateId // not actually block palette state id. index of min to max block state id
    ) {
        final Object shapeIds = blockCollisionShapes.getBlocks().getAdditionalProperties().get(block.getName());
        if (shapeIds instanceof Integer) {
            return Optional.of(getCollisionBoxFromShapeId(blockCollisionShapes, (Integer) shapeIds));
        } else if (shapeIds instanceof List) {
            final List<Integer> shapeIdList = (List<Integer>) shapeIds;
            final List<List<CollisionBox>> collisionList = shapeIdList.stream()
                    .map(shapeId -> getCollisionBoxFromShapeId(blockCollisionShapes, shapeId))
                    .toList();
            return Optional.of(collisionList.get(stateId));
        } else {
            DEFAULT_LOG.warn("Did not find collision box for block: {}", block.getName());
            return Optional.empty();
        }
    }

    private List<CollisionBox> getCollisionBoxFromShapeId(BlockCollisionShapes blockCollisionShapes, final Integer shapeId) {
        final List<List<Double>> shapeList = blockCollisionShapes.getShapes().getAdditionalProperties().get("" + shapeId);
        return shapeList.stream()
                .map(shapeSublist -> new CollisionBox(shapeSublist.get(0), shapeSublist.get(3), shapeSublist.get(1), shapeSublist.get(4), shapeSublist.get(2), shapeSublist.get(5)))
                .collect(Collectors.toList());
    }

    public float getBlockSlipperiness(Block block) {
        float slippy = 0.6f;
        if (block.getName().equals("ice")) slippy = 0.98f;
        if (block.getName().equals("slime_block")) slippy = 0.8f;
        if (block.getName().equals("packed_ice")) slippy = 0.98f;
        if (block.getName().equals("frosted_ice")) slippy = 0.98f;
        if (block.getName().equals("blue_ice")) slippy = 0.989f;
        return slippy;
    }
}
