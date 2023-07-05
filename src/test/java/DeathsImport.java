import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zenith.database.ConnectionPool;
import com.zenith.database.dto.tables.Connections;
import com.zenith.database.dto.tables.Names;
import com.zenith.database.dto.tables.pojos.Deaths;
import com.zenith.database.dto.tables.records.ConnectionsRecord;
import com.zenith.database.dto.tables.records.DeathsRecord;
import com.zenith.database.dto.tables.records.NamesRecord;
import com.zenith.feature.deathmessages.DeathMessageParseResult;
import com.zenith.feature.deathmessages.DeathMessagesParser;
import com.zenith.feature.deathmessages.KillerType;
import lombok.ToString;
import org.jooq.*;
import org.jooq.impl.DSL;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

import static com.zenith.Shared.DATABASE_LOG;
import static java.util.Objects.isNull;

public class DeathsImport {


    private int batchCount = 0;

    //    @Test
    public void importDeathsOldSchema() {
        final ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        final List<Deaths> buffer = new ArrayList<>();
        final int maxBuffer = 100;
        final DeathMessagesParser deathMessagesParser = new DeathMessagesParser();
        final ConnectionPool outConnectionPool = new ConnectionPool("jdbc:postgresql://localhost:5432/postgres", "postgres", "a");
        try (final Connection outConnection = outConnectionPool.getWriteConnection()) {
            final DSLContext outContext = DSL.using(outConnection, SQLDialect.POSTGRES);

            try (final InputStream is = new FileInputStream(new File("importData/2b2tbotchatmayjun.json"))) {
                try (final JsonParser jsonParser = objectMapper.getFactory().createParser(is)) {
                    while (!Objects.equals(jsonParser.nextFieldName(), "messages")) {
                        continue;
                    }
                    if (jsonParser.nextToken() == JsonToken.START_ARRAY) {
                        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
                            final DiscordMessage discordMessage = objectMapper.readValue(jsonParser, DiscordMessage.class);
                            if (!discordMessage.author.id.equals("756249692723216455")) {
                                continue;
                            }
                            if (discordMessage.embeds.size() != 1) {
                                continue;
                            }
                            final Instant time = OffsetDateTime.parse(discordMessage.timestamp).toInstant();
                            if (time.isAfter(Instant.ofEpochSecond(1669843274L))) {     // "2022-11-30T21:21:14.165+00:00"
                                continue;
                            }

                            final String desc = discordMessage.embeds.get(0).description.replace("\0", "").replace("\b", "");
                            final String[] spaceSplit = desc.split(" ");
                            if (spaceSplit.length < 2 || spaceSplit[1].startsWith("whispers:")) {
                                continue;
                            }
                            if (desc.startsWith("<")
                                    || desc.startsWith(":rotating_light:")
                                    || desc.startsWith("Position in queue")
                                    || desc.startsWith("Bad command.")
                                    || desc.startsWith("This player is not online")
                                    || desc.startsWith("[SERVER]")
                                    || desc.startsWith("to ")
                                    || (desc.endsWith("left the game") && spaceSplit.length == 4)
                                    || (desc.startsWith("Now ignoring") && spaceSplit.length == 3)
                                    || desc.endsWith("players have spawned at least once on the world.")
                                    || (desc.startsWith("world is") && desc.endsWith(" GB."))
                                    || (desc.endsWith("joined") && spaceSplit.length == 2)
                                    || (desc.endsWith("left") && spaceSplit.length == 2)
                                    || desc.startsWith("Permanently ignoring ")
                                    || desc.equals("2b2t is full")
                                    || desc.equals("Connecting to the server...")
                                    || desc.equals("You have lost connection to the server")
                                    || desc.startsWith("[ Ignored players")
                                    || desc.equals("No players ignored.")
                                    || desc.startsWith("Exception Connecting:")
                                    || desc.startsWith("An internal error occurred")
                                    || desc.startsWith("Bot kicked")
                                    || desc.startsWith("/")
                                    || desc.startsWith("test queue lengths")
                                    || desc.startsWith("main queue lengths")
                                    || desc.startsWith("Connected to main server")
                                    || desc.startsWith("You are already queued to server")
                                    || desc.equals("Connected to the server.")
                                    || desc.equals("Reconnecting to server main.")
                                    || desc.equals("You are already on server main.")
                                    || desc.equals("You need to be in queue for at least one server.")
                                    || desc.equals("Queueing for offline server main, you will remain in queue until the server is online.")
                                    || desc.startsWith("Server test requires")
                                    || desc.equals("Queued for server main.")
                            ) {
                                continue;
                            }

                            final Optional<DeathMessageParseResult> parse = deathMessagesParser.parse(desc);
                            if (parse.isPresent()) {
                                final String victim = parse.get().getVictim();
                                final UUID victimUuid = getPlayerUuid(time, parse.get().getVictim(), outContext);
                                final String killerName = (parse.get().getKiller().isPresent() && parse.get().getKiller().get().getType() == KillerType.PLAYER) ? parse.get().getKiller().get().getName() : null;
                                final UUID killerUuid = (parse.get().getKiller().isPresent() && parse.get().getKiller().get().getType() == KillerType.PLAYER) ? getPlayerUuid(time, parse.get().getKiller().get().getName(), outContext) : null;
                                final String weapon = parse.get().getWeapon().orElse(null);
                                final String mob = (parse.get().getKiller().isPresent() && parse.get().getKiller().get().getType() == KillerType.MOB) ? parse.get().getKiller().get().getName() : null;
                                final Deaths d = new Deaths(time.atOffset(ZoneOffset.UTC), desc,
                                        victim, victimUuid,
                                        killerName, killerUuid, weapon, mob);
                                buffer.add(d);
                                if (buffer.size() > maxBuffer) {
                                    writeBuffer(buffer, outContext);
                                    buffer.clear();
                                }
                            } else {
                                DATABASE_LOG.error("Unable to parse: {}", desc);
                            }
                        }
                    }
                    writeBuffer(buffer, outContext);
                    buffer.clear();
                }
            }

        } catch (final Exception e) {
            DATABASE_LOG.error("", e);
        }
    }

    //    @Test
    public void importDeathsNewSchema() {
        final ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        final List<Deaths> buffer = new ArrayList<>();
        final int maxBuffer = 100;
        final DeathMessagesParser deathMessagesParser = new DeathMessagesParser();
        final ConnectionPool outConnectionPool = new ConnectionPool("jdbc:postgresql://localhost:5432/postgres", "postgres", "a");
        try (final Connection outConnection = outConnectionPool.getWriteConnection()) {
            final DSLContext outContext = DSL.using(outConnection, SQLDialect.POSTGRES);

            try (final InputStream is = new FileInputStream(new File("importData/2b2tbotchatmayjun.json"))) {
                try (final JsonParser jsonParser = objectMapper.getFactory().createParser(is)) {
                    while (!Objects.equals(jsonParser.nextFieldName(), "messages")) {
                        continue;
                    }
                    if (jsonParser.nextToken() == JsonToken.START_ARRAY) {
                        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
                            final DiscordMessage discordMessage = objectMapper.readValue(jsonParser, DiscordMessage.class);
                            if (!discordMessage.author.id.equals("756249692723216455")) {
                                continue;
                            }
                            if (discordMessage.embeds.size() < 1) {
                                continue;
                            }
                            final Instant time = OffsetDateTime.parse(discordMessage.timestamp).toInstant();
                            if (time.isBefore(Instant.ofEpochSecond(1669843274L))) {
                                continue;
                            }
                            for (DiscordMessageEmbed discordMessageEmbed : discordMessage.embeds) {
                                if (isNull(discordMessageEmbed.color) || !discordMessageEmbed.color.equals("#AA0000")) {
                                    continue;
                                }
                                final Instant tTime = OffsetDateTime.parse(discordMessageEmbed.timestamp).toInstant();
                                final String desc = discordMessageEmbed.description.replace("\0", "").replace("\b", "").replace("`", "");
                                final String[] spaceSplit = desc.split(" ");
                                if (spaceSplit.length < 2 || spaceSplit[1].startsWith("whispers:")) {
                                    continue;
                                }
                                if (desc.startsWith("<")
                                        || desc.startsWith(":rotating_light:")
                                        || desc.startsWith("Position in queue")
                                        || desc.startsWith("Bad command.")
                                        || desc.startsWith("This player is not online")
                                        || desc.startsWith("[SERVER]")
                                        || desc.startsWith("to ")
                                        || (desc.endsWith("left the game") && spaceSplit.length == 4)
                                        || (desc.startsWith("Now ignoring") && spaceSplit.length == 3)
                                        || desc.endsWith("players have spawned at least once on the world.")
                                        || (desc.startsWith("world is") && desc.endsWith(" GB."))
                                        || (desc.endsWith("joined") && spaceSplit.length == 2)
                                        || (desc.endsWith("left") && spaceSplit.length == 2)
                                        || desc.startsWith("Permanently ignoring ")
                                        || desc.equals("2b2t is full")
                                        || desc.equals("Connecting to the server...")
                                        || desc.equals("You have lost connection to the server")
                                        || desc.startsWith("[ Ignored players")
                                        || desc.equals("No players ignored.")
                                        || desc.startsWith("Exception Connecting:")
                                        || desc.startsWith("An internal error occurred")
                                        || desc.startsWith("Bot kicked")
                                        || desc.startsWith("/")
                                        || desc.startsWith("test queue lengths")
                                        || desc.startsWith("main queue lengths")
                                        || desc.startsWith("Connected to main server")
                                        || desc.startsWith("You are already queued to server")
                                        || desc.equals("Connected to the server.")
                                        || desc.equals("Reconnecting to server main.")
                                        || desc.equals("You are already on server main.")
                                        || desc.equals("You need to be in queue for at least one server.")
                                        || desc.equals("Queueing for offline server main, you will remain in queue until the server is online.")
                                        || desc.startsWith("Server test requires")
                                        || desc.equals("Queued for server main.")
                                        || desc.startsWith("2B2BOT is about to update")
                                ) {
                                    continue;
                                }

                                final Optional<DeathMessageParseResult> parse = deathMessagesParser.parse(desc);
                                if (parse.isPresent()) {
                                    final String victim = parse.get().getVictim();
                                    final UUID victimUuid = getPlayerUuid(tTime, parse.get().getVictim(), outContext);
                                    final String killerName = (parse.get().getKiller().isPresent() && parse.get().getKiller().get().getType() == KillerType.PLAYER) ? parse.get().getKiller().get().getName() : null;
                                    final UUID killerUuid = (parse.get().getKiller().isPresent() && parse.get().getKiller().get().getType() == KillerType.PLAYER) ? getPlayerUuid(tTime, parse.get().getKiller().get().getName(), outContext) : null;
                                    final String weapon = parse.get().getWeapon().orElse(null);
                                    final String mob = (parse.get().getKiller().isPresent() && parse.get().getKiller().get().getType() == KillerType.MOB) ? parse.get().getKiller().get().getName() : null;
                                    final Deaths d = new Deaths(tTime.atOffset(ZoneOffset.UTC), desc,
                                            victim, victimUuid,
                                            killerName, killerUuid, weapon, mob);
                                    buffer.add(d);
                                    if (buffer.size() > maxBuffer) {
                                        writeBuffer(buffer, outContext);
                                        buffer.clear();
                                    }
                                } else {
                                    DATABASE_LOG.error("Unable to parse: {}", desc);
                                }
                            }
                        }
                    }
                    writeBuffer(buffer, outContext);
                    buffer.clear();
                }
            }

        } catch (final Exception e) {
            DATABASE_LOG.error("", e);
        }
    }

    //    @Test
    public void reparseProdDeaths() {
        final List<Query> buffer = new ArrayList<>();
        final int maxBuffer = 100;
        final DeathMessagesParser deathMessagesParser = new DeathMessagesParser();
        final ConnectionPool outConnectionPool = new ConnectionPool("jdbc:postgresql://localhost:5432/postgres", "postgres", "a");
        try (final Connection outConnection = outConnectionPool.getWriteConnection()) {
            final DSLContext outContext = DSL.using(outConnection, SQLDialect.POSTGRES);
            com.zenith.database.dto.tables.Deaths d = com.zenith.database.dto.tables.Deaths.DEATHS;
            final Result<DeathsRecord> existingDeaths = outContext.selectFrom(d)
                    .fetch(); // this is only fine because this table is pretty small currently. Otherwise would need to partition and batch this
            for (DeathsRecord dr : existingDeaths) {
                final Optional<DeathMessageParseResult> deathMessageParseResult = deathMessagesParser.parse(dr.get(d.DEATH_MESSAGE));
                if (!deathMessageParseResult.isPresent()) {
                    DATABASE_LOG.error("Unable to parse: {}", dr.getDeathMessage());
                    continue;
                }
                final DeathMessageParseResult pr = deathMessageParseResult.get();
                final String killerName = (pr.getKiller().isPresent() && pr.getKiller().get().getType() == KillerType.PLAYER) ? pr.getKiller().get().getName() : null;
                final String weapon = pr.getWeapon().orElse(null);
                final String mob = (pr.getKiller().isPresent() && pr.getKiller().get().getType() == KillerType.MOB) ? pr.getKiller().get().getName() : null;
                final UpdateConditionStep<DeathsRecord> query = outContext.update(d)
                        .set(d.KILLER_PLAYER_NAME, killerName)
                        .set(d.WEAPON_NAME, weapon)
                        .set(d.KILLER_MOB, mob) // this is a bit slow, could add an index on time and victim name to speed this up a bit
                        .where(d.TIME.eq(dr.get(d.TIME)).and(d.VICTIM_PLAYER_NAME.eq(dr.get(d.VICTIM_PLAYER_NAME))));
                buffer.add(query);
                if (buffer.size() >= maxBuffer) {
                    writeBufferQ(buffer, outContext);
                    buffer.clear();
                }
            }
            writeBufferQ(buffer, outContext);
        } catch (final Exception e) {
            DATABASE_LOG.error("", e);
        }
    }

    private void writeBufferQ(List<Query> buffer, DSLContext outContext) {
        DATABASE_LOG.info("Writing batch {}", batchCount++);
        outContext.batch(buffer).execute();
    }

    private UUID getPlayerUuid(final Instant time, String username, final DSLContext inContext) {
        try {
            final Connections c = Connections.CONNECTIONS;
            Result<ConnectionsRecord> connectionsRecords = inContext.selectFrom(c)
                    .where(c.PLAYER_NAME.eq(username).and(c.TIME.lt(time.atOffset(ZoneOffset.UTC))))
                    .orderBy(c.TIME.desc())
                    .limit(1)
                    .fetch();
            if (connectionsRecords.isEmpty()) {
                // try looking a bit ahead if we missed a join for example
                connectionsRecords = inContext.selectFrom(c)
                        .where(c.PLAYER_NAME.eq(username).and(c.TIME.lt(time.plus(Duration.ofDays(14)).atOffset(ZoneOffset.UTC))))
                        .orderBy(c.TIME.asc())
                        .limit(1)
                        .fetch();
                if (connectionsRecords.isEmpty()) {
                    final Names n = Names.NAMES;
                    Result<NamesRecord> namesRecords = inContext.selectFrom(n)
                            .where(n.NAME.eq(username)
                                    .and(n.CHANGEDTOAT.isNull().or(n.CHANGEDTOAT.lt(time.atOffset(ZoneOffset.UTC)))))
                            .orderBy(n.CHANGEDTOAT.desc())
                            .limit(1)
                            .fetch();
                    if (namesRecords.isEmpty()) {
                        return null;
                    }
                    NamesRecord namesRecord = namesRecords.get(0);
                    UUID uuid = namesRecord.getUuid();
                    return uuid;
                }
            }
            final ConnectionsRecord record = connectionsRecords.get(0);
            UUID uuid = record.getPlayerUuid();
            return uuid;
        } catch (final Exception e) {
            return null;
        }

    }

    private void writeBuffer(List<Deaths> buffer, DSLContext outContext) {
        DATABASE_LOG.info("Writing batch {}", batchCount++);
        com.zenith.database.dto.tables.Deaths d = com.zenith.database.dto.tables.Deaths.DEATHS;
        final List<InsertSetMoreStep<DeathsRecord>> queries = buffer.stream().map(death -> outContext.insertInto(d)
                        .set(d.TIME, death.getTime())
                        .set(d.DEATH_MESSAGE, death.getDeathMessage())
                        .set(d.VICTIM_PLAYER_NAME, death.getVictimPlayerName())
                        .set(d.VICTIM_PLAYER_UUID, death.getVictimPlayerUuid())
                        .set(d.KILLER_PLAYER_NAME, death.getKillerPlayerName())
                        .set(d.KILLER_PLAYER_UUID, death.getKillerPlayerUuid())
                        .set(d.WEAPON_NAME, death.getWeaponName())
                        .set(d.KILLER_MOB, death.getKillerMob()))
                .collect(Collectors.toList());
        outContext.batch(queries).execute();
    }

    private static class DiscordMessage {
        @JsonProperty("timestamp")
        public String timestamp;
        @JsonProperty("author")
        public DiscordMessageAuthor author;
        @JsonProperty("embeds")
        public List<DiscordMessageEmbed> embeds;
    }

    private static class DiscordMessageAuthor {
        @JsonProperty("id")
        public String id;
    }

    @ToString
    private static class DiscordMessageEmbed {
        @JsonProperty("description")
        public String description;
        @JsonProperty("timestamp")
        public String timestamp;
        @JsonProperty("color")
        public String color;
    }

}
