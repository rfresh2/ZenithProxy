package com.zenith.discord;

import net.kyori.adventure.text.TextComponent;
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
        assertTrue(child.style().decorations().get(TextDecoration.BOLD) == TextDecoration.State.TRUE);
        assertTrue(((TextComponent) child).content().equals("Test"));
    }

    @Test
    public void testBoldReplaceMedium() {
        var str = "**Test two words** and another **bold** word";

        var c = EmbedSerializer.serializeText(str);

        var child1 = c.children().get(0);
        assertSame(child1.style().decorations().get(TextDecoration.BOLD), TextDecoration.State.TRUE);
        assertEquals("Test two words", ((TextComponent) child1).content());

        var child2 = c.children().get(1);
        assertSame(child2.style().decorations().get(TextDecoration.BOLD), TextDecoration.State.NOT_SET);
        assertEquals(" and another ", ((TextComponent) child2).content());

        var child3 = c.children().get(2);
        assertSame(child3.style().decorations().get(TextDecoration.BOLD), TextDecoration.State.TRUE);
        assertEquals("bold", ((TextComponent) child3).content());
    }

    @Test
    public void codeStrTestSimple() {
        var str = "`Test`";

        var c = EmbedSerializer.serializeText(str);

        var child = c.children().get(0);
        assertSame(child.style().color(), NamedTextColor.GRAY);
        assertEquals("Test", ((TextComponent) child).content());
    }

    @Test
    public void codeBlockTestSimple() {
        var str = "```Test```";

        var c = EmbedSerializer.serializeText(str);

        var child = c.children().get(0);
        assertSame(child.style().color(), NamedTextColor.GRAY);
        assertEquals("Test", ((TextComponent) child).content());
    }

    @Test
    public void complexTest() {
        var str = "**Test** `code` ```code block```";

        var c = EmbedSerializer.serializeText(str);

        var child1 = c.children().get(0);
        assertSame(child1.style().decorations().get(TextDecoration.BOLD), TextDecoration.State.TRUE);
        assertEquals("Test", ((TextComponent) child1).content());

        var child2 = c.children().get(2);
        assertSame(child2.style().color(), NamedTextColor.GRAY);
        assertEquals("code", ((TextComponent) child2).content());

        var child3 = c.children().get(4);
        assertSame(child3.style().color(), NamedTextColor.GRAY);
        assertEquals("code block", ((TextComponent) child3).content());
    }
}
