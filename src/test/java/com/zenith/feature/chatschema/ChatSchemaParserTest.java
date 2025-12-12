package com.zenith.feature.chatschema;

import org.geysermc.mcprotocollib.auth.GameProfile;
import org.geysermc.mcprotocollib.protocol.data.game.PlayerListEntry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static com.zenith.Globals.CACHE;
import static org.junit.jupiter.api.Assertions.*;

public class ChatSchemaParserTest {
    PlayerListEntry rfresh2Entry = new PlayerListEntry(
        "rfresh2", UUID.fromString("572e683c-888a-4a0d-bc10-5d9cfa76d892")
    );

    PlayerListEntry rfresh2BedrockEntry = new PlayerListEntry(
        ".rfresh2", UUID.fromString("00000000-0000-0000-0009-01f6c03c69e5")
    );

    PlayerListEntry arrfreshEntry = new PlayerListEntry(
        "aRRfresh", UUID.fromString("7cb31e32-65dc-494e-b812-3391670374c3")
    );

    ChatSchema schema2b2t = new ChatSchema(
        "<$s> $m",
        "$s whispers: $m",
        "to $r: $m"
    );
    ChatSchema schemaEssentials = new ChatSchema(
        "<$s> $m",
        "[$s -> me] $m",
        "[me -> $r] $m"
    );

    @BeforeEach
    public void setUp() {
        CACHE.getTabListCache().add(rfresh2Entry);
        CACHE.getTabListCache().add(arrfreshEntry);
        CACHE.getTabListCache().add(rfresh2BedrockEntry);
        CACHE.getProfileCache().setProfile(new GameProfile(rfresh2Entry.getProfileId(), rfresh2Entry.getName()));
    }

    @AfterEach
    public void tearDown() {
        CACHE.getTabListCache().remove(rfresh2Entry);
        CACHE.getTabListCache().remove(arrfreshEntry);
        CACHE.getProfileCache().setProfile(null);
    }

    @Test
    public void test2b2tPublicChat() {
        String input = "<rfresh2> what's up?";
        var result = ChatSchemaParser.parse(input, schema2b2t);
        assertNotNull(result);
        assertEquals(ChatType.PUBLIC_CHAT, result.type());
        assertEquals(rfresh2Entry, result.sender());
        assertNull(result.receiver());
        assertEquals("what's up?", result.messageContent());
    }

    @Test
    public void test2b2tBedrockPublicChat() {
        String input = "<.rfresh2> what's up?";
        var result = ChatSchemaParser.parse(input, schema2b2t);
        assertNotNull(result);
        assertEquals(ChatType.PUBLIC_CHAT, result.type());
        assertEquals(rfresh2BedrockEntry, result.sender());
        assertNull(result.receiver());
        assertEquals("what's up?", result.messageContent());
    }


    @Test
    public void test2b2tWhisperOutgoing() {
        String input = "to aRRfresh: what's up?";
        var result = ChatSchemaParser.parse(input, schema2b2t);
        assertNotNull(result);
        assertEquals(ChatType.WHISPER_OUTBOUND, result.type());
        assertEquals(rfresh2Entry, result.sender());
        assertEquals(arrfreshEntry, result.receiver());
        assertEquals("what's up?", result.messageContent());
    }

    @Test
    public void test2b2tWhisperInbound() {
        String input = "aRRfresh whispers: what's up?";
        var result = ChatSchemaParser.parse(input, schema2b2t);
        assertNotNull(result);
        assertEquals(ChatType.WHISPER_INBOUND, result.type());
        assertEquals(arrfreshEntry, result.sender());
        assertEquals(rfresh2Entry, result.receiver());
        assertEquals("what's up?", result.messageContent());
    }

    @Test
    public void testEssentialsWhisperOutgoing() {
        String input = "[me -> aRRfresh] what's up?";
        var result = ChatSchemaParser.parse(input, schemaEssentials);
        assertNotNull(result);
        assertEquals(ChatType.WHISPER_OUTBOUND, result.type());
        assertEquals(rfresh2Entry, result.sender());
        assertEquals(arrfreshEntry, result.receiver());
        assertEquals("what's up?", result.messageContent());
    }

    @Test
    public void testEssentialsWhisperInbound() {
        String input = "[aRRfresh -> me] what's up?";
        var result = ChatSchemaParser.parse(input, schemaEssentials);
        assertNotNull(result);
        assertEquals(ChatType.WHISPER_INBOUND, result.type());
        assertEquals(arrfreshEntry, result.sender());
        assertEquals(rfresh2Entry, result.receiver());
        assertEquals("what's up?", result.messageContent());
    }

    @Test
    public void testEssentialsWithGroupPublicChat() {
        String input = "[default] rfresh2: what's up?";
        var schema = new ChatSchema(
            "[$w] $s: $m",
            schemaEssentials.whisperInbound(),
            schemaEssentials.whisperOutbound()
        );
        var result = ChatSchemaParser.parse(input, schema);
        assertNotNull(result);
        assertEquals(ChatType.PUBLIC_CHAT, result.type());
        assertEquals(rfresh2Entry, result.sender());
        assertNull(result.receiver());
        assertEquals("what's up?", result.messageContent());
    }

    @Test
    public void testDoubleWordWildcardMatch() {
        String input = ">> [BOSS] [ADMIN] rfresh2 : what's up?";
        var schema = new ChatSchema(
            ">> [$w] $s : $m",
            schemaEssentials.whisperInbound(),
            schemaEssentials.whisperOutbound()
        );
        var result = ChatSchemaParser.parse(input, schema);
        assertNotNull(result);
        assertEquals(ChatType.PUBLIC_CHAT, result.type());
        assertEquals(rfresh2Entry, result.sender());
        assertNull(result.receiver());
        assertEquals("what's up?", result.messageContent());
    }
}
