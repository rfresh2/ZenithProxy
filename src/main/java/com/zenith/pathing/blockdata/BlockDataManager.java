package com.zenith.pathing.blockdata;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zenith.pathing.CollisionBox;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;

public class BlockDataManager {
    private final ObjectMapper objectMapper;
    private Map<Integer, Block> blockWithCollisionMap;

    public BlockDataManager() {
        this.objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        initBlockWithCollisions(getBlockData(), getBlockCollisionShapes());
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
            return objectMapper.readValue(new File("data/pc/1.12/blocks.json"), new TypeReference<List<BlockData>>() {
            });
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    private BlockCollisionShapes getBlockCollisionShapes() {
        try {
            return objectMapper.readValue(new File("data/pc/1.12/blockCollisionShapes.json"), BlockCollisionShapes.class);
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }


    private void initBlockWithCollisions(List<BlockData> blockDataList, BlockCollisionShapes blockCollisionShapes) {
        this.blockWithCollisionMap = blockDataList.stream()
                .map(blockData -> new Block(blockData.getId(), blockData.getDisplayName(), blockData.getName(), blockData.getBoundingBox(), getBlockVariationMapping(blockCollisionShapes, blockData)))
                .collect(Collectors.toMap(Block::getId, v -> v));
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
