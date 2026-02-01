package com.zenith.feature.pathfinder.movement;

import com.zenith.feature.pathfinder.PathInput;
import com.zenith.feature.player.Rotation;
import com.zenith.mc.block.BlockPos;
import lombok.Data;
import lombok.experimental.Accessors;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

@Data
@Accessors(chain = true)
public class MovementState {
    private MovementStatus status;
    private MovementTarget target = new MovementTarget();
    private final Map<PathInput, Boolean> inputStates = new HashMap<>();
    private @Nullable BlockPos clickTarget = null;

    public MovementState setInput(PathInput input, boolean forced) {
//        PATH_LOG.info("Set Input Target: {}: {}", input.name(), forced);
        this.inputStates.put(input, forced);
        return this;
    }

    public MovementState setClickTarget(@Nullable BlockPos pos) {
        this.clickTarget = pos;
        return this;
    }

    public boolean isInputForced(PathInput input) {
        return this.inputStates.getOrDefault(input, false);
    }

    public record MovementTarget(@Nullable Rotation rotation, boolean forceRotations) {
        public MovementTarget() {
            this(null, false);
        }
    }
}
