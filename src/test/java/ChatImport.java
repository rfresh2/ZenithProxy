import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zenith.database.ConnectionPool;
import com.zenith.database.dto.tables.Chats;
import com.zenith.database.dto.tables.Connections;
import com.zenith.database.dto.tables.records.ChatsRecord;
import com.zenith.database.dto.tables.records.ConnectionsRecord;
import lombok.Builder;
import lombok.Data;
import lombok.ToString;
import org.jooq.DSLContext;
import org.jooq.InsertSetMoreStep;
import org.jooq.Result;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.zenith.util.Constants.DATABASE_LOG;
import static java.util.Arrays.asList;

public class ChatImport {

    final Pattern userNameValidPattern = Pattern.compile("[A-Za-z0-9_]{1,16}");
    final List<String> discordEscapeChars = asList("*", "_", "~");
    final Instant cutoffTimeBefore = Instant.ofEpochSecond(1646856000L);
    final Instant cutoffTimeAfter = Instant.ofEpochSecond(1654282800L);
    int batchCount = 0;

    //    @Test
    public void chatImport() {
        final ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        final List<ParsedChat> buffer = new ArrayList<>();
        final int maxBuffer = 100;
        final ConnectionPool inConnectionPool = new ConnectionPool("jdbc:postgresql://localhost:5432/postgres", "postgres", "a");
        final ConnectionPool outConnectionPool = new ConnectionPool();
        try (final Connection outConnection = outConnectionPool.getWriteConnection();
             final Connection inConnection = inConnectionPool.getWriteConnection()) {
            final DSLContext outContext = DSL.using(outConnection, SQLDialect.POSTGRES);
            final DSLContext inContext = DSL.using(inConnection, SQLDialect.POSTGRES);
            try (final InputStream is = new FileInputStream(new File("data/2b2tbotchatmayjun.json"))) {
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
                            if (!discordMessage.embeds.get(0).description.startsWith("<")) {
                                continue;
                            }
                            try {
                                Instant time = OffsetDateTime.parse(discordMessage.timestamp).toInstant();

                                if (time.isBefore(cutoffTimeBefore) || time.isAfter(cutoffTimeAfter)) {
                                    continue;
                                }
                                final String username = extractUsername(discordMessage.embeds.get(0).description);
                                final ParsedChat parsedChat = new ParsedChat.ParsedChatBuilder()
                                        .time(time)
                                        .username(username)
                                        .chat(extractChat(discordMessage.embeds.get(0).description))
                                        .uuid(extractUuid(username, inContext, time))
                                        .build();
                                buffer.add(parsedChat);
                            } catch (final RuntimeException e) {
                                DATABASE_LOG.error("Error parsing: {}" + discordMessage.embeds.get(0).toString());
                            }
                            if (buffer.size() > maxBuffer) {
                                writeChats(outContext, buffer);
                                buffer.clear();
                            }
                        }
                    }

                }
                writeChats(outContext, buffer);
            } catch (final Exception e) {
                throw new RuntimeException(e);
            }
        } catch (final Exception e) {

        }

    }

    private UUID extractUuid(String username, final DSLContext inContext, final Instant time) {
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
                    return null;
                }
            }
            final ConnectionsRecord record = connectionsRecords.get(0);
            UUID uuid = record.getPlayerUuid();
            return uuid;
        } catch (final Exception e) {
            return null;
        }

    }

    private void writeChats(DSLContext context, List<ParsedChat> buffer) {
        final Chats c = Chats.CHATS;
        List<InsertSetMoreStep<ChatsRecord>> queries = buffer.stream().map(chat -> context.insertInto(c)
                        .set(c.TIME, chat.time.atOffset(ZoneOffset.UTC))
                        .set(c.PLAYER_NAME, chat.username)
                        .set(c.CHAT, chat.chat)
                        .set(c.PLAYER_UUID, chat.uuid))
                .collect(Collectors.toList());
        DATABASE_LOG.info("Writing batch {} ", batchCount++);
        context.batch(queries).execute();
    }

    private String extractChat(String description) {
        try {
            return trimDiscordEscape(description.substring(description.indexOf(" ")).trim());
        } catch (final Exception e) {
            // can throw when chat is blank, probably from some whitespace char message that was sent
            throw new RuntimeException(e);
        }

    }

    private String trimDiscordEscape(String chat) {
        final int backslashIndex = chat.indexOf("\\");
        if (backslashIndex != -1) {
            String out = chat;
            for (String c : discordEscapeChars) {
                out = out.replace("\\" + c, c);
            }
            return out;
        }
        return chat;
    }

    private String extractUsername(String description) {
        final Matcher m = userNameValidPattern.matcher(description);
        if (m.find()) {
            return m.group();
        }
        throw new RuntimeException("no username found");
    }

    static class DiscordMain {
        @JsonProperty("messages")
        public List<DiscordMessage> messages;

    }

    @Data
    @Builder
    private static class ParsedChat {
        private final String username;
        private final UUID uuid;
        private final Instant time;
        private final String chat;
    }

    private static class DiscordMessage {
        @JsonProperty("timestamp")
        public String timestamp;
        @JsonProperty("content")
        public String content;
        @JsonProperty("author")
        public DiscordMessageAuthor author;
        @JsonProperty("embeds")
        public List<DiscordMessageEmbed> embeds;
    }

    @ToString
    private static class DiscordMessageEmbed {
        @JsonProperty("description")
        public String description;
        @JsonProperty("color")
        public String color;
        @JsonProperty("timestamp")
        public String timestamp;
        @JsonProperty("thumbnail")
        public DiscordEmbedThumbnail thumbnail;
    }

    private static class DiscordMessageAuthor {
        @JsonProperty("id")
        public String id;

    }

    private static class DiscordEmbedThumbnail {
        @JsonProperty("url")
        public String url;
    }
}
