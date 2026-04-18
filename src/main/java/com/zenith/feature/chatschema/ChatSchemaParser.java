package com.zenith.feature.chatschema;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.geysermc.mcprotocollib.protocol.data.game.PlayerListEntry;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.zenith.Globals.*;

@NullMarked
public class ChatSchemaParser {

    private static final String senderToken = "$s";
    private static final String receiverToken = "$r";
    private static final String messageToken = "$m";
    private static final String wildcardStringToken = "$w";

    private static final LoadingCache<String, Pattern> compiledPatternsCache = CacheBuilder.newBuilder()
        .maximumSize(10)
        .expireAfterAccess(Duration.ofMinutes(10))
        .build(CacheLoader.from(ChatSchemaParser::compilePattern));

    public static @Nullable ChatParseResult parse(String input) {
        return parse(input, getSchema());
    }

    public static @Nullable ChatParseResult parse(String input, ChatSchema schema) {
        var outboundWhisperParse = tryParseOutboundWhisper(input, schema.whisperOutbound());
        if (outboundWhisperParse != null) return outboundWhisperParse;

        var inboundWhisperParse = tryParseInboundWhisper(input, schema.whisperInbound());
        if (inboundWhisperParse != null) return inboundWhisperParse;

        var publicChatParse = tryParsePublicChat(input, schema.publicChat());
        if (publicChatParse != null) return publicChatParse;

        return null;
    }

    public static ChatSchema getSchema() {
        return CONFIG.client.chatSchemas.serverSchemas.getOrDefault(
            getServerAddress(),
            ChatSchema.DEFAULT_SCHEMA
        );
    }

    public static boolean hasCustomSchema() {
        return CONFIG.client.chatSchemas.serverSchemas.containsKey(getServerAddress());
    }

    public static String getServerAddress() {
        return CONFIG.client.server.address.toLowerCase().trim();
    }

    public static @Nullable ChatParseResult tryParsePublicChat(String rawInput, String publicChatSchema) {
        return tryParseChat(ChatType.PUBLIC_CHAT, rawInput, publicChatSchema);
    }

    public static @Nullable ChatParseResult tryParseOutboundWhisper(String rawInput, String outboundWhisperSchema) {
        return tryParseChat(ChatType.WHISPER_OUTBOUND, rawInput, outboundWhisperSchema);
    }

    public static @Nullable ChatParseResult tryParseInboundWhisper(String rawInput, String inboundWhisperSchema) {
        return tryParseChat(ChatType.WHISPER_INBOUND, rawInput, inboundWhisperSchema);
    }

    public static @Nullable ChatParseResult tryParseChat(ChatType type, String rawInput, String inputSchema) {
        try {
            return tryParseChat0(type, rawInput, inputSchema);
        } catch (Exception e) {
            CLIENT_LOG.debug("Error parsing chat schema: {} with input: {}", inputSchema, rawInput, e);
            return null;
        }
    }

    private static Pattern compilePattern(String schema) {
        StringBuilder pattern = new StringBuilder();
        pattern.append("\\Q");
        for (int i = 0; i < schema.length(); i++) {
            char c = schema.charAt(i);
            if (c == '$' && (i + 1 < schema.length())) {
                var potentialToken = schema.substring(i, i + 2);
                String p = switch (potentialToken) {
                    case senderToken, receiverToken -> "([\\w\\d_.]+)";
                    case messageToken, wildcardStringToken -> "(.+)";
                    default -> null;
                };
                if (p != null) {
                    pattern.append("\\E");
                    pattern.append(p);
                    pattern.append("\\Q");
                    i++;
                    continue;
                }
            }
            pattern.append(c);
        }
        pattern.append("\\E");
        return Pattern.compile(pattern.toString());
    }

    private static @Nullable ChatParseResult tryParseChat0(ChatType type, String rawInput, String schema) {
        Pattern pattern = compiledPatternsCache.getUnchecked(schema);
        Matcher matcher = pattern.matcher(rawInput);

        if (!matcher.matches()) return null;

        int groupCount = matcher.groupCount();
        String[] groups = new String[groupCount];
        for (int i = 0; i < groupCount; i++) {
            groups[i] = matcher.group(i + 1); // Group indices start at 1
        }

        PlayerListEntry sender = null;
        PlayerListEntry receiver = null;
        String messageContent = null;

        int index = 0;
        for (int i = 0; i < schema.length(); i++) {
            char c = schema.charAt(i);
            if (c == '$' && (i + 1 < schema.length())) {
                var potentialToken = schema.substring(i, i + 2);
                switch (potentialToken) {
                    case senderToken -> {
                        if (index >= groupCount) {
                            return null;
                        }
                        String s = groups[index];
                        var profile = CACHE.getTabListCache().getFromName(s);
                        if (profile.isEmpty()) {
                            return null;
                        }
                        sender = profile.get();
                        index++;
                    }
                    case receiverToken -> {
                        if (index >= groupCount) {
                            return null;
                        }
                        String s = groups[index];
                        var profile = CACHE.getTabListCache().getFromName(s);
                        if (profile.isEmpty()) {
                            return null;
                        }
                        receiver = profile.get();
                        index++;
                    }
                    case messageToken -> {
                        if (index >= groupCount) {
                            return null;
                        }
                        messageContent = groups[index];
                        index++;
                    }
                    case wildcardStringToken -> {
                        index++;
                    }
                }
            }
        }
        if (index < groups.length) {
            // more groups than expected
            return null;
        }
        switch (type) {
            case PUBLIC_CHAT -> {
                if (sender == null) return null;
                if (receiver != null) return null;
                if (messageContent == null) return null;
            }
            case WHISPER_OUTBOUND -> {
                if (sender == null) sender = getSelfEntry();
                else if (sender != getSelfEntry()) return null;
                if (receiver == null) return null;
                if (messageContent == null) return null;
            }
            case WHISPER_INBOUND -> {
                if (sender == null) return null;
                if (receiver == null) receiver = getSelfEntry();
                else if (receiver != getSelfEntry()) return null;
                if (messageContent == null) return null;
            }
        }
        return new ChatParseResult(type, sender, receiver, messageContent);
    }

    private static @Nullable PlayerListEntry getSelfEntry() {
        var selfProfile = CACHE.getProfileCache().getProfile();
        if (selfProfile == null) return null;
        return CACHE.getTabListCache().get(selfProfile.getId()).orElse(null);
    }
}
