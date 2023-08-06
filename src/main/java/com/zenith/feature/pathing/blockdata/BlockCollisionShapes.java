package com.zenith.feature.pathing.blockdata;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * blockCollisionShapes
 * <p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "blocks",
        "shapes"
})
public class BlockCollisionShapes {

    /**
     * Each block's collision shape id(s).
     * (Required)
     */
    @JsonProperty("blocks")
    @JsonPropertyDescription("Each block's collision shape id(s).")
    private Blocks blocks;
    /**
     * Collision shapes by id, each shape being composed of a list of collision boxes.
     * (Required)
     */
    @JsonProperty("shapes")
    @JsonPropertyDescription("Collision shapes by id, each shape being composed of a list of collision boxes.")
    private Shapes shapes;

    /**
     * Each block's collision shape id(s).
     * (Required)
     */
    @JsonProperty("blocks")
    public Blocks getBlocks() {
        return blocks;
    }

    /**
     * Each block's collision shape id(s).
     * (Required)
     */
    @JsonProperty("blocks")
    public void setBlocks(Blocks blocks) {
        this.blocks = blocks;
    }

    /**
     * Collision shapes by id, each shape being composed of a list of collision boxes.
     * (Required)
     */
    @JsonProperty("shapes")
    public Shapes getShapes() {
        return shapes;
    }

    /**
     * Collision shapes by id, each shape being composed of a list of collision boxes.
     * (Required)
     */
    @JsonProperty("shapes")
    public void setShapes(Shapes shapes) {
        this.shapes = shapes;
    }

}
