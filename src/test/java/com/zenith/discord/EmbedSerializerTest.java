package com.zenith.discord;

import com.zenith.util.ComponentSerializer;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class EmbedSerializerTest {
    @Test
    public void testBoldReplaceSimple() {
        var str = "**Test**";

        var c = EmbedSerializer.serializeText(str);

        var child = c.children().get(0);
        assertSame(TextDecoration.State.TRUE, child.style().decorations().get(TextDecoration.BOLD));
        assertEquals("Test", ((TextComponent) child).content());
    }

    @Test
    public void testBoldReplaceMedium() {
        var str = "**Test two words** and another **bold** word";

        var c = EmbedSerializer.serializeText(str);

        var child1 = c.children().get(0);
        assertSame(TextDecoration.State.TRUE, child1.style().decorations().get(TextDecoration.BOLD));
        assertEquals("Test two words", ((TextComponent) child1).content());

        var child2 = c.children().get(1);
        assertSame(TextDecoration.State.NOT_SET, child2.style().decorations().get(TextDecoration.BOLD));
        assertEquals(" and another ", ((TextComponent) child2).content());

        var child3 = c.children().get(2);
        assertSame(TextDecoration.State.TRUE, child3.style().decorations().get(TextDecoration.BOLD));
        assertEquals("bold", ((TextComponent) child3).content());
    }

    @Test
    public void testItalicReplaceSimple() {
        var str = "*Test*";
        var c = EmbedSerializer.serializeText(str);
        var child = c.children().get(0);
        assertSame(TextDecoration.State.TRUE, child.style().decorations().get(TextDecoration.ITALIC));
        assertEquals("Test", ((TextComponent) child).content());
    }

    @Test
    public void testUnderlineItalicReplaceSimple() {
        var str = "_Test_";
        var c = EmbedSerializer.serializeText(str);
        var child = c.children().get(0);
        assertSame(TextDecoration.State.TRUE, child.style().decorations().get(TextDecoration.ITALIC));
        assertEquals("Test", ((TextComponent) child).content());
    }

    @Test
    public void testUnderlineReplaceSimple() {
        var str = "__Test__";
        var c = EmbedSerializer.serializeText(str);
        var child = c.children().get(0);
        assertSame(TextDecoration.State.TRUE, child.style().decorations().get(TextDecoration.UNDERLINED));
        assertEquals("Test", ((TextComponent) child).content());
    }

    @Test
    public void codeStrTestSimple() {
        var str = "`Test`";

        var c = EmbedSerializer.serializeText(str);

        var child = c.children().get(0);
        assertSame(NamedTextColor.GRAY, child.style().color());
        assertEquals("Test", ((TextComponent) child).content());
    }

    @Test
    public void codeBlockTestSimple() {
        var str = "```Test```";

        var c = EmbedSerializer.serializeText(str);

        var child = c.children().get(0);
        assertSame(NamedTextColor.GRAY, child.style().color());
        assertEquals("Test", ((TextComponent) child).content());
    }

    @Test
    public void complexTest() {
        var str = "**Test** `code` ```code block```";

        var c = EmbedSerializer.serializeText(str);

        var child1 = c.children().get(0);
        assertSame(TextDecoration.State.TRUE, child1.style().decorations().get(TextDecoration.BOLD));
        assertEquals("Test", ((TextComponent) child1).content());

        var child2 = c.children().get(2);
        assertSame(NamedTextColor.GRAY, child2.style().color());
        assertEquals("code", ((TextComponent) child2).content());

        var child3 = c.children().get(4);
        assertSame(NamedTextColor.GRAY, child3.style().color());
        assertEquals("code block", ((TextComponent) child3).content());
    }

    @Test
    public void linksTest() {
        var str = "[Test](https://example.com)";

        var c = EmbedSerializer.serializeText(str);

        var child = c.children().get(0);
        assertSame(NamedTextColor.BLUE, child.style().color());
        assertEquals("Test", ((TextComponent) child).content());
        assertEquals("https://example.com", ((ClickEvent.Payload.Text) child.clickEvent().payload()).value());
    }

    @Test
    public void unescapeTest() {
        var in = "**bold multi word** *italic* _underline_ `code`";
        var str = DiscordBot.escape(in);
        var c = EmbedSerializer.serializeText(str);
        var text = ComponentSerializer.serializePlain(c);
        assertEquals(in, text);
    }

    @Test
    public void spoilerTest() {
        var str = "||spoiler||";
        var c = EmbedSerializer.serializeText(str);
        var text = ComponentSerializer.serializePlain(c);
        assertEquals("spoiler", text);
        assertTrue(c.style().hasDecoration(TextDecoration.ITALIC));
        assertEquals("spoiler", ((TextComponent) c.children().getFirst()).content());
    }

    @Test
    public void chatSchemaMultiCodeStrTest() {
        var str = """
                  **Default Schema**
                  * Public Chat: `<$s> $m`
                  * Whisper Inbound: `$s whispers: $m`
                  * Whisper Outbound: `to $r: $m`
                  """;
        var c = EmbedSerializer.serializeText(str);
        var actual = ComponentSerializer.serializePlain(c);
        var withoutDiscordFormatting = """
                  Default Schema
                  * Public Chat: <$s> $m
                  * Whisper Inbound: $s whispers: $m
                  * Whisper Outbound: to $r: $m
                  """;
        assertEquals(withoutDiscordFormatting, actual);
        assertTrue(c.children().getFirst() instanceof TextComponent t && t.content().equals("Default Schema") && t.hasDecoration(TextDecoration.BOLD));
        var codeCount = c.children().stream()
            .filter(child -> child.style().color() == NamedTextColor.GRAY)
            .count();
        assertEquals(3, codeCount);
    }
}
