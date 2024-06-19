package com.zenith.feature.whitelist;

import com.zenith.feature.api.ProfileData;
import com.zenith.feature.api.crafthead.CraftheadApi;
import com.zenith.feature.api.minetools.MinetoolsApi;
import com.zenith.feature.api.mojang.MojangApi;
import com.zenith.feature.api.sessionserver.SessionServerApi;
import com.zenith.util.Wait;
import lombok.Getter;

import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.zenith.Shared.*;

@Getter
public class PlayerListsManager {
    private PlayerList whitelist;
    private PlayerList spectatorWhitelist;
    private PlayerList friendsList;
    private PlayerList ignoreList;
    private PlayerList stalkList;
    private ScheduledFuture<?> refreshScheduledFuture;

    public void init() { // must be called after config is loaded
        whitelist = new PlayerList("whitelist", CONFIG.server.extra.whitelist.whitelist);
        spectatorWhitelist = new PlayerList("spectatorWhitelist", CONFIG.server.spectator.whitelist);
        friendsList = new PlayerList("friendsList", CONFIG.client.extra.friendsList);
        ignoreList = new PlayerList("ignoreList", CONFIG.client.extra.chat.ignoreList);
        stalkList = new PlayerList("stalkList", CONFIG.client.extra.stalk.stalking);
        startRefreshTask();
    }

    public void startRefreshTask() {
        stopRefreshTask();
        refreshScheduledFuture = EXECUTOR.scheduleAtFixedRate(
            this::refreshLists,
            ThreadLocalRandom.current().nextInt(Math.max(1, (int) (CONFIG.server.playerListsRefreshIntervalMins / 2))),
            Math.max(10L, CONFIG.server.playerListsRefreshIntervalMins),
            TimeUnit.MINUTES);
    }

    public void stopRefreshTask() {
        if (refreshScheduledFuture != null) {
            refreshScheduledFuture.cancel(true);
        }
    }

    private void refreshLists() {
        var playerEntryList = Stream
            .of(getWhitelist(), getSpectatorWhitelist(), getFriendsList(), getIgnoreList(), getStalkList())
            .map(PlayerList::entries)
            .flatMap(Collection::stream)
            .toList();

        // avoid duplicate API requests for the same UUID
        final Map<UUID, PlayerEntry> uniquePlayers = playerEntryList.stream()
            .collect(Collectors.toMap(PlayerEntry::getUuid, Function.identity(), (existing, replacement) -> existing));

        for (var entry : uniquePlayers.entrySet()) {
            Wait.waitMs(250); // trying to avoid mojang API rate limiting
            refreshEntry(entry.getValue())
                .ifPresentOrElse(
                    entry::setValue,
                    () -> SERVER_LOG.error("PlayerLists refresh: unable to refresh player with username: {} and uuid: {}", entry.getValue().getUsername(), entry.getValue().getUuid().toString())
                );
        }

        for (PlayerEntry e : playerEntryList) {
            var newEntry = uniquePlayers.get(e.getUuid());
            e.setUsername(newEntry.getUsername());
            e.setLastRefreshed(newEntry.getLastRefreshed());
        }
    }

    private Optional<PlayerEntry> refreshEntry(final PlayerEntry playerEntry) {
        return createPlayerListEntry(playerEntry.getUuid());
    }

    public static Optional<PlayerEntry> createPlayerListEntry(final String username) {
        return getProfileFromUsername(username)
            .map(profile -> new PlayerEntry(profile.name(), profile.uuid(), Instant.now().getEpochSecond()));
    }

    public static Optional<PlayerEntry> createPlayerListEntry(final UUID uuid) {
        return getProfileFromUUID(uuid)
            .map(profile -> new PlayerEntry(profile.name(), profile.uuid(), Instant.now().getEpochSecond()));
    }

    public static Optional<ProfileData> getProfileFromUsername(final String username) {
        return MojangApi.INSTANCE.getProfile(username).map(o -> (ProfileData) o)
            .or(() -> CraftheadApi.INSTANCE.getProfile(username).map(o -> (ProfileData) o)
                .or(() -> MinetoolsApi.INSTANCE.getProfileFromUsername(username)));
    }

    public static Optional<ProfileData> getProfileFromUUID(final UUID uuid) {
        return SessionServerApi.INSTANCE.getProfile(uuid).map(o -> (ProfileData) o)
            .or(() -> CraftheadApi.INSTANCE.getProfile(uuid).map(o -> (ProfileData) o)
                .or(() -> MinetoolsApi.INSTANCE.getProfileFromUUID(uuid)));
    }
}
