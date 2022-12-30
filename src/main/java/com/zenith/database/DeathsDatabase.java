package com.zenith.database;

import com.collarmc.pounce.Subscribe;
import com.zenith.cache.data.tab.PlayerEntry;
import com.zenith.database.dto.tables.Deaths;
import com.zenith.database.dto.tables.records.DeathsRecord;
import com.zenith.event.proxy.DeathMessageEvent;
import com.zenith.util.DeathMessagesHelper;
import org.jooq.DSLContext;
import org.jooq.InsertSetMoreStep;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

import java.sql.Connection;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static com.zenith.util.Constants.*;

public class DeathsDatabase extends Database {
    private final DeathMessagesHelper deathMessagesHelper;

    public DeathsDatabase(ConnectionPool connectionPool) {
        super(connectionPool);
        this.deathMessagesHelper = new DeathMessagesHelper();
    }

    @Subscribe
    public void handleDeathMessageEvent(DeathMessageEvent event) {
        if (!CONFIG.client.server.address.endsWith("2b2t.org")) return;
        final Optional<DeathMessagesHelper.DeathMessageParseResult> deathMessageParseResult = deathMessagesHelper.parse(event.mcTextRoot);
        deathMessageParseResult.ifPresent(d -> writeDeath(d, event.message, Instant.now().atOffset(ZoneOffset.UTC)));
    }

    private void writeDeath(final DeathMessagesHelper.DeathMessageParseResult deathMessageParseResult, final String rawDeathMessage, final OffsetDateTime time) {
        try (final Connection connection = connectionPool.getWriteConnection()) {
            final DSLContext context = DSL.using(connection, SQLDialect.POSTGRES);
            final Deaths d = Deaths.DEATHS;
            // todo: known problem: if a victim logs right when they die they aren't in the tablist when we check, thus throwing an exception here
            //  we could query mojang API as fallback and get around this
            final Optional<PlayerEntry> victimEntry = CACHE.getTabListCache().getTabList().getFromName(deathMessageParseResult.getVictim());
            InsertSetMoreStep<DeathsRecord> step = context.insertInto(d)
                    .set(d.TIME, time)
                    .set(d.DEATH_MESSAGE, rawDeathMessage)
                    .set(d.VICTIM_PLAYER_NAME, victimEntry.get().getName())
                    .set(d.VICTIM_PLAYER_UUID, victimEntry.get().getId());
            if (deathMessageParseResult.getKiller().isPresent()) {
                final Optional<PlayerEntry> killerEntry = CACHE.getTabListCache().getTabList().getFromName(deathMessageParseResult.getKiller().get());
                step
                        .set(d.KILLER_PLAYER_NAME, killerEntry.get().getName())
                        .set(d.KILLER_PLAYER_UUID, killerEntry.get().getId());
            }
            if (deathMessageParseResult.getWeapon().isPresent()) {
                step.set(d.WEAPON_NAME, deathMessageParseResult.getWeapon().get());
            }
            step.execute();
        } catch (final Exception e) {
            if (e.getMessage().contains("violates exclusion constraint")) {
                // expected due to multiple proxies writing the same connection
                DATABASE_LOG.debug("death dedupe detected");
            } else {
                DATABASE_LOG.error("Error writing death", e);
            }
        }
    }
}
