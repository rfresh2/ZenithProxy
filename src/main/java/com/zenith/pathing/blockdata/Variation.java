package com.zenith.pathing.blockdata;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "metadata",
        "displayName",
        "description"
})
public class Variation {

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
    @JsonProperty("description")
    private String description;

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

    @JsonProperty("description")
    public String getDescription() {
        return description;
    }

    @JsonProperty("description")
    public void setDescription(String description) {
        this.description = description;
    }

}
