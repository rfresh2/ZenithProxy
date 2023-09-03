package com.zenith.feature.pathing.blockdata;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zenith.cache.data.chunk.ChunkCache;
import com.zenith.feature.pathing.CollisionBox;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import lombok.Getter;
import lombok.Setter;

import java.util.*;
import java.util.stream.Collectors;

import static com.zenith.Shared.DEFAULT_LOG;
import static java.util.Objects.isNull;

@Getter
@Setter
public class BlockDataManager {
    private final ObjectMapper objectMapper;
    private Map<Integer, Block> blockWithCollisionMap;
    private int maxStates;
    private int blockBitsPerEntry;
    private List<String> stateIdToBlockName = new ArrayList<>(25000);

    public BlockDataManager() {
        this.objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        List<BlockData> blockData = getBlockData();
        initBlockPalette(blockData);
        BlockCollisionShapes blockCollisionShapes = getBlockCollisionShapes();
        initBlockWithCollisions(blockData, blockCollisionShapes);
    }

    public Optional<Block> getBlockFromId(int id) {
        if (this.blockWithCollisionMap.containsKey(id)) {
            return Optional.of(this.blockWithCollisionMap.get(id));
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


    private void initBlockWithCollisions(List<BlockData> blockDataList, BlockCollisionShapes blockCollisionShapes) {
        this.blockWithCollisionMap = blockDataList.stream()
                .map(blockData -> new Block(blockData.getId(), blockData.getDisplayName(), blockData.getName(), blockData.getBoundingBox(), getBlockVariationMapping(blockCollisionShapes, blockData)))
                .collect(Collectors.toMap(Block::getId, v -> v));
    }

    private void initBlockPalette(List<BlockData> blockDataList) {
        // todo: validate this works lol
        this.maxStates = 1 + blockDataList.stream()
            .mapToInt(blockData -> blockData.getMaxStateId())
            .max()
            .orElse(0);
        DEFAULT_LOG.info("Max states: {}", this.maxStates);
        for (int i = 0; i < this.maxStates; i++) {
            this.stateIdToBlockName.add(null);
        }
        this.blockBitsPerEntry = ChunkCache.log2RoundUp(this.maxStates);
        for (BlockData data : blockDataList) {
            Integer minStateId = data.getMinStateId();
            Integer maxStateId = data.getMaxStateId();
            if (minStateId != null && maxStateId != null) {
                for (int i = minStateId; i <= maxStateId; i++) {
                    this.stateIdToBlockName.set(i, data.getName());
                }
            }
        }
        DEFAULT_LOG.info("State ID to Block Name size: {}", this.stateIdToBlockName.size());
    }

    private Map<Integer, List<CollisionBox>> getBlockVariationMapping(BlockCollisionShapes blockCollisionShapes, BlockData blockData) {
        final Map<Integer, List<CollisionBox>> map = new Int2ObjectOpenHashMap<>();
        if (isNull(blockData.getVariations())) {
            map.put(0, getCollisionBoxesFromBlockState(blockCollisionShapes, blockData, 0).orElse(Collections.emptyList()));
        } else {
            blockData.getVariations().forEach(variation -> {
                map.put(variation.getMetadata(), getCollisionBoxesFromBlockState(blockCollisionShapes, blockData, variation.getMetadata()).orElse(Collections.emptyList()));
            });
        }
        return map;
    }

    private Optional<List<CollisionBox>> getCollisionBoxesFromBlockState(BlockCollisionShapes blockCollisionShapes, final BlockData block, final int stateId) {
        final Object shapeIds = blockCollisionShapes.getBlocks().getAdditionalProperties().get(block.getName());
        if (shapeIds instanceof Integer) {
            return Optional.of(getCollisionBoxFromShapeId(blockCollisionShapes, (Integer) shapeIds));
        } else if (shapeIds instanceof List) {
            final List<Integer> shapeIdList = (List<Integer>) shapeIds;
            final List<List<CollisionBox>> collisionList = shapeIdList.stream()
                    .map(shapeId -> getCollisionBoxFromShapeId(blockCollisionShapes, shapeId))
                    .collect(Collectors.toList());
            return Optional.of(collisionList.get(stateId));
        } else {
            return Optional.empty();
        }
    }

    private List<CollisionBox> getCollisionBoxFromShapeId(BlockCollisionShapes blockCollisionShapes, final Integer shapeId) {
        final List<List<Double>> shapeList = blockCollisionShapes.getShapes().getAdditionalProperties().get("" + shapeId);
        return shapeList.stream()
                .map(shapeSublist -> new CollisionBox(shapeSublist.get(0), shapeSublist.get(3), shapeSublist.get(1), shapeSublist.get(4), shapeSublist.get(2), shapeSublist.get(5)))
                .collect(Collectors.toList());
    }
}
