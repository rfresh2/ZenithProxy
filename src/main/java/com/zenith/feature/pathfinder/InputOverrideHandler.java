package com.zenith.feature.pathfinder;

import com.zenith.feature.pathfinder.behavior.Behavior;
import com.zenith.feature.player.ClickTarget;
import com.zenith.feature.player.Input;
import com.zenith.mc.block.BlockPos;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.Hand;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class InputOverrideHandler extends Behavior {
    /**
     * Maps inputs to whether or not we are forcing their state down.
     */
    private final Map<PathInput, Boolean> inputForceStateMap = new HashMap<>();
    private @Nullable BlockPos clickTarget = null;
    @Nullable public Input currentInput;

    public InputOverrideHandler(Baritone baritone) {
        super(baritone);
    }

    /**
     * Returns whether or not we are forcing down the specified {@link Input}.
     *
     * @param input The input
     * @return Whether or not it is being forced down
     */
    public final boolean isInputForcedDown(PathInput input) {
        return input != null && this.inputForceStateMap.getOrDefault(input, false);
    }

    /**
     * Sets whether or not the specified {@link Input} is being forced down.
     *
     * @param input  The {@link Input}
     * @param forced Whether or not the state is being forced
     */
    public final void setInputForceState(PathInput input, boolean forced) {
        this.inputForceStateMap.put(input, forced);
    }

    public final void setClickTarget(@Nullable BlockPos pos) {
        this.clickTarget = pos;
    }

    /**
     * Clears the override state for all keys
     */
    public final void clearAllKeys() {
        this.inputForceStateMap.clear();
        this.clickTarget = null;
    }

    public final void onTick() {
        if (isInputForcedDown(PathInput.LEFT_CLICK_BLOCK)) {
            setInputForceState(PathInput.RIGHT_CLICK_BLOCK, false);
        }

        if (inputForceStateMap.containsValue(true)) {
            var in = Input.builder();
            this.inputForceStateMap.forEach((entry, pressed) -> {
                if (!pressed) return;
                switch (entry) {
                    case MOVE_FORWARD -> in.pressingForward(true);
                    case MOVE_BACK -> in.pressingBack(true);
                    case MOVE_LEFT -> in.pressingLeft(true);
                    case MOVE_RIGHT -> in.pressingRight(true);
                    case JUMP -> in.jumping(true);
                    case SNEAK -> in.sneaking(true);
                    case SPRINT -> in.sprinting(true);
                    case LEFT_CLICK_BLOCK -> {
                        in.leftClick(true)
                            .hand(Hand.MAIN_HAND);
                        if (clickTarget != null) {
                            in.clickTarget(new ClickTarget.BlockPosition(clickTarget.x(), clickTarget.y(), clickTarget.z()));
                        } else {
                            in.clickTarget(ClickTarget.AnyBlock.INSTANCE);
                        }
                    }
                    case RIGHT_CLICK_BLOCK -> {
                        in.rightClick(true)
                            .hand(Hand.MAIN_HAND);
                        if (clickTarget != null) {
                            in.clickTarget(new ClickTarget.BlockPosition(clickTarget.x(), clickTarget.y(), clickTarget.z()));
                        } else {
                            in.clickTarget(ClickTarget.AnyBlock.INSTANCE);
                        }
                    }
                }
            });
            this.currentInput = in.build();
        } else {
            this.currentInput = null;
        }
    }
}
