package com.zenith.database;

import com.zenith.Proxy;
import com.zenith.database.dto.records.DeathsRecord;
import com.zenith.event.proxy.DeathMessageEvent;
import com.zenith.feature.api.ProfileData;
import com.zenith.feature.deathmessages.DeathMessageParseResult;
import com.zenith.feature.deathmessages.Killer;
import com.zenith.feature.deathmessages.KillerType;
import com.zenith.feature.whitelist.PlayerListsManager;
import org.geysermc.mcprotocollib.protocol.data.game.PlayerListEntry;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static com.zenith.Shared.*;

public class DeathsDatabase extends LiveDatabase {
    public DeathsDatabase(final QueryExecutor queryExecutor, final RedisClient redisClient) {
        super(queryExecutor, redisClient);
    }

    @Override
    public void subscribeEvents() {
        EVENT_BUS.subscribe(this,
            DeathMessageEvent.class, this::handleDeathMessageEvent
        );
    }

    @Override
    public String getLockKey() {
        return "Deaths";
    }

    @Override
    public Instant getLastEntryTime() {
        try (var handle = this.queryExecutor.getJdbi().open()) {
            var result = handle.select("SELECT time FROM deaths ORDER BY time DESC LIMIT 1;")
                .mapTo(OffsetDateTime.class)
                .findOne();
            if (result.isEmpty()) {
                DATABASE_LOG.warn("Deaths database unable to sync. Database empty?");
                return Instant.EPOCH;
            }
            return result.get().toInstant();
        }
    }

    public void handleDeathMessageEvent(DeathMessageEvent event) {
        if (!Proxy.getInstance().isOn2b2t()) return;
        writeDeath(event.deathMessageParseResult(), event.deathMessageRaw(), Instant.now().atOffset(ZoneOffset.UTC));
    }

    private void writeDeath(final DeathMessageParseResult deathMessageParseResult, final String rawDeathMessage, final OffsetDateTime time) {
        final Optional<PlayerListEntry> victimEntry = getPlayerEntryFromNameWithFallback(deathMessageParseResult.getVictim());
        if (victimEntry.isEmpty()) {
            DATABASE_LOG.error("Unable to resolve victim player data: {}", deathMessageParseResult.getVictim());
            return;
        }
        var victimPlayerName = victimEntry.get().getName();
        var victimPlayerUuid = victimEntry.get().getProfileId();
        var pojo = new DeathsRecord(time, rawDeathMessage, victimPlayerName, victimPlayerUuid, null, null, null, null);

        if (deathMessageParseResult.getKiller().isPresent()) {
            final Killer killer = deathMessageParseResult.getKiller().get();
            if (killer.getType().equals(KillerType.PLAYER)) {
                final Optional<PlayerListEntry> killerEntry = getPlayerEntryFromNameWithFallback(killer.getName());
                if (killerEntry.isEmpty()) {
                    pojo
                        .setKillerPlayerName(killer.getName());
                    DATABASE_LOG.error("Unable to resolve killer player data: {}", deathMessageParseResult.getKiller());
                } else {
                    pojo
                        .setKillerPlayerName(killerEntry.get().getName())
                        .setKillerPlayerUuid(killerEntry.get().getProfileId());
                }
            } else if (killer.getType().equals(KillerType.MOB)) {
                pojo
                    .setKillerMob(killer.getName());
            }
        }
        if (deathMessageParseResult.getWeapon().isPresent()) {
            pojo.setWeaponName(deathMessageParseResult.getWeapon().get());
        }
        this.insert(time.toInstant(), pojo, handle ->
            handle.createUpdate("INSERT INTO deaths (time, death_message, victim_player_name, victim_player_uuid, killer_player_name, killer_player_uuid, weapon_name, killer_mob) VALUES (:time, :deathMessage, :victimPlayerName, :victimPlayerUuid, :killerPlayerName, :killerPlayerUuid, :weaponName, :killerMob)")
                .bind("time", pojo.getTime())
                .bind("deathMessage", pojo.getDeathMessage())
                .bind("victimPlayerName", pojo.getVictimPlayerName())
                .bind("victimPlayerUuid", pojo.getVictimPlayerUuid())
                .bind("killerPlayerName", pojo.getKillerPlayerName())
                .bind("killerPlayerUuid", pojo.getKillerPlayerUuid())
                .bind("weaponName", pojo.getWeaponName())
                .bind("killerMob", pojo.getKillerMob())
                .execute()
        );
    }

    private Optional<PlayerListEntry> getPlayerEntryFromNameWithFallback(final String username) {
        Optional<PlayerListEntry> tablistEntry = CACHE.getTabListCache().getFromName(username);
        if (tablistEntry.isPresent()) {
            return tablistEntry;
        } else {
            // note: this doesn't actually add them to the whitelist, just using this as a convenience function
            final Optional<ProfileData> profileData = PlayerListsManager.getProfileFromUsername(username);
            if (profileData.isPresent()) {
                return Optional.of(new PlayerListEntry(profileData.get().name(), profileData.get().uuid()));
            }
        }
        return Optional.empty();
    }
}
