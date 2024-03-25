package com.zenith.feature.entities;

import com.github.steveice10.mc.protocol.data.game.entity.type.EntityType;

public record EntityData(String name, double width, double height, EntityType mcplType) { }
