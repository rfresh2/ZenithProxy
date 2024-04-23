package com.zenith.generator;

import com.google.common.base.CaseFormat;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.zenith.DataGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.locale.Language;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.io.FileWriter;
import java.io.Writer;
import java.util.Collection;
import java.util.List;

public class Blocks implements Generator {
    @Override
    public void generate() {
        JsonArray resultBlocksArray = new JsonArray();
        var blockRegistry = BuiltInRegistries.BLOCK;
//        List<MaterialsDataGenerator.MaterialInfo> availableMaterials = MaterialsDataGenerator.getGlobalMaterialInfo();

        blockRegistry.forEach(block -> resultBlocksArray.add(generateBlock(blockRegistry, block)));
        try (Writer out = new FileWriter(DataGenerator.outputFile("blocks.json"))) {
            DataGenerator.gson.toJson(resultBlocksArray, out);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        DataGenerator.LOG.info("Dumped blocks.json");
    }

    public JsonObject generateBlock(Registry<Block> registry, Block block) {
        JsonObject blockDesc = new JsonObject();
        List<BlockState> blockStates = block.getStateDefinition().getPossibleStates();
        BlockState defaultState = block.defaultBlockState();
        var registryKey = registry.getKey(block);
        Item blockItem = block.asItem();
        String localizationKey = blockItem.getDescriptionId();

        blockDesc.addProperty("id", registry.getId(block));
        blockDesc.addProperty("name", registryKey.getPath());
        blockDesc.addProperty("displayName", Language.getInstance().getOrDefault(localizationKey));

        blockDesc.addProperty("hardness", block.defaultDestroyTime());
        blockDesc.addProperty("resistance", block.getExplosionResistance());
        blockDesc.addProperty("stackSize", blockItem.getMaxStackSize());
        blockDesc.addProperty("diggable", block.defaultDestroyTime() != -1.0f);
        // todo: add material
//        blockDesc.addProperty("material", findMatchingBlockMaterial(defaultState, materials));
//        blockDesc.addProperty("transparent", !defaultState.canOcclude());
//        blockDesc.addProperty("emitLight", defaultState.getLightEmission());
//        blockDesc.addProperty("filterLight", defaultState.getOpacity(EmptyBlockView.INSTANCE, BlockPos.ORIGIN));

        blockDesc.addProperty("defaultState", Block.getId(defaultState));
        blockDesc.addProperty("minStateId", Block.getId(blockStates.getFirst()));
        blockDesc.addProperty("maxStateId", Block.getId(blockStates.getLast()));

        JsonArray stateProperties = new JsonArray();

        blockStates.stream()
            .flatMap(s -> s.getProperties().stream())
            .distinct()
            .forEach(properties -> stateProperties.add(generateStateProperty(properties)));
        blockDesc.add("states", stateProperties);

//        List<Item> effectiveTools = getItemsEffectiveForBlock(defaultState);

        //Only add harvest tools if tool is required for harvesting this block
//        if (defaultState.isToolRequired()) {
//            JsonObject effectiveToolsObject = new JsonObject();
//            for (Item effectiveItem : effectiveTools) {
//                effectiveToolsObject.addProperty(Integer.toString(Item.getRawId(effectiveItem)), true);
//            }
//            blockDesc.add("harvestTools", effectiveToolsObject);
//        }

//        List<ItemStack> actualBlockDrops = new ArrayList<>();
//        populateDropsIfPossible(defaultState, effectiveTools.isEmpty() ? Items.AIR : effectiveTools.get(0), actualBlockDrops);
//
//        JsonArray dropsArray = new JsonArray();
//        for (ItemStack dropStack : actualBlockDrops) {
//            dropsArray.add(Item.getRawId(dropStack.getItem()));
//        }
//        blockDesc.add("drops", dropsArray);

        VoxelShape blockCollisionShape = defaultState.getCollisionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
        blockDesc.addProperty("boundingBox", blockCollisionShape.isEmpty() ? "empty" : "block");

        return blockDesc;
    }

    private <T extends Comparable<T>> JsonObject generateStateProperty(Property<T> property) {
        JsonObject propertyObject = new JsonObject();
        Collection<T> propertyValues = property.getPossibleValues();

        propertyObject.addProperty("name", property.getName());
        propertyObject.addProperty("type", getPropertyTypeName(property));
        propertyObject.addProperty("num_values", propertyValues.size());

        //Do not add values for vanilla boolean properties, they are known by default
        if (!(property instanceof BooleanProperty)) {
            JsonArray propertyValuesArray = new JsonArray();
            for (T propertyValue : propertyValues) {
                propertyValuesArray.add(property.getName(propertyValue));
            }
            propertyObject.add("values", propertyValuesArray);
        }
        return propertyObject;
    }

    private static String getPropertyTypeName(Property<?> property) {
        //Explicitly handle default minecraft properties
        if (property instanceof BooleanProperty) {
            return "bool";
        }
        if (property instanceof IntegerProperty) {
            return "int";
        }
        if (property instanceof EnumProperty) {
            return "enum";
        }

        //Use simple class name as fallback, this code will give something like
        //example_type for ExampleTypeProperty class name
        String rawPropertyName = property.getClass().getSimpleName().replace("Property", "");
        return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, rawPropertyName);
    }
}
