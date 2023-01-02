package com.zenith.database;

import com.collarmc.pounce.Subscribe;
import com.zenith.cache.data.tab.PlayerEntry;
import com.zenith.database.dto.tables.Deaths;
import com.zenith.database.dto.tables.records.DeathsRecord;
import com.zenith.event.proxy.DeathMessageEvent;
import com.zenith.util.WhitelistEntry;
import com.zenith.util.deathmessages.DeathMessageParseResult;
import com.zenith.util.deathmessages.DeathMessagesParser;
import org.jooq.DSLContext;
import org.jooq.InsertSetMoreStep;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static com.zenith.util.Constants.*;

public class DeathsDatabase extends Database {
    private final DeathMessagesParser deathMessagesHelper;

    public DeathsDatabase(final QueryQueue queryQueue) {
        super(queryQueue);
        this.deathMessagesHelper = new DeathMessagesParser();
    }

    @Subscribe
    public void handleDeathMessageEvent(DeathMessageEvent event) {
        if (!CONFIG.client.server.address.endsWith("2b2t.org")) return;
        final Optional<DeathMessageParseResult> deathMessageParseResult = deathMessagesHelper.parse(event.mcTextRoot);
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
                final Optional<PlayerEntry> killerEntry = getPlayerEntryFromNameWithFallback(deathMessageParseResult.getKiller().get());
                if (!killerEntry.isPresent()) {
                    DATABASE_LOG.error("Unable to resolve killer player data: {}", deathMessageParseResult.getKiller());
                    return;
                }
                query
                        .set(d.KILLER_PLAYER_NAME, killerEntry.get().getName())
                        .set(d.KILLER_PLAYER_UUID, killerEntry.get().getId());
            }
            if (deathMessageParseResult.getWeapon().isPresent()) {
                query.set(d.WEAPON_NAME, deathMessageParseResult.getWeapon().get());
            }
            queryQueue.add(query);
        } catch (final Exception e) {
            if (e.getMessage().contains("violates exclusion constraint") || e.getMessage().contains("deadlock detected")) {
                // expected due to multiple proxies writing the same death
            } else {
                DATABASE_LOG.debug("Error writing death: {}", rawDeathMessage, e);
            }
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
