package com.zenith.pathing.blockdata;

import com.google.common.collect.ImmutableMap;
import com.zenith.pathing.CollisionBox;
import lombok.Data;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Data
public class Block {
    public static Block AIR = new Block(0, "Air", "air", BlockData.BoundingBox.EMPTY, ImmutableMap.of(0, Collections.singletonList(new CollisionBox(0.0, 0.0, 0.0, 0.0, 0.0, 0.0))));

    private final Integer id;
    private final String displayName;
    private final String name;
    private final BlockData.BoundingBox boundingBox;
    private final Map<Integer, List<CollisionBox>> stateCollisionBoxes;

    public List<CollisionBox> getCollisionBoxesForStateId(final Integer id) {
        return stateCollisionBoxes.getOrDefault(id, Collections.emptyList());
    }
}
