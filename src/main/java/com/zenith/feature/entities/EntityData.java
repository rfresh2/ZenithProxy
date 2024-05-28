package com.zenith.feature.entities;

import org.geysermc.mcprotocollib.protocol.data.game.entity.type.EntityType;

public record EntityData(int id, String name, double width, double height, EntityType mcplType) { }
