package com.zenith.util;


import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.mc.auth.service.ProfileService;
import com.zenith.Proxy;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static com.zenith.util.Constants.*;
import static java.util.Objects.nonNull;

public class WhitelistManager {
    private final ScheduledExecutorService whitelistRefreshExecutorService;
    private final Random random;
    private ScheduledFuture<?> refreshScheduledFuture;

    public WhitelistManager() {
        this.random = new Random();
        this.whitelistRefreshExecutorService = Executors.newSingleThreadScheduledExecutor();
        if (CONFIG.server.extra.whitelist.whitelistRefresh) {
            startRefreshTask();
        }
    }

    public void startRefreshTask() {
        stopRefreshTask();
        refreshScheduledFuture = whitelistRefreshExecutorService.scheduleAtFixedRate(this::refreshWhitelistEntries,
                random.nextInt(Math.max(1, (int) (CONFIG.server.extra.whitelist.whitelistRefreshIntervalMins / 2))),
                Math.max(10L, CONFIG.server.extra.whitelist.whitelistRefreshIntervalMins),
                TimeUnit.MINUTES);
    }

    public void stopRefreshTask() {
        if (nonNull(refreshScheduledFuture)) {
            refreshScheduledFuture.cancel(true);
        }
    }

    public boolean addWhitelistEntryByUsername(final String username) {
        final Optional<WhitelistEntry> whitelistEntryOptional = getWhitelistEntryFromUsername(username);
        if (whitelistEntryOptional.isPresent()) {
            final WhitelistEntry whitelistEntry = whitelistEntryOptional.get();
            if (!CONFIG.server.extra.whitelist.whitelist.contains(whitelistEntry)) {
                CONFIG.server.extra.whitelist.whitelist.add(whitelistEntry);
            }
            return true;
        }
        return false;
    }

    public void removeWhitelistEntryByUsername(final String username) {
        CONFIG.server.extra.whitelist.whitelist
                .removeIf(whitelistEntry -> whitelistEntry.username.equalsIgnoreCase(username));
    }

    public void clearWhitelist() {
        CONFIG.server.extra.whitelist.whitelist.clear();
    }

    public boolean addSpectatorWhitelistEntryByUsername(final String username) {
        final Optional<WhitelistEntry> whitelistEntryOptional = getWhitelistEntryFromUsername(username);
        if (whitelistEntryOptional.isPresent()) {
            final WhitelistEntry whitelistEntry = whitelistEntryOptional.get();
            if (!CONFIG.server.spectator.whitelist.contains(whitelistEntry)) {
                CONFIG.server.spectator.whitelist.add(whitelistEntry);
            }
            return true;
        }
        return false;
    }

    public void removeSpectatorWhitelistEntryByUsername(final String username) {
        CONFIG.server.spectator.whitelist.removeIf(s -> s.username.equalsIgnoreCase(username));
    }

    public void clearSpectatorWhitelist() {
        CONFIG.server.spectator.whitelist.clear();
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

    public void refreshWhitelistEntries() {
        SERVER_LOG.info("Refreshing whitelist entries...");
        CONFIG.server.extra.whitelist.whitelist.forEach(this::refreshWhitelistEntry);
        CONFIG.server.spectator.whitelist.forEach(this::refreshWhitelistEntry);
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
