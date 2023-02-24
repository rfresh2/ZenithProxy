package com.zenith.database;

import com.collarmc.pounce.Subscribe;
import com.zenith.cache.data.tab.PlayerEntry;
import com.zenith.database.dto.tables.Deaths;
import com.zenith.database.dto.tables.records.DeathsRecord;
import com.zenith.event.proxy.DeathMessageEvent;
import com.zenith.util.WhitelistEntry;
import com.zenith.util.deathmessages.DeathMessageParseResult;
import com.zenith.util.deathmessages.DeathMessagesParser;
import com.zenith.util.deathmessages.Killer;
import com.zenith.util.deathmessages.KillerType;
import org.jooq.DSLContext;
import org.jooq.InsertSetMoreStep;
import org.jooq.Result;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static com.zenith.util.Constants.*;

public class DeathsDatabase extends LockingDatabase {
    private final DeathMessagesParser deathMessagesHelper;

    public DeathsDatabase(final QueryExecutor queryExecutor, final RedisClient redisClient) {
        super(queryExecutor, redisClient);
        this.deathMessagesHelper = new DeathMessagesParser();
    }

    @Override
    public String getLockKey() {
        return "Deaths";
    }

    @Override
    public Instant getLastEntryTime() {
        final DSLContext context = DSL.using(SQLDialect.POSTGRES);
        final Deaths c = Deaths.DEATHS;
        final Result<DeathsRecord> recordResult = this.queryExecutor.fetch(context.selectFrom(c)
                .orderBy(c.TIME.desc())
                .limit(1));
        if (recordResult.isEmpty()) {
            DATABASE_LOG.warn("Deaths database unable to sync. Database empty?");
            return Instant.EPOCH;
        }
        final DeathsRecord deathsRecord = recordResult.get(0);
        return deathsRecord.getTime().toInstant();
    }

    @Subscribe
    public void handleDeathMessageEvent(DeathMessageEvent event) {
        if (!CONFIG.client.server.address.endsWith("2b2t.org")) return;
        final Optional<DeathMessageParseResult> deathMessageParseResult = deathMessagesHelper.parse(event.mcTextRoot.toRawString());
        deathMessageParseResult.ifPresent(d -> writeDeath(d, event.message, Instant.now().atOffset(ZoneOffset.UTC)));
    }

    private void writeDeath(final DeathMessageParseResult deathMessageParseResult, final String rawDeathMessage, final OffsetDateTime time) {
        try {
            final DSLContext context = DSL.using(SQLDialect.POSTGRES);
            final Deaths d = Deaths.DEATHS;
            final Optional<PlayerEntry> victimEntry = getPlayerEntryFromNameWithFallback(deathMessageParseResult.getVictim());
            if (!victimEntry.isPresent()) {
                DATABASE_LOG.error("Unable to resolve victim player data: {}", deathMessageParseResult.getVictim());
                return;
            }
            final InsertSetMoreStep<DeathsRecord> query = context.insertInto(d)
                    .set(d.TIME, time)
                    .set(d.DEATH_MESSAGE, rawDeathMessage)
                    .set(d.VICTIM_PLAYER_NAME, victimEntry.get().getName())
                    .set(d.VICTIM_PLAYER_UUID, victimEntry.get().getId());
            if (deathMessageParseResult.getKiller().isPresent()) {
                final Killer killer = deathMessageParseResult.getKiller().get();
                if (killer.getType().equals(KillerType.PLAYER)) {
                    final Optional<PlayerEntry> killerEntry = getPlayerEntryFromNameWithFallback(killer.getName());
                    if (!killerEntry.isPresent()) {
                        query
                                .set(d.KILLER_PLAYER_NAME, killerEntry.get().getName());
                        DATABASE_LOG.error("Unable to resolve killer player data: {}", deathMessageParseResult.getKiller());
                        return;
                    } else {
                        query
                                .set(d.KILLER_PLAYER_NAME, killerEntry.get().getName())
                                .set(d.KILLER_PLAYER_UUID, killerEntry.get().getId());
                    }
                } else if (killer.getType().equals(KillerType.MOB)) {
                    query
                            .set(d.KILLER_MOB, killer.getName());
                }
            }
            if (deathMessageParseResult.getWeapon().isPresent()) {
                query.set(d.WEAPON_NAME, deathMessageParseResult.getWeapon().get());
            }
            this.insert(time.toInstant(), query);
        } catch (final Exception e) {
            DATABASE_LOG.error("Error writing death: {}", rawDeathMessage, e);
        }
    }

    private Optional<PlayerEntry> getPlayerEntryFromNameWithFallback(final String username) {
        Optional<PlayerEntry> tablistEntry = CACHE.getTabListCache().getTabList().getFromName(username);
        if (tablistEntry.isPresent()) {
            return tablistEntry;
        } else {
            // note: this doesn't actually add them to the whitelist, just using this as a convenience function
            final Optional<WhitelistEntry> whitelistEntryFromUsername = WHITELIST_MANAGER.getWhitelistEntryFromUsername(username);
            if (whitelistEntryFromUsername.isPresent()) {
                return Optional.of(new PlayerEntry(whitelistEntryFromUsername.get().username, whitelistEntryFromUsername.get().uuid));
            }
        }
        return Optional.empty();
    }
}
