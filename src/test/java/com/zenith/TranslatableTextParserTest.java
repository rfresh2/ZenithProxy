package com.zenith;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TranslatableTextParserTest {

    @Test
    public void translatableTextComponentParseTest() {
        final String chatText = "{\"translate\":\"chat.type.text\",\"with\":[{\"text\":\"bonk2b2t\"},{\"text\":\"you should never talk about that with them\"}]}";
        final String rawString = Shared.FORMAT_PARSER.parse(chatText).toRawString();
        assertEquals(rawString, "<bonk2b2t> you should never talk about that with them");
    }
}
