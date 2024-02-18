package com.zenith.feature.whitelist;

import com.github.steveice10.mc.auth.data.GameProfile;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static com.zenith.feature.whitelist.PlayerListsManager.createPlayerListEntry;

public record PlayerList(
    String name,
    ArrayList<PlayerEntry> entries
) {
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

    // returns true if the account was added. false if the account was already present
    public boolean add(final String username, final UUID uuid) {
        var entry = new PlayerEntry(username, uuid, Instant.now().getEpochSecond());
        if (!entries.contains(entry)) {
            entries.add(entry);
            return true;
        }
        return false;
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
}
