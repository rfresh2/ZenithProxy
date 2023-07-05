package com.zenith.feature.whitelist;


import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.mc.auth.service.ProfileService;
import com.zenith.Proxy;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static com.zenith.Shared.*;
import static java.util.Objects.nonNull;

public class WhitelistManager {
    private final Random random;
    private ScheduledFuture<?> refreshScheduledFuture;

    public WhitelistManager() {
        this.random = new Random();
        if (CONFIG.server.extra.whitelist.whitelistRefresh) {
            startRefreshTask();
        }
    }

    public void startRefreshTask() {
        stopRefreshTask();
        refreshScheduledFuture = SCHEDULED_EXECUTOR_SERVICE.scheduleAtFixedRate(this::refreshWhitelistEntries,
                random.nextInt(Math.max(1, (int) (CONFIG.server.extra.whitelist.whitelistRefreshIntervalMins / 2))),
                Math.max(10L, CONFIG.server.extra.whitelist.whitelistRefreshIntervalMins),
                TimeUnit.MINUTES);
    }

    public void stopRefreshTask() {
        if (nonNull(refreshScheduledFuture)) {
            refreshScheduledFuture.cancel(true);
        }
    }

    public Optional<WhitelistEntry> addWhitelistEntryByUsername(final String username) {
        return addWhitelistEntryByUsernameBase(username, CONFIG.server.extra.whitelist.whitelist);
    }

    public void removeWhitelistEntryByUsername(final String username) {
        CONFIG.server.extra.whitelist.whitelist
                .removeIf(whitelistEntry -> whitelistEntry.username.equalsIgnoreCase(username));
    }

    public void clearWhitelist() {
        CONFIG.server.extra.whitelist.whitelist.clear();
    }

    public Optional<WhitelistEntry> addSpectatorWhitelistEntryByUsername(final String username) {
        return addWhitelistEntryByUsernameBase(username, CONFIG.server.spectator.whitelist);
    }

    public void removeSpectatorWhitelistEntryByUsername(final String username) {
        CONFIG.server.spectator.whitelist.removeIf(s -> s.username.equalsIgnoreCase(username));
    }

    public void clearSpectatorWhitelist() {
        CONFIG.server.spectator.whitelist.clear();
    }

    public Optional<WhitelistEntry> addFriendWhitelistEntryByUsername(final String username) {
        return addWhitelistEntryByUsernameBase(username, CONFIG.client.extra.friendsList);
    }

    public void removeFriendWhitelistEntryByUsername(final String username) {
        CONFIG.client.extra.friendsList.removeIf(s -> s.username.equalsIgnoreCase(username));
    }

    public void clearFriendWhitelist() {
        CONFIG.client.extra.friendsList.clear();
    }

    public Optional<WhitelistEntry> addIgnoreWhitelistEntryByUsername(final String username) {
        return addWhitelistEntryByUsernameBase(username, CONFIG.client.extra.chat.ignoreList);
    }

    public void removeIgnoreWhitelistEntryByUsername(final String username) {
        CONFIG.client.extra.chat.ignoreList.removeIf(s -> s.username.equalsIgnoreCase(username));
    }

    public void clearIgnoreWhitelist() {
        CONFIG.client.extra.chat.ignoreList.clear();
    }

    private Optional<WhitelistEntry> addWhitelistEntryByUsernameBase(final String username, final List<WhitelistEntry> destList) {
        final Optional<WhitelistEntry> whitelistEntryOptional = getWhitelistEntryFromUsername(username);
        if (whitelistEntryOptional.isPresent()) {
            final WhitelistEntry whitelistEntry = whitelistEntryOptional.get();
            if (!destList.contains(whitelistEntry)) {
                destList.add(whitelistEntry);
            }
        }
        return whitelistEntryOptional;
    }

    public boolean isProfileWhitelisted(final GameProfile clientGameProfile) {
        final Optional<WhitelistEntry> whitelistedOptional = CONFIG.server.extra.whitelist.whitelist.stream()
                .filter(whitelistEntry -> whitelistEntry.uuid.equals(clientGameProfile.getId()))
                .findFirst();
        if (whitelistedOptional.isPresent()) {
            // player is whitelisted
            // let's update their player profile
            whitelistedOptional.get().lastRefreshed = Instant.now().getEpochSecond();
            whitelistedOptional.get().username = clientGameProfile.getName();
            return true;
        } else {
            return false;
        }
    }

    public boolean isUUIDWhitelisted(final UUID uuid) {
        return CONFIG.server.extra.whitelist.whitelist.stream()
                .map(entry -> entry.uuid)
                .anyMatch(id -> Objects.equals(id, uuid));
    }

    public boolean isUserNameWhitelisted(final String name) {
        return CONFIG.server.extra.whitelist.whitelist.stream()
                .map(entry -> entry.username)
                .anyMatch(name::equalsIgnoreCase);
    }

    public boolean isUUIDFriendWhitelisted(final UUID uuid) {
        return CONFIG.client.extra.friendsList.stream().anyMatch(entry -> Objects.equals(entry.uuid, uuid));
    }

    public boolean isProfileSpectatorWhitelisted(final GameProfile clientGameProfile) {
        final Optional<WhitelistEntry> whitelistedOptional = CONFIG.server.spectator.whitelist.stream()
                .filter(whitelistEntry -> whitelistEntry.uuid.equals(clientGameProfile.getId()))
                .findFirst();
        if (whitelistedOptional.isPresent()) {
            // player is whitelisted
            // let's update their player profile
            whitelistedOptional.get().lastRefreshed = Instant.now().getEpochSecond();
            whitelistedOptional.get().username = clientGameProfile.getName();
            return true;
        } else {
            return false;
        }
    }

    public boolean isUUIDSpectatorWhitelisted(final UUID uuid) {
        return CONFIG.server.spectator.whitelist.stream()
                .map(entry -> entry.uuid)
                .anyMatch(id -> Objects.equals(id, uuid));
    }

    public boolean isUserNameSpectatorWhitelisted(final String name) {
        return CONFIG.server.spectator.whitelist.stream()
                .map(entry -> entry.username)
                .anyMatch(name::equalsIgnoreCase);
    }

    public boolean isPlayerIgnored(final String name) {
        return CONFIG.client.extra.chat.ignoreList.stream()
                .map(entry -> entry.username)
                .anyMatch(name::equalsIgnoreCase);
    }

    public void refreshWhitelistEntries() {
        SERVER_LOG.info("Refreshing whitelist entries...");
        CONFIG.server.extra.whitelist.whitelist.forEach(this::refreshWhitelistEntry);
        CONFIG.server.spectator.whitelist.forEach(this::refreshWhitelistEntry);
        CONFIG.client.extra.friendsList.forEach(this::refreshWhitelistEntry);
        SERVER_LOG.info("Whitelist refresh complete");
        saveConfig();
    }

    public void kickNonWhitelistedPlayers() {
        Proxy.getInstance().getActiveConnections().stream()
                .filter(con -> nonNull(con.getProfileCache().getProfile()))
                .filter(con -> !WHITELIST_MANAGER.isProfileWhitelisted(con.getProfileCache().getProfile()))
                .filter(con -> !(WHITELIST_MANAGER.isProfileSpectatorWhitelisted(con.getProfileCache().getProfile()) && con.isSpectator()))
                .forEach(con -> con.disconnect("Not whitelisted"));
    }

    private void refreshWhitelistEntry(final WhitelistEntry whitelistEntry) {
        final Optional<WhitelistEntry> whitelistEntryFromUUID = getWhitelistEntryFromUUID(whitelistEntry.uuid);
        if (whitelistEntryFromUUID.isPresent()) {
            // in-place update
            whitelistEntry.username = whitelistEntryFromUUID.get().username;
            whitelistEntry.lastRefreshed = Instant.now().getEpochSecond();
        } else {
            SERVER_LOG.error("Whitelist refresh: unable to refresh player with username: {} and uuid: {}", whitelistEntry.username, whitelistEntry.uuid.toString());
            // todo: remove from whitelist?
            //  send notification in discord?
            // most likely would be caused by throttled network requests to mojang API
            // there may also be some state where the account gets deleted
        }
    }

    private Optional<WhitelistEntry> getWhitelistEntryFromUUID(final UUID uuid) {
        final ProfileService profileService = new ProfileService();
        final Optional<GameProfile> profileOptional = Optional.ofNullable(profileService.findProfileByUUID(uuid));
        if (profileOptional.isPresent()) {
            final GameProfile gameProfile = profileOptional.get();
            return Optional.of(new WhitelistEntry(gameProfile.getName(), gameProfile.getId(), Instant.now().getEpochSecond()));
        } else {
            return Optional.empty();
        }
    }

    public Optional<WhitelistEntry> getWhitelistEntryFromUsername(final String username) {
        final ProfileService profileService = new ProfileService();
        int tries = 0;
        final SingleLookupProfileLookupCallbackHelper callbackHelper = new SingleLookupProfileLookupCallbackHelper();
        while (tries++ < 3 && !callbackHelper.completed) {
            profileService.findProfilesByName(new String[]{username}, callbackHelper, false);
            if (callbackHelper.completed && !callbackHelper.errorException.isPresent() && callbackHelper.gameProfile.isPresent()) {
                GameProfile gameProfile = callbackHelper.gameProfile.get();
                return Optional.of(new WhitelistEntry(gameProfile.getName(), gameProfile.getId(), Instant.now().getEpochSecond()));
            } else {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        callbackHelper.errorException
                .ifPresent(e -> SERVER_LOG.error("Failed getting WhitelistEntry for username: {}", username, e));
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
