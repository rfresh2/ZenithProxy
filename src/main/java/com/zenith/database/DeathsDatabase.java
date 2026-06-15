package com.zenith.database;

import com.zenith.Proxy;
import com.zenith.database.dto.records.DeathsFeedRecord;
import com.zenith.event.chat.DeathMessageChatEvent;
import com.zenith.feature.api.ProfileData;
import com.zenith.feature.deathmessages.DeathMessageParseResult;
import com.zenith.feature.deathmessages.Killer;
import com.zenith.feature.deathmessages.KillerType;
import com.zenith.feature.whitelist.PlayerListsManager;
import com.zenith.util.ComponentSerializer;
import net.kyori.adventure.text.Component;
import org.geysermc.mcprotocollib.protocol.data.game.PlayerListEntry;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static com.zenith.Globals.*;

public class DeathsDatabase extends LiveDatabase {
    public DeathsDatabase(final QueryExecutor queryExecutor, final RedisClient redisClient) {
        super(queryExecutor, redisClient);
    }

    @Override
    public void subscribeEvents() {
        EVENT_BUS.subscribe(
            this,
            DeathMessageChatEvent.class, this::handleDeathMessageEvent
        );
    }

    @Override
    public String getLockKey() {
        return "Deaths";
    }

    @Override
    public Instant getLastEntryTime() {
        try (var handle = this.queryExecutor.jdbi().open()) {
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

    public void handleDeathMessageEvent(DeathMessageChatEvent event) {
        if (!Proxy.getInstance().isOn2b2t()) return;
        writeDeath(event.deathMessage(), event.message(), Instant.now().atOffset(ZoneOffset.UTC), event.component());
    }

    private void writeDeath(final DeathMessageParseResult deathMessageParseResult, final String rawDeathMessage, final OffsetDateTime time, final Component component) {
        final Optional<PlayerListEntry> victimEntry = getPlayerEntryFromNameWithFallback(deathMessageParseResult.victim());
        if (victimEntry.isEmpty()) {
            DATABASE_LOG.error("Unable to resolve victim player data: {}", deathMessageParseResult.victim());
            return;
        }
        String killerPlayerName = null;
        UUID killerPlayerUuid = null;
        var victimPlayerName = victimEntry.get().getName();
        var victimPlayerUuid = victimEntry.get().getProfileId();
        String killerMob = null;
        String weaponName = null;
        if (deathMessageParseResult.killer().isPresent()) {
            final Killer killer = deathMessageParseResult.killer().get();
            if (killer.type().equals(KillerType.PLAYER)) {
                final Optional<PlayerListEntry> killerEntry = getPlayerEntryFromNameWithFallback(killer.name());
                if (killerEntry.isEmpty()) {
                    killerPlayerName = killer.name();
                    DATABASE_LOG.error("Unable to resolve killer player data: {}", deathMessageParseResult.killer());
                } else {
                    killerPlayerName = killerEntry.get().getName();
                    killerPlayerUuid = killerEntry.get().getProfileId();
                }
            } else if (killer.type().equals(KillerType.MOB)) {
                killerMob = killer.name();
            }
        }
        if (deathMessageParseResult.weapon().isPresent()) {
            weaponName = deathMessageParseResult.weapon().get();
        }
        var pojo = new DeathsFeedRecord(
            time,
            rawDeathMessage,
            victimPlayerName,
            victimPlayerUuid,
            killerPlayerName,
            killerPlayerUuid,
            weaponName,
            killerMob,
            ComponentSerializer.serializeJson(component)
        );
        this.insert(
            time.toInstant(),
            pojo,
            handle -> {
                handle.createUpdate(
                        "INSERT INTO deaths (time, death_message, victim_player_name, victim_player_uuid, killer_player_name, killer_player_uuid, weapon_name, killer_mob) VALUES (:time, :deathMessage, :victimPlayerName, :victimPlayerUuid, :killerPlayerName, :killerPlayerUuid, :weaponName, :killerMob)")
                    .bind("time", pojo.time())
                    .bind("deathMessage", pojo.deathMessage())
                    .bind("victimPlayerName", pojo.victimPlayerName())
                    .bind("victimPlayerUuid", pojo.victimPlayerUuid())
                    .bind("killerPlayerName", pojo.killerPlayerName())
                    .bind("killerPlayerUuid", pojo.killerPlayerUuid())
                    .bind("weaponName", pojo.weaponName())
                    .bind("killerMob", pojo.killerMob())
                    .execute();
            }
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
