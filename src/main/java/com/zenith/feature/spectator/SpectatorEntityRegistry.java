package com.zenith.feature.spectator;

import com.zenith.feature.spectator.entity.SpectatorEntity;
import com.zenith.feature.spectator.entity.mob.*;
import com.zenith.feature.spectator.entity.object.SpectatorEntityEgg;
import com.zenith.feature.spectator.entity.object.SpectatorEntityEndCrystal;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.zenith.Shared.SERVER_LOG;
import static java.util.Objects.isNull;

public final class SpectatorEntityRegistry {

    private SpectatorEntityRegistry() {}

    static final Object2ObjectOpenHashMap<String, SpectatorEntity> spectatorEntityMap = new Object2ObjectOpenHashMap<>();

    static {
        spectatorEntityMap.put("cat", new SpectatorEntityCat());
        spectatorEntityMap.put("dog", new SpectatorEntityDog());
        spectatorEntityMap.put("bat", new SpectatorEntityBat());
        spectatorEntityMap.put("crystal", new SpectatorEntityEndCrystal());
        spectatorEntityMap.put("dragon", new SpectatorEntityEnderDragon());
        spectatorEntityMap.put("wither", new SpectatorEntityWither());
        spectatorEntityMap.put("egg", new SpectatorEntityEgg());
        spectatorEntityMap.put("creeper", new SpectatorEntityCreeper());
        spectatorEntityMap.put("vex", new SpectatorEntityVex());
        spectatorEntityMap.put("chicken", new SpectatorEntityChicken());
        spectatorEntityMap.put("parrot", new SpectatorEntityParrot());
        spectatorEntityMap.put("ghast", new SpectatorEntityGhast());
    }

    public static SpectatorEntity getSpectatorEntityWithDefault(final String identifier) {
        SpectatorEntity spectatorEntity = spectatorEntityMap.get(identifier);
        if (isNull(spectatorEntity)) {
            SERVER_LOG.error("Unknown Spectator Entity Identifier: " + identifier);
            return spectatorEntityMap.get("cat");
        }
        return spectatorEntity;
    }

    public static Optional<SpectatorEntity> getSpectatorEntity(final String identifier) {
        SpectatorEntity spectatorEntity = spectatorEntityMap.get(identifier);
        if (isNull(spectatorEntity)) {
            SERVER_LOG.error("Unknown Spectator Entity Identifier: " + identifier);
            return Optional.empty();
        }
        return Optional.of(spectatorEntity);
    }

    public static List<String> getEntityIdentifiers() {
        return new ArrayList<>(spectatorEntityMap.keySet());
    }
}
