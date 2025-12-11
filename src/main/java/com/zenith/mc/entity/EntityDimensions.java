package com.zenith.mc.entity;

import com.zenith.mc.block.CollisionBox;
import lombok.Data;

@Data
public class EntityDimensions {
    private final float width;
    private final float height;
    private final float eyeHeight;
    private final CollisionBox collisionBox;

    public EntityDimensions(float width, float height, float eyeHeight) {
        this.width = width;
        this.height = height;
        this.eyeHeight = eyeHeight;
        this.collisionBox = toCollisionBox();
    }

    private CollisionBox toCollisionBox() {
        double w = this.width / 2.0;
        return new CollisionBox(
            -w, w,
            0, this.height,
            -w, w
        );
    }
}
