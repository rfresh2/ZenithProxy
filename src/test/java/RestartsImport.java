import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zenith.database.ConnectionPool;
import com.zenith.database.dto.tables.Restarts;
import com.zenith.database.dto.tables.records.RestartsRecord;
import lombok.Data;
import org.jooq.DSLContext;
import org.jooq.InsertSetMoreStep;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;

public class RestartsImport {

    //    @Test
    public void restartsImport() {
        final List<ParsedMessage> messages = getParsedMessages();
        final ConnectionPool connectionPool = new ConnectionPool();

        try (final Connection connection = connectionPool.getWriteConnection()) {
            final DSLContext context = DSL.using(connection, SQLDialect.POSTGRES);
            final Restarts r = Restarts.RESTARTS;
            List<InsertSetMoreStep<RestartsRecord>> queries = messages.stream().map(m -> context.insertInto(r).set(r.TIME, m.time.plus(Duration.ofMinutes(15)).atOffset(ZoneOffset.UTC))).collect(Collectors.toList());
            context.batch(queries).execute();
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    private List<ParsedMessage> getParsedMessages() {
        final ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        try {
            final List<DiscordMessage> deserialized = objectMapper.readValue(new File("data/lolritterrestarts.json"), DiscordMain.class).messages;
            List<ParsedMessage> parsedMessages = deserialized.stream().map(this::parseMessage).collect(Collectors.toList());
            // filter 15 mins warning messages only
            return parsedMessages.stream().filter(m -> m.content.contains("15 Minutes")).collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private ParsedMessage parseMessage(final DiscordMessage discordMessage) {
        return new ParsedMessage(OffsetDateTime.parse(discordMessage.timestamp).toInstant(), discordMessage.content, discordMessage.author.id);
    }

    static class DiscordMain {
        @JsonProperty("messages")
        public List<DiscordMessage> messages;

    }

    static class DiscordMessage {
        @JsonProperty("timestamp")
        public String timestamp;
        @JsonProperty("content")
        public String content;
        @JsonProperty("author")
        public DiscordMessageAuthor author;
    }

    static class DiscordMessageAuthor {
        @JsonProperty("id")
        public String id;
    }

    @Data
    static class ParsedMessage {
        private final Instant time;
        private final String content;
        private final String authorId;
    }
}
