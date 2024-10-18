package com.zenith.feature.world;

import lombok.Data;

@Data
public class Input {
    public boolean pressingForward;
    public boolean pressingBack;
    public boolean pressingLeft;
    public boolean pressingRight;
    public boolean jumping;
    public boolean sneaking;
    public boolean sprinting;

    public Input(final boolean pressingForward, final boolean pressingBack, final boolean pressingLeft, final boolean pressingRight, final boolean jumping, final boolean sneaking, final boolean sprinting) {
        this.pressingForward = pressingForward;
        this.pressingBack = pressingBack;
        this.pressingLeft = pressingLeft;
        this.pressingRight = pressingRight;
        this.jumping = jumping;
        this.sneaking = sneaking;
        this.sprinting = sprinting;
    }

    public Input() {
        this(false, false, false, false, false, false, false);
    }

    public Input(Input in) {
        this(in.pressingForward, in.pressingBack, in.pressingLeft, in.pressingRight, in.jumping, in.sneaking, in.sprinting);
    }

    public void reset() {
        pressingForward = false;
        pressingBack = false;
        pressingLeft = false;
        pressingRight = false;
        jumping = false;
        sneaking = false;
        sprinting = false;
    }

    public float getMovementSideways() {
        float f = 0.0F;
        if (pressingLeft) {
            --f;
        }
        if (pressingRight) {
            ++f;
        }
        if (sneaking) {
            f *= 0.3F;
        }
        return f * 0.98f;
    }

    public float getMovementForward() {
        float f = 0.0F;
        if (pressingForward) {
            ++f;
        }
        if (pressingBack) {
            --f;
        }
        if (sneaking) {
            f *= 0.3F;
        }
        return f * 0.98f;
    }
}
