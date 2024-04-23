package com.zenith.generator;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.zenith.DataGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.io.FileWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class BlockCollisionShapes implements Generator {
    @Override
    public void generate() {
        var blockRegistry = BuiltInRegistries.BLOCK;
        BlockShapesCache blockShapesCache = new BlockShapesCache();

        blockRegistry.forEach(blockShapesCache::processBlock);

        JsonObject resultObject = new JsonObject();

        resultObject.add("blocks", blockShapesCache.dumpBlockShapeIndices(blockRegistry));
        resultObject.add("shapes", blockShapesCache.dumpShapesObject());

        try (Writer out = new FileWriter(DataGenerator.outputFile("blockCollisionShapes.json"))) {
            DataGenerator.gson.toJson(resultObject, out);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        DataGenerator.LOG.info("Dumped blockCollisionShapes.json");
    }

    private static class BlockShapesCache {
        public LinkedHashMap<VoxelShape, Integer> uniqueBlockShapes = new LinkedHashMap<>();
        public LinkedHashMap<Block, List<Integer>> blockCollisionShapes = new LinkedHashMap<>();
        private int lastCollisionShapeId = 0;

        public void processBlock(Block block) {
            List<BlockState> blockStates = block.getStateDefinition().getPossibleStates();
            List<Integer> blockCollisionShapes = new ArrayList<>();

            for (BlockState blockState : blockStates) {
                VoxelShape blockShape = blockState.getCollisionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
                Integer blockShapeIndex = uniqueBlockShapes.get(blockShape);

                if (blockShapeIndex == null) {
                    blockShapeIndex = lastCollisionShapeId++;
                    uniqueBlockShapes.put(blockShape, blockShapeIndex);
                }
                blockCollisionShapes.add(blockShapeIndex);
            }

            this.blockCollisionShapes.put(block, blockCollisionShapes);
        }

        public JsonObject dumpBlockShapeIndices(Registry<Block> blockRegistry) {
            JsonObject resultObject = new JsonObject();

            for (var entry : blockCollisionShapes.entrySet()) {
                List<Integer> blockCollisions = entry.getValue();
                long distinctShapesCount = blockCollisions.stream().distinct().count();
                JsonElement blockCollision;
                if (distinctShapesCount == 1L) {
                    blockCollision = new JsonPrimitive(blockCollisions.get(0));
                } else {
                    blockCollision = new JsonArray();
                    for (int collisionId : blockCollisions) {
                        ((JsonArray) blockCollision).add(collisionId);
                    }
                }

                var registryKey = blockRegistry.getKey(entry.getKey());
                resultObject.add(registryKey.getPath(), blockCollision);
            }

            return resultObject;
        }

        public JsonObject dumpShapesObject() {
            JsonObject shapesObject = new JsonObject();

            for (var entry : uniqueBlockShapes.entrySet()) {
                JsonArray boxesArray = new JsonArray();
                entry.getKey().forAllBoxes((x1, y1, z1, x2, y2, z2) -> {
                    JsonArray oneBoxJsonArray = new JsonArray();

                    oneBoxJsonArray.add(x1);
                    oneBoxJsonArray.add(y1);
                    oneBoxJsonArray.add(z1);

                    oneBoxJsonArray.add(x2);
                    oneBoxJsonArray.add(y2);
                    oneBoxJsonArray.add(z2);

                    boxesArray.add(oneBoxJsonArray);
                });
                shapesObject.add(Integer.toString(entry.getValue()), boxesArray);
            }
            return shapesObject;
        }
    }
}
