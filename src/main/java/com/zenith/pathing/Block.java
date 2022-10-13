package com.zenith.pathing;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class Block {
    private final Integer id;
    private final String displayName;
    private final String name;
    private final Float hardness;
    private final Integer stackSize;
    private final Boolean diggable;
    private final BoundingBox boundingBox;
    private final String material;
    private final Map<String, Boolean> harvestTools;
    private final List<State> states;
    //    private final List<Integer> drops;
    private final Boolean transparent;
    private final Integer emitLight;
    private final Integer filterLight;
    private final Integer minStateId;
    private final Integer maxStateId;
    private final Integer defaultState;
}
