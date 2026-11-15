package com.zenith.feature.queue.mcping.data;

import com.zenith.feature.queue.mcping.rawData.Players;
import com.zenith.feature.queue.mcping.rawData.Version;
import com.zenith.util.ComponentSerializer;
import net.kyori.adventure.text.Component;

public record MCResponse(Players players, Version version, String favicon, String description, Component descriptionComponent) {
    public MCResponse(Players players, Version version, String favicon, Component descriptionComponent) {
        this(players, version, favicon, ComponentSerializer.serializePlain(descriptionComponent), descriptionComponent);
    }
}
