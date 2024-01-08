package com.zenith.feature.whitelist;

import com.github.steveice10.mc.auth.data.GameProfile;
import lombok.Data;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static com.zenith.Shared.*;
import static java.util.Objects.nonNull;

@Data
public class PlayerList {
    private final String name;
    private final ArrayList<PlayerEntry> entries;
    private ScheduledFuture<?> refreshScheduledFuture;

    public void startRefreshTask() {
        stopRefreshTask();
        refreshScheduledFuture = SCHEDULED_EXECUTOR_SERVICE.scheduleAtFixedRate(this::refresh,
                                                                                // todo: move these configs to more central config
                                                                                ThreadLocalRandom.current().nextInt(Math.max(1, (int) (CONFIG.server.playerListsRefreshIntervalMins / 2))),
                                                                                Math.max(10L, CONFIG.server.playerListsRefreshIntervalMins),
                                                                                TimeUnit.MINUTES);
    }

    public void stopRefreshTask() {
        if (nonNull(refreshScheduledFuture)) {
            refreshScheduledFuture.cancel(true);
        }
    }

    public Optional<PlayerEntry> add(final String username) {
        final Optional<PlayerEntry> playerListEntryOptional = createPlayerListEntry(username);
        if (playerListEntryOptional.isPresent()) {
            var playerListEntry = playerListEntryOptional.get();
            if (!entries.contains(playerListEntry)) {
                entries.add(playerListEntry);
            }
        }
        return playerListEntryOptional;
    }

    public void remove(final String username) {
        this.entries.removeIf(entry -> entry.getUsername().equalsIgnoreCase(username));
    }

    public void remove(final UUID uuid) {
        this.entries.removeIf(entry -> entry.getUuid().equals(uuid));
    }

    public void clear() {
        entries.clear();
    }

    public boolean contains(final GameProfile clientGameProfile) {
        final Optional<PlayerEntry> presentOptional = entries.stream()
            .filter(entry -> entry.getUuid().equals(clientGameProfile.getId()))
            .findFirst();
        if (presentOptional.isPresent()) {
            // player is present
            // let's update their player profile
            presentOptional.get().setLastRefreshed(Instant.now().getEpochSecond());
            presentOptional.get().setUsername(clientGameProfile.getName());
            return true;
        } else {
            return false;
        }
    }

    public boolean contains(final UUID uuid) {
        return entries.stream()
            .map(PlayerEntry::getUuid)
            .anyMatch(id -> Objects.equals(id, uuid));
    }

    public boolean contains(final String name) {
        return entries.stream()
            .map(PlayerEntry::getUsername)
            .anyMatch(name::equalsIgnoreCase);
    }


    private void refresh() {
        SERVER_LOG.debug("Refreshing {} entries...", name);
        entries.forEach(this::refreshEntry);
    }

    private void refreshEntry(final PlayerEntry playerEntry) {
        final Optional<PlayerEntry> entryFromUUID = createPlayerListEntry(playerEntry.getUuid());
        if (entryFromUUID.isPresent()) {
            // in-place update
            playerEntry.setUsername(entryFromUUID.get().getUsername());
            playerEntry.setLastRefreshed(Instant.now().getEpochSecond());
        } else {
            SERVER_LOG.error("{}} refresh: unable to refresh player with username: {} and uuid: {}", name, playerEntry.getUsername(), playerEntry.getUuid().toString());
        }
    }

    public static Optional<PlayerEntry> createPlayerListEntry(final String username) {
        return MOJANG_API.getProfileFromUsername(username)
            .map(profile -> new PlayerEntry(profile.name(),
                                            profile.uuid(),
                                            Instant.now().getEpochSecond()));
    }

    public static Optional<PlayerEntry> createPlayerListEntry(final UUID uuid) {
        return SESSION_SERVER_API.getProfileFromUUID(uuid)
            .map(profile -> new PlayerEntry(
                profile.name(),
                profile.uuid(),
                Instant.now().getEpochSecond()));
    }
}
