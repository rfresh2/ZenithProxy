import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zenith.database.ConnectionPool;
import com.zenith.database.dto.enums.Connectiontype;
import com.zenith.database.dto.tables.Connections;
import com.zenith.database.dto.tables.records.ConnectionsRecord;
import lombok.Builder;
import lombok.Data;
import org.jooq.DSLContext;
import org.jooq.InsertSetMoreStep;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.zenith.util.Constants.DATABASE_LOG;

public class ConnectionsImport {
    final Pattern usernamePattern = Pattern.compile("`\\w+`");
    final Pattern uuidPattern = Pattern.compile("[a-f0-9]{8}(?:-[a-f0-9]{4}){4}[a-f0-9]{8}");
    final Pattern userNameValidPattern = Pattern.compile("[A-Za-z0-9_]{1,16}$");

    //    @Test
    public void connectionsImport() {
        List<ParsedConnection> connections = getParsedConnections();
        final ConnectionPool connectionPool = new ConnectionPool();

        try (final Connection connection = connectionPool.getWriteConnection()) {
            final DSLContext context = DSL.using(connection, SQLDialect.POSTGRES);
            final Connections c = Connections.CONNECTIONS;
            final int partitionSize = 100;
            Collection<List<ParsedConnection>> connectionsBatched = IntStream.range(0, connections.size())
                    .boxed()
                    .collect(Collectors.groupingBy(partition -> (partition / partitionSize), Collectors.mapping(connections::get, Collectors.toList())))
                    .values();
            int count = 0;
            for (List<ParsedConnection> sublist : connectionsBatched) {
                DATABASE_LOG.info("Inserting batch {}", count++);
                List<InsertSetMoreStep<ConnectionsRecord>> batch = sublist.stream()
                        .map(i -> context.insertInto(c)
                                .set(c.TIME, i.time.atOffset(ZoneOffset.UTC))
                                .set(c.PLAYER_UUID, i.uuid)
                                .set(c.PLAYER_NAME, i.username)
                                .set(c.CONNECTION, i.connectiontype))
                        .collect(Collectors.toList());
                context.batch(batch).execute();
            }
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }

    }

    private List<ParsedConnection> getParsedConnections() {
        final ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        final Instant cutoff = Instant.ofEpochSecond(1672343220L);
        try {
            final List<ParsedConnection> deserialized = objectMapper.readValue(new File("data/2b2tbotjoins.json"), DiscordMain.class)
                    .messages.stream()
                    .flatMap(discordMessage -> discordMessage.embeds.stream())
                    .map(embed -> ParsedConnection.builder()
                            .time(OffsetDateTime.parse(embed.timestamp).toInstant())
                            .username(extractUsername(embed.description))
                            .uuid(extractUuid(embed.thumbnail.url))
                            .connectiontype(extractConnectionType(embed.color))
                            .build())
                    .filter(c -> c.time.isBefore(cutoff))
                    .collect(Collectors.toList());
            return deserialized;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Connectiontype extractConnectionType(String color) {
        if (color.equals("#FF0000")) {
            return Connectiontype.LEAVE;
        } else if (color.equals("#00FF00")) {
            return Connectiontype.JOIN;
        } else {
            throw new RuntimeException("no color found");
        }
    }

    private UUID extractUuid(String url) {
        final Matcher m = uuidPattern.matcher(url);
        try {
            m.find();
            return UUID.fromString(m.group());
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String extractUsername(String description) {
        final Matcher m = usernamePattern.matcher(description);
        m.find();
        String uname = m.group().replace("`", "");
        final Matcher mv = userNameValidPattern.matcher(uname);
        if (!mv.matches()) {
            throw new RuntimeException("invalid username");
        }
        return uname;
    }


    static class DiscordMain {
        @JsonProperty("messages")
        public List<DiscordMessage> messages;

    }

    @Data
    @Builder
    static class ParsedConnection {
        private final String username;
        private final UUID uuid;
        private final Instant time;
        private final Connectiontype connectiontype;
    }

    static class DiscordMessage {
        @JsonProperty("timestamp")
        public String timestamp;
        @JsonProperty("content")
        public String content;
        @JsonProperty("author")
        public DiscordMessageAuthor author;
        @JsonProperty("embeds")
        public List<DiscordMessageEmbed> embeds;
    }

    static class DiscordMessageEmbed {
        @JsonProperty("description")
        public String description;
        @JsonProperty("color")
        public String color;
        @JsonProperty("timestamp")
        public String timestamp;
        @JsonProperty("thumbnail")
        public DiscordEmbedThumbnail thumbnail;
    }

    static class DiscordMessageAuthor {
        @JsonProperty("id")
        public String id;

    }

    static class DiscordEmbedThumbnail {
        @JsonProperty("url")
        public String url;
    }
}
