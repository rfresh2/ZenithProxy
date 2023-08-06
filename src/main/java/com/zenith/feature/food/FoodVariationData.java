package com.zenith.feature.food;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import javax.annotation.processing.Generated;


@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "metadata",
        "displayName"
})
@Generated("jsonschema2pojo")
public class FoodVariationData {

    /**
     * (Required)
     */
    @JsonProperty("metadata")
    private Integer metadata;
    /**
     * (Required)
     */
    @JsonProperty("displayName")
    private String displayName;

    /**
     * (Required)
     */
    @JsonProperty("metadata")
    public Integer getMetadata() {
        return metadata;
    }

    /**
     * (Required)
     */
    @JsonProperty("metadata")
    public void setMetadata(Integer metadata) {
        this.metadata = metadata;
    }

    /**
     * (Required)
     */
    @JsonProperty("displayName")
    public String getDisplayName() {
        return displayName;
    }

    /**
     * (Required)
     */
    @JsonProperty("displayName")
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

}
