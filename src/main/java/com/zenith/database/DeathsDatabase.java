package com.zenith.database;

import com.github.steveice10.mc.protocol.data.game.PlayerListEntry;
import com.zenith.database.dto.tables.Deaths;
import com.zenith.database.dto.tables.records.DeathsRecord;
import com.zenith.event.Subscription;
import com.zenith.event.proxy.DeathMessageEvent;
import com.zenith.feature.api.ProfileData;
import com.zenith.feature.deathmessages.DeathMessageParseResult;
import com.zenith.feature.deathmessages.Killer;
import com.zenith.feature.deathmessages.KillerType;
import com.zenith.feature.whitelist.PlayerList;
import org.jooq.DSLContext;
import org.jooq.Result;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

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
    public Subscription subscribeEvents() {
        return EVENT_BUS.subscribe(
            DeathMessageEvent.class, this::handleDeathMessageEvent
        );
    }

    @Override
    public String getLockKey() {
        return "Deaths";
    }

    @Override
    public Instant getLastEntryTime() {
        final DSLContext context = DSL.using(SQLDialect.POSTGRES);
        final Deaths d = Deaths.DEATHS;
        final Result<DeathsRecord> recordResult = this.queryExecutor.fetch(context.selectFrom(d)
                .orderBy(d.TIME.desc())
                .limit(1));
        if (recordResult.isEmpty()) {
            DATABASE_LOG.warn("Deaths database unable to sync. Database empty?");
            return Instant.EPOCH;
        }
        final DeathsRecord deathsRecord = recordResult.get(0);
        return deathsRecord.get(d.TIME).toInstant();
    }

    public void handleDeathMessageEvent(DeathMessageEvent event) {
        if (!CONFIG.client.server.address.endsWith("2b2t.org")) return;
        writeDeath(event.deathMessageParseResult(), event.deathMessageRaw(), Instant.now().atOffset(ZoneOffset.UTC));
    }

    private void writeDeath(final DeathMessageParseResult deathMessageParseResult, final String rawDeathMessage, final OffsetDateTime time) {
        try {
            final DSLContext context = DSL.using(SQLDialect.POSTGRES);
            final Deaths d = Deaths.DEATHS;
            final Optional<PlayerListEntry> victimEntry = getPlayerEntryFromNameWithFallback(deathMessageParseResult.getVictim());
            if (victimEntry.isEmpty()) {
                DATABASE_LOG.error("Unable to resolve victim player data: {}", deathMessageParseResult.getVictim());
                return;
            }
            final DeathsRecord record = context.newRecord(d)
                    .setTime(time)
                    .setDeathMessage(rawDeathMessage)
                    .setVictimPlayerName(victimEntry.get().getName())
                    .setVictimPlayerUuid(victimEntry.get().getProfileId());
            if (deathMessageParseResult.getKiller().isPresent()) {
                final Killer killer = deathMessageParseResult.getKiller().get();
                if (killer.getType().equals(KillerType.PLAYER)) {
                    final Optional<PlayerListEntry> killerEntry = getPlayerEntryFromNameWithFallback(killer.getName());
                    if (killerEntry.isEmpty()) {
                        record
                                .setKillerPlayerName(killerEntry.get().getName());
                        DATABASE_LOG.error("Unable to resolve killer player data: {}", deathMessageParseResult.getKiller());
                    } else {
                        record
                                .setKillerPlayerName(killerEntry.get().getName())
                                .setKillerPlayerUuid(killerEntry.get().getProfileId());
                    }
                } else if (killer.getType().equals(KillerType.MOB)) {
                    record
                            .setKillerMob(killer.getName());
                }
            }
            if (deathMessageParseResult.getWeapon().isPresent()) {
                record.setWeaponName(deathMessageParseResult.getWeapon().get());
            }
            var query = context.insertInto(d)
                .set(record);
            this.insert(time.toInstant(),
                        record.into(com.zenith.database.dto.tables.pojos.Deaths.class),
                        query);
        } catch (final Exception e) {
            DATABASE_LOG.error("Error writing death: {}", rawDeathMessage, e);
        }
    }

    private Optional<PlayerListEntry> getPlayerEntryFromNameWithFallback(final String username) {
        Optional<PlayerListEntry> tablistEntry = CACHE.getTabListCache().getFromName(username);
        if (tablistEntry.isPresent()) {
            return tablistEntry;
        } else {
            // note: this doesn't actually add them to the whitelist, just using this as a convenience function
            final Optional<ProfileData> profileData = PlayerList.getProfileFromUsername(username);
            if (profileData.isPresent()) {
                return Optional.of(new PlayerListEntry(profileData.get().name(), profileData.get().uuid()));
            }
        }
        return Optional.empty();
    }
}
