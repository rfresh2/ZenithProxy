package com.zenith.feature.pathing;

import com.zenith.feature.pathing.raycast.BlockOrEntityRaycastResult;
import lombok.Data;

import java.util.function.Predicate;

@Data
public class Input {
    public static Predicate<BlockOrEntityRaycastResult> DEFAULT_CLICK_PREDICATE = (result) -> true;
    public float movementSideways;
    public float movementForward;
    public boolean pressingForward;
    public boolean pressingBack;
    public boolean pressingLeft;
    public boolean pressingRight;
    public boolean jumping;
    public boolean sneaking;
    public boolean sprinting;
    public boolean leftClick;
    public Predicate<BlockOrEntityRaycastResult> leftClickPredicate;
    public boolean rightClick;
    public Predicate<BlockOrEntityRaycastResult> rightClickPredicate;

    public Input(final boolean pressingForward,
                 final boolean pressingBack,
                 final boolean pressingLeft,
                 final boolean pressingRight,
                 final boolean jumping,
                 final boolean sneaking,
                 final boolean sprinting,
                 final boolean leftClick,
                 final Predicate<BlockOrEntityRaycastResult> leftClickPredicate,
                 final boolean rightClick,
                 final Predicate<BlockOrEntityRaycastResult> rightClickPredicate
    ) {
        this.pressingForward = pressingForward;
        this.pressingBack = pressingBack;
        this.pressingLeft = pressingLeft;
        this.pressingRight = pressingRight;
        this.jumping = jumping;
        this.sneaking = sneaking;
        this.sprinting = sprinting;
        this.leftClick = leftClick;
        this.leftClickPredicate = leftClickPredicate;
        this.rightClick = rightClick;
        this.rightClickPredicate = rightClickPredicate;
    }

    public Input() {
        this(
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            DEFAULT_CLICK_PREDICATE,
            false,
            DEFAULT_CLICK_PREDICATE);
    }

    public void reset() {
        movementSideways = 0;
        movementForward = 0;
        pressingForward = false;
        pressingBack = false;
        pressingLeft = false;
        pressingRight = false;
        jumping = false;
        sneaking = false;
        sprinting = false;
        leftClick = false;
        leftClickPredicate = DEFAULT_CLICK_PREDICATE;
        rightClick = false;
        rightClickPredicate = DEFAULT_CLICK_PREDICATE;
    }
}
