package com.zenith.feature.pathing.blockdata;

import com.fasterxml.jackson.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "id",
    "displayName",
    "name",
    "hardness",
    "stackSize",
    "diggable",
    "boundingBox",
    "material",
    "harvestTools",
    "variations",
    "states",
    "drops",
    "transparent",
    "emitLight",
    "filterLight",
    "minStateId",
    "maxStateId",
    "defaultState",
    "resistance"
})
public class BlockData {

    /**
     * The unique identifier for a block
     * (Required)
     *
     */
    @JsonProperty("id")
    @JsonPropertyDescription("The unique identifier for a block")
    private Integer id;
    /**
     * The display name of a block
     * (Required)
     *
     */
    @JsonProperty("displayName")
    @JsonPropertyDescription("The display name of a block")
    private String displayName;
    /**
     * The name of a block
     * (Required)
     *
     */
    @JsonProperty("name")
    @JsonPropertyDescription("The name of a block")
    private String name;
    /**
     * Hardness of a block
     * (Required)
     *
     */
    @JsonProperty("hardness")
    @JsonPropertyDescription("Hardness of a block")
    private Double hardness;
    /**
     * Stack size for a block
     * (Required)
     *
     */
    @JsonProperty("stackSize")
    @JsonPropertyDescription("Stack size for a block")
    private Integer stackSize;
    /**
     * true if a block is diggable
     * (Required)
     *
     */
    @JsonProperty("diggable")
    @JsonPropertyDescription("true if a block is diggable")
    private Boolean diggable;
    /**
     * BoundingBox of a block
     * (Required)
     *
     */
    @JsonProperty("boundingBox")
    @JsonPropertyDescription("BoundingBox of a block")
    private BoundingBox boundingBox;
    /**
     * Material of a block
     *
     */
    @JsonProperty("material")
    @JsonPropertyDescription("Material of a block")
    private String material;

    @JsonProperty("variations")
    private List<Variation> variations;
    @JsonProperty("states")
    private List<State> states;
    /**
     *
     * (Required)
     *
     */
    @JsonProperty("drops")
    private List<Object> drops;
    /**
     * true if a block is transparent
     * (Required)
     *
     */
    @JsonProperty("transparent")
    @JsonPropertyDescription("true if a block is transparent")
    private Boolean transparent;
    /**
     * Light emitted by that block
     * (Required)
     *
     */
    @JsonProperty("emitLight")
    @JsonPropertyDescription("Light emitted by that block")
    private Integer emitLight;
    /**
     * Light filtered by that block
     * (Required)
     *
     */
    @JsonProperty("filterLight")
    @JsonPropertyDescription("Light filtered by that block")
    private Integer filterLight;
    /**
     * Minimum state id
     *
     */
    @JsonProperty("minStateId")
    @JsonPropertyDescription("Minimum state id")
    private Integer minStateId;
    /**
     * Maximum state id
     *
     */
    @JsonProperty("maxStateId")
    @JsonPropertyDescription("Maximum state id")
    private Integer maxStateId;
    /**
     * Default state id
     *
     */
    @JsonProperty("defaultState")
    @JsonPropertyDescription("Default state id")
    private Integer defaultState;
    /**
     * Blast resistance
     *
     */
    @JsonProperty("resistance")
    @JsonPropertyDescription("Blast resistance")
    private Double resistance;

    /**
     * The unique identifier for a block
     * (Required)
     *
     */
    @JsonProperty("id")
    public Integer getId() {
        return id;
    }

    /**
     * The unique identifier for a block
     * (Required)
     *
     */
    @JsonProperty("id")
    public void setId(Integer id) {
        this.id = id;
    }

    /**
     * The display name of a block
     * (Required)
     *
     */
    @JsonProperty("displayName")
    public String getDisplayName() {
        return displayName;
    }

    /**
     * The display name of a block
     * (Required)
     *
     */
    @JsonProperty("displayName")
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    /**
     * The name of a block
     * (Required)
     *
     */
    @JsonProperty("name")
    public String getName() {
        return name;
    }

    /**
     * The name of a block
     * (Required)
     *
     */
    @JsonProperty("name")
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Hardness of a block
     * (Required)
     *
     */
    @JsonProperty("hardness")
    public Double getHardness() {
        return hardness;
    }

    /**
     * Hardness of a block
     * (Required)
     *
     */
    @JsonProperty("hardness")
    public void setHardness(Double hardness) {
        this.hardness = hardness;
    }

    /**
     * Stack size for a block
     * (Required)
     *
     */
    @JsonProperty("stackSize")
    public Integer getStackSize() {
        return stackSize;
    }

    /**
     * Stack size for a block
     * (Required)
     *
     */
    @JsonProperty("stackSize")
    public void setStackSize(Integer stackSize) {
        this.stackSize = stackSize;
    }

    /**
     * true if a block is diggable
     * (Required)
     *
     */
    @JsonProperty("diggable")
    public Boolean getDiggable() {
        return diggable;
    }

    /**
     * true if a block is diggable
     * (Required)
     *
     */
    @JsonProperty("diggable")
    public void setDiggable(Boolean diggable) {
        this.diggable = diggable;
    }

    /**
     * BoundingBox of a block
     * (Required)
     *
     */
    @JsonProperty("boundingBox")
    public BoundingBox getBoundingBox() {
        return boundingBox;
    }

    /**
     * BoundingBox of a block
     * (Required)
     *
     */
    @JsonProperty("boundingBox")
    public void setBoundingBox(BoundingBox boundingBox) {
        this.boundingBox = boundingBox;
    }

    /**
     * Material of a block
     *
     */
    @JsonProperty("material")
    public String getMaterial() {
        return material;
    }

    /**
     * Material of a block
     *
     */
    @JsonProperty("material")
    public void setMaterial(String material) {
        this.material = material;
    }

    @JsonProperty("variations")
    public List<Variation> getVariations() {
        return variations;
    }

    @JsonProperty("variations")
    public void setVariations(List<Variation> variations) {
        this.variations = variations;
    }

    @JsonProperty("states")
    public List<State> getStates() {
        return states;
    }

    @JsonProperty("states")
    public void setStates(List<State> states) {
        this.states = states;
    }

    /**
     *
     * (Required)
     *
     */
    @JsonProperty("drops")
    public List<Object> getDrops() {
        return drops;
    }

    /**
     *
     * (Required)
     *
     */
    @JsonProperty("drops")
    public void setDrops(List<Object> drops) {
        this.drops = drops;
    }

    /**
     * true if a block is transparent
     * (Required)
     *
     */
    @JsonProperty("transparent")
    public Boolean getTransparent() {
        return transparent;
    }

    /**
     * true if a block is transparent
     * (Required)
     *
     */
    @JsonProperty("transparent")
    public void setTransparent(Boolean transparent) {
        this.transparent = transparent;
    }

    /**
     * Light emitted by that block
     * (Required)
     *
     */
    @JsonProperty("emitLight")
    public Integer getEmitLight() {
        return emitLight;
    }

    /**
     * Light emitted by that block
     * (Required)
     *
     */
    @JsonProperty("emitLight")
    public void setEmitLight(Integer emitLight) {
        this.emitLight = emitLight;
    }

    /**
     * Light filtered by that block
     * (Required)
     *
     */
    @JsonProperty("filterLight")
    public Integer getFilterLight() {
        return filterLight;
    }

    /**
     * Light filtered by that block
     * (Required)
     *
     */
    @JsonProperty("filterLight")
    public void setFilterLight(Integer filterLight) {
        this.filterLight = filterLight;
    }

    /**
     * Minimum state id
     *
     */
    @JsonProperty("minStateId")
    public Integer getMinStateId() {
        return minStateId;
    }

    /**
     * Minimum state id
     *
     */
    @JsonProperty("minStateId")
    public void setMinStateId(Integer minStateId) {
        this.minStateId = minStateId;
    }

    /**
     * Maximum state id
     *
     */
    @JsonProperty("maxStateId")
    public Integer getMaxStateId() {
        return maxStateId;
    }

    /**
     * Maximum state id
     *
     */
    @JsonProperty("maxStateId")
    public void setMaxStateId(Integer maxStateId) {
        this.maxStateId = maxStateId;
    }

    /**
     * Default state id
     *
     */
    @JsonProperty("defaultState")
    public Integer getDefaultState() {
        return defaultState;
    }

    /**
     * Default state id
     *
     */
    @JsonProperty("defaultState")
    public void setDefaultState(Integer defaultState) {
        this.defaultState = defaultState;
    }

    /**
     * Blast resistance
     *
     */
    @JsonProperty("resistance")
    public Double getResistance() {
        return resistance;
    }

    /**
     * Blast resistance
     *
     */
    @JsonProperty("resistance")
    public void setResistance(Double resistance) {
        this.resistance = resistance;
    }


    /**
     * BoundingBox of a block
     *
     */

    public enum BoundingBox {

        BLOCK("block"),
        EMPTY("empty");
        private final String value;
        private final static Map<String, BoundingBox> CONSTANTS = new HashMap<>();

        static {
            for (BoundingBox c: values()) {
                CONSTANTS.put(c.value, c);
            }
        }

        BoundingBox(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return this.value;
        }

        @JsonValue
        public String value() {
            return this.value;
        }

        @JsonCreator
        public static BoundingBox fromValue(String value) {
            BoundingBox constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }

}
