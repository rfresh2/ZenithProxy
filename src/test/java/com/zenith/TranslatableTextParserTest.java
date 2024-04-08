package com.zenith;

import com.zenith.util.ComponentSerializer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TranslatableTextParserTest {

    @Test
    public void translatableTextComponentParseTest() {
        Shared.loadConfig();
        Shared.loadLaunchConfig();
        Logger blah = Shared.CLIENT_LOG; // init in shared static block
        final String chatText = "{\"translate\":\"chat.type.text\",\"with\":[{\"text\":\"bonk2b2t\"},{\"text\":\"you should never talk about that with them\"}]}";
        Component deserialize = ComponentSerializer.deserialize(chatText);
        assertTrue(deserialize instanceof TranslatableComponent);
        String serialize = ComponentSerializer.serializePlain(deserialize);
        assertEquals("<bonk2b2t> you should never talk about that with them", serialize);
    }

    @Test
    public void serializePlainWithLinkTest() {
        final String chatText = "hello go to https://google.com";
        Component deserialize = ComponentSerializer.deserialize(chatText);
    }
}
