package com.zenith.feature.items;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * item
 * <p>
 *
 *
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "id",
    "displayName",
    "stackSize",
    "enchantCategories",
    "repairWith",
    "maxDurability",
    "name",
    "variations"
})
public class ItemsData {

    /**
     * The unique identifier for an item
     * (Required)
     *
     */
    @JsonProperty("id")
    @JsonPropertyDescription("The unique identifier for an item")
    private int id;
    /**
     * The display name of an item
     * (Required)
     *
     */
    @JsonProperty("displayName")
    @JsonPropertyDescription("The display name of an item")
    private String displayName;
    /**
     * Stack size for an item
     * (Required)
     *
     */
    @JsonProperty("stackSize")
    @JsonPropertyDescription("Stack size for an item")
    private int stackSize;
    /**
     * describes categories of enchants this item can use
     *
     */
    @JsonProperty("enchantCategories")
    @JsonDeserialize(as = java.util.LinkedHashSet.class)
    @JsonPropertyDescription("describes categories of enchants this item can use")
    private Set<String> enchantCategories;
    /**
     * describes what items this item can be fixed with in an anvil
     *
     */
    @JsonProperty("repairWith")
    @JsonDeserialize(as = java.util.LinkedHashSet.class)
    @JsonPropertyDescription("describes what items this item can be fixed with in an anvil")
    private Set<String> repairWith;
    /**
     * the amount of durability an item has before being damaged/used
     *
     */
    @JsonProperty("maxDurability")
    @JsonPropertyDescription("the amount of durability an item has before being damaged/used")
    private int maxDurability;
    /**
     * The name of an item
     * (Required)
     *
     */
    @JsonProperty("name")
    @JsonPropertyDescription("The name of an item")
    private String name;
    @JsonProperty("variations")
    private List<ItemsVariation> variations;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new LinkedHashMap<String, Object>();

    /**
     * The unique identifier for an item
     * (Required)
     *
     */
    @JsonProperty("id")
    public int getId() {
        return id;
    }

    /**
     * The unique identifier for an item
     * (Required)
     *
     */
    @JsonProperty("id")
    public void setId(int id) {
        this.id = id;
    }

    /**
     * The display name of an item
     * (Required)
     *
     */
    @JsonProperty("displayName")
    public String getDisplayName() {
        return displayName;
    }

    /**
     * The display name of an item
     * (Required)
     *
     */
    @JsonProperty("displayName")
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Stack size for an item
     * (Required)
     *
     */
    @JsonProperty("stackSize")
    public int getStackSize() {
        return stackSize;
    }

    /**
     * Stack size for an item
     * (Required)
     *
     */
    @JsonProperty("stackSize")
    public void setStackSize(int stackSize) {
        this.stackSize = stackSize;
    }

    /**
     * describes categories of enchants this item can use
     *
     */
    @JsonProperty("enchantCategories")
    public Set<String> getEnchantCategories() {
        return enchantCategories;
    }

    /**
     * describes categories of enchants this item can use
     *
     */
    @JsonProperty("enchantCategories")
    public void setEnchantCategories(Set<String> enchantCategories) {
        this.enchantCategories = enchantCategories;
    }

    /**
     * describes what items this item can be fixed with in an anvil
     *
     */
    @JsonProperty("repairWith")
    public Set<String> getRepairWith() {
        return repairWith;
    }

    /**
     * describes what items this item can be fixed with in an anvil
     *
     */
    @JsonProperty("repairWith")
    public void setRepairWith(Set<String> repairWith) {
        this.repairWith = repairWith;
    }

    /**
     * the amount of durability an item has before being damaged/used
     *
     */
    @JsonProperty("maxDurability")
    public int getMaxDurability() {
        return maxDurability;
    }

    /**
     * the amount of durability an item has before being damaged/used
     *
     */
    @JsonProperty("maxDurability")
    public void setMaxDurability(int maxDurability) {
        this.maxDurability = maxDurability;
    }

    /**
     * The name of an item
     * (Required)
     *
     */
    @JsonProperty("name")
    public String getName() {
        return name;
    }

    /**
     * The name of an item
     * (Required)
     *
     */
    @JsonProperty("name")
    public void setName(String name) {
        this.name = name;
    }

    @JsonProperty("variations")
    public List<ItemsVariation> getVariations() {
        return variations;
    }

    @JsonProperty("variations")
    public void setVariations(List<ItemsVariation> variations) {
        this.variations = variations;
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

}
