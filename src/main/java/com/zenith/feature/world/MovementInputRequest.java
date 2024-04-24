package com.zenith.feature.world;

import java.util.Optional;

public record MovementInputRequest(Optional<Input> input, Optional<Float> yaw, Optional<Float> pitch, int priority) { }
