package com.zenith.feature.pathing.blockdata;

import com.zenith.feature.pathing.CollisionBox;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import lombok.Data;

import java.util.Collections;
import java.util.List;

@Data
public class Block {
    private static final Int2ObjectMap<List<CollisionBox>> EMPTY_STATE_COLLISION_BOXES = new Int2ObjectOpenHashMap<>();
    static {
        EMPTY_STATE_COLLISION_BOXES.put(0, Collections.singletonList(new CollisionBox(0.0, 0.0, 0.0, 0.0, 0.0, 0.0)));
    }
    public static Block AIR = new Block(0,
                                        "Air",
                                        "air",
                                        BlockData.BoundingBox.EMPTY,
                                        EMPTY_STATE_COLLISION_BOXES);

    private final Integer id;
    private final String displayName;
    private final String name;
    private final BlockData.BoundingBox boundingBox;
    private final Int2ObjectMap<List<CollisionBox>> stateCollisionBoxes;

    public List<CollisionBox> getCollisionBoxesForStateId(final int id) {
        return stateCollisionBoxes.getOrDefault(id, Collections.emptyList());
    }

    public boolean isFluid() {
        return name.equals("water") || name.equals("lava") || name.equals("bubble_column");
    }

    public boolean isWater() {
        return name.equals("water") || name.equals("bubble_column");
    }
}
