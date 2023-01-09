package com.zenith.util.food;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.List;


/**
 * food
 * <p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "id",
        "displayName",
        "stackSize",
        "name",
        "foodPoints",
        "saturation",
        "effectiveQuality",
        "saturationRatio",
        "variations"
})
public class FoodData {

    /**
     * The unique identifier for an item
     * (Required)
     */
    @JsonProperty("id")
    @JsonPropertyDescription("The unique identifier for an item")
    private Integer id;
    /**
     * The display name of an item
     * (Required)
     */
    @JsonProperty("displayName")
    @JsonPropertyDescription("The display name of an item")
    private String displayName;
    /**
     * Stack size for an item
     * (Required)
     */
    @JsonProperty("stackSize")
    @JsonPropertyDescription("Stack size for an item")
    private Integer stackSize;
    /**
     * The name of an item
     * (Required)
     */
    @JsonProperty("name")
    @JsonPropertyDescription("The name of an item")
    private String name;
    /**
     * The amount of food points the food item replenishes
     * (Required)
     */
    @JsonProperty("foodPoints")
    @JsonPropertyDescription("The amount of food points the food item replenishes")
    private Double foodPoints;
    /**
     * The amount of saturation points the food restores
     * (Required)
     */
    @JsonProperty("saturation")
    @JsonPropertyDescription("The amount of saturation points the food restores")
    private Double saturation;
    /**
     * The effective quality of the food item
     * (Required)
     */
    @JsonProperty("effectiveQuality")
    @JsonPropertyDescription("The effective quality of the food item")
    private Double effectiveQuality;
    /**
     * The saturation ratio of the food item
     * (Required)
     */
    @JsonProperty("saturationRatio")
    @JsonPropertyDescription("The saturation ratio of the food item")
    private Double saturationRatio;
    @JsonProperty("variations")
    private List<FoodVariationData> variations = null;

    /**
     * The unique identifier for an item
     * (Required)
     */
    @JsonProperty("id")
    public Integer getId() {
        return id;
    }

    /**
     * The unique identifier for an item
     * (Required)
     */
    @JsonProperty("id")
    public void setId(Integer id) {
        this.id = id;
    }

    /**
     * The display name of an item
     * (Required)
     */
    @JsonProperty("displayName")
    public String getDisplayName() {
        return displayName;
    }

    /**
     * The display name of an item
     * (Required)
     */
    @JsonProperty("displayName")
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Stack size for an item
     * (Required)
     */
    @JsonProperty("stackSize")
    public Integer getStackSize() {
        return stackSize;
    }

    /**
     * Stack size for an item
     * (Required)
     */
    @JsonProperty("stackSize")
    public void setStackSize(Integer stackSize) {
        this.stackSize = stackSize;
    }

    /**
     * The name of an item
     * (Required)
     */
    @JsonProperty("name")
    public String getName() {
        return name;
    }

    /**
     * The name of an item
     * (Required)
     */
    @JsonProperty("name")
    public void setName(String name) {
        this.name = name;
    }

    /**
     * The amount of food points the food item replenishes
     * (Required)
     */
    @JsonProperty("foodPoints")
    public Double getFoodPoints() {
        return foodPoints;
    }

    /**
     * The amount of food points the food item replenishes
     * (Required)
     */
    @JsonProperty("foodPoints")
    public void setFoodPoints(Double foodPoints) {
        this.foodPoints = foodPoints;
    }

    /**
     * The amount of saturation points the food restores
     * (Required)
     */
    @JsonProperty("saturation")
    public Double getSaturation() {
        return saturation;
    }

    /**
     * The amount of saturation points the food restores
     * (Required)
     */
    @JsonProperty("saturation")
    public void setSaturation(Double saturation) {
        this.saturation = saturation;
    }

    /**
     * The effective quality of the food item
     * (Required)
     */
    @JsonProperty("effectiveQuality")
    public Double getEffectiveQuality() {
        return effectiveQuality;
    }

    /**
     * The effective quality of the food item
     * (Required)
     */
    @JsonProperty("effectiveQuality")
    public void setEffectiveQuality(Double effectiveQuality) {
        this.effectiveQuality = effectiveQuality;
    }

    /**
     * The saturation ratio of the food item
     * (Required)
     */
    @JsonProperty("saturationRatio")
    public Double getSaturationRatio() {
        return saturationRatio;
    }

    /**
     * The saturation ratio of the food item
     * (Required)
     */
    @JsonProperty("saturationRatio")
    public void setSaturationRatio(Double saturationRatio) {
        this.saturationRatio = saturationRatio;
    }

    @JsonProperty("variations")
    public List<FoodVariationData> getVariations() {
        return variations;
    }

    @JsonProperty("variations")
    public void setVariations(List<FoodVariationData> variations) {
        this.variations = variations;
    }

}
