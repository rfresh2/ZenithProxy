package com.zenith.feature.whitelist;

import com.zenith.feature.api.ProfileData;
import com.zenith.feature.api.crafthead.CraftheadApi;
import com.zenith.feature.api.mcprofile.MCProfileApi;
import com.zenith.feature.api.minetools.MinetoolsApi;
import com.zenith.feature.api.mojang.MojangApi;
import com.zenith.feature.api.sessionserver.SessionServerApi;
import com.zenith.util.Wait;
import lombok.Getter;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.zenith.Globals.*;
import static com.zenith.util.BedrockUtil.isBedrock;

@Getter
public class PlayerListsManager {
    private final List<PlayerList> playerLists = new ArrayList<>();
    private PlayerList whitelist;
    private PlayerList blacklist;
    private PlayerList spectatorWhitelist;
    private PlayerList friendsList;
    private PlayerList ignoreList;
    private PlayerList stalkList;
    private PlayerList spawnPatrolIgnoreList;
    private ScheduledFuture<?> refreshScheduledFuture;

    public void init() { // must be called after config is loaded
        whitelist = register(new PlayerList("whitelist", CONFIG.server.extra.whitelist.whitelist));
        blacklist = register(new PlayerList("blacklist", CONFIG.server.extra.whitelist.blacklist));
        spectatorWhitelist = register(new PlayerList("spectatorWhitelist", CONFIG.server.spectator.whitelist));
        friendsList = register(new PlayerList("friendsList", CONFIG.client.extra.friendsList));
        ignoreList = register(new PlayerList("ignoreList", CONFIG.client.extra.chat.ignoreList));
        stalkList = register(new PlayerList("stalkList", CONFIG.client.extra.stalk.stalking));
        spawnPatrolIgnoreList = register(new PlayerList("spawnPatrolIgnoreList", CONFIG.client.extra.spawnPatrol.ignoreList));
        startRefreshTask();
    }

    public PlayerList register(PlayerList playerList) {
        playerLists.add(playerList);
        return playerList;
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
        var playerEntryList = getPlayerLists().stream()
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
        if (isBedrock(username)) {
            return MCProfileApi.INSTANCE.getBedrockProfile(username.replace(".", "")).map(o -> (ProfileData) o).filter(PlayerListsManager::validProfile);
        }
        return MojangApi.INSTANCE.getProfile(username).map(o -> (ProfileData) o).filter(PlayerListsManager::validProfile)
            .or(() -> CraftheadApi.INSTANCE.getProfile(username).map(o -> (ProfileData) o).filter(PlayerListsManager::validProfile)
                .or(() -> MinetoolsApi.INSTANCE.getProfileFromUsername(username).filter(PlayerListsManager::validProfile)));
    }

    public static Optional<ProfileData> getProfileFromUUID(final UUID uuid) {
        if (isBedrock(uuid)) {
            return MCProfileApi.INSTANCE.getBedrockProfile(uuid).map(o -> (ProfileData) o).filter(PlayerListsManager::validProfile);
        }
        return SessionServerApi.INSTANCE.getProfile(uuid).map(o -> (ProfileData) o).filter(PlayerListsManager::validProfile)
            .or(() -> CraftheadApi.INSTANCE.getProfile(uuid).map(o -> (ProfileData) o).filter(PlayerListsManager::validProfile)
                .or(() -> MinetoolsApi.INSTANCE.getProfileFromUUID(uuid).filter(PlayerListsManager::validProfile)));
    }

    private static boolean validProfile(final ProfileData profile) {
        return profile != null && profile.uuid() != null && profile.name() != null;
    }
}
