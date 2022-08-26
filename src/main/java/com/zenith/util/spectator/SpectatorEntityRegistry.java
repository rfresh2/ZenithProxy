package com.zenith.util.spectator;

import com.zenith.util.spectator.entity.*;

import java.util.*;

import static com.zenith.util.Constants.SERVER_LOG;
import static java.util.Objects.isNull;

public final class SpectatorEntityRegistry {

    private SpectatorEntityRegistry() {}

    static final Map<String, SpectatorEntity> spectatorEntityMap = new HashMap<>();

    static {
        spectatorEntityMap.put("cat", new SpectatorCatEntity());
        spectatorEntityMap.put("dog", new SpectatorDogEntity());
        spectatorEntityMap.put("bat", new SpectatorBatEntity());
        spectatorEntityMap.put("crystal", new SpectatorEntityEndCrystal());
        spectatorEntityMap.put("dragon", new SpectatorEntityEnderDragon());
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
