package com.zenith.feature.whitelist;

import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.mc.auth.service.ProfileService;
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
            .filter(whitelistEntry -> whitelistEntry.getUuid().equals(clientGameProfile.getId()))
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
        final Optional<PlayerEntry> whitelistEntryFromUUID = createPlayerListEntry(playerEntry.getUuid());
        if (whitelistEntryFromUUID.isPresent()) {
            // in-place update
            playerEntry.setUsername(whitelistEntryFromUUID.get().getUsername());
            playerEntry.setLastRefreshed(Instant.now().getEpochSecond());
        } else {
            SERVER_LOG.error("{}} refresh: unable to refresh player with username: {} and uuid: {}", name, playerEntry.getUsername(), playerEntry.getUuid().toString());
        }
    }

    public static Optional<PlayerEntry> createPlayerListEntry(final UUID uuid) {
        final ProfileService profileService = new ProfileService();
        final Optional<GameProfile> profileOptional = Optional.ofNullable(profileService.findProfileByUUID(uuid));
        if (profileOptional.isPresent()) {
            final GameProfile gameProfile = profileOptional.get();
            return Optional.of(new PlayerEntry(gameProfile.getName(), gameProfile.getId(), Instant.now().getEpochSecond()));
        } else {
            return Optional.empty();
        }
    }

    public static Optional<PlayerEntry> createPlayerListEntry(final String username) {
        final ProfileService profileService = new ProfileService();
        int tries = 0;
        final SingleLookupProfileLookupCallbackHelper callbackHelper = new SingleLookupProfileLookupCallbackHelper();
        while (tries++ < 3 && !callbackHelper.completed) {
            profileService.findProfilesByName(new String[]{username}, callbackHelper, false);
            if (callbackHelper.completed && callbackHelper.errorException.isEmpty() && callbackHelper.gameProfile.isPresent()) {
                GameProfile gameProfile = callbackHelper.gameProfile.get();
                return Optional.of(new PlayerEntry(gameProfile.getName(), gameProfile.getId(), Instant.now().getEpochSecond()));
            } else {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        callbackHelper.errorException
            .ifPresent(e -> SERVER_LOG.error("Failed getting PlayerListEntry for username: {}", username, e));
        return Optional.empty();
    }

    private static final class SingleLookupProfileLookupCallbackHelper implements ProfileService.ProfileLookupCallback {
        public Optional<GameProfile> gameProfile = Optional.empty();
        public Optional<Exception> errorException = Optional.empty();
        public boolean completed = false;

        @Override
        public void onProfileLookupSucceeded(GameProfile profile) {
            gameProfile = Optional.of(profile);
            completed = true;
        }

        @Override
        public void onProfileLookupFailed(GameProfile profile, Exception e) {
            completed = true;
        }
    }
}
