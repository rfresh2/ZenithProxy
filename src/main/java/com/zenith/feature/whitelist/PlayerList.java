package com.zenith.feature.whitelist;

import org.geysermc.mcprotocollib.auth.GameProfile;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static com.zenith.feature.whitelist.PlayerListsManager.createPlayerListEntry;

public record PlayerList(
    String name,
    ArrayList<PlayerEntry> entries // reference to list in Config
) {
    public synchronized Optional<PlayerEntry> add(final String username) {
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
    public synchronized boolean add(final String username, final UUID uuid) {
        var entry = new PlayerEntry(username, uuid, Instant.now().getEpochSecond());
        if (!entries.contains(entry)) {
            entries.add(entry);
            return true;
        }
        return false;
    }

    public synchronized void remove(final String username) {
        this.entries.removeIf(entry -> entry.getUsername().equalsIgnoreCase(username));
    }

    public synchronized void remove(final UUID uuid) {
        this.entries.removeIf(entry -> entry.getUuid().equals(uuid));
    }

    public synchronized void clear() {
        entries.clear();
    }

    // todo: these lookups could be sped up with secondary hashmaps
    //  we'd have to be very careful to keep those in sync
    //  as is, this shouldn't be too impactful for the extra complexity. O(n) loops are generally pretty fast at small sizes

    public synchronized boolean contains(final GameProfile clientGameProfile) {
        for (int i = 0; i < entries.size(); i++) {
            final PlayerEntry entry = entries.get(i);
            if (entry.getUuid().equals(clientGameProfile.getId())) {
                // player is present
                // let's update their player profile
                entry.setLastRefreshed(Instant.now().getEpochSecond());
                entry.setUsername(clientGameProfile.getName());
                return true;
            }
        }
        return false;
    }

    public synchronized boolean contains(final UUID uuid) {
        for (int i = 0; i < entries.size(); i++) {
            final PlayerEntry entry = entries.get(i);
            if (Objects.equals(entry.getUuid(), uuid))
                return true;
        }
        return false;
    }

    public synchronized boolean contains(final String name) {
        for (int i = 0; i < entries.size(); i++) {
            final PlayerEntry entry = entries.get(i);
            if (name.equalsIgnoreCase(entry.getUsername()))
                return true;
        }
        return false;
    }
}
