package net.daporkchop.toobeetooteebot.util;

import com.google.gson.JsonParser;
import net.daporkchop.toobeetooteebot.text.ITextComponent;

public class ChatUtils {
    private static JsonParser parser = new JsonParser();

    public static String getOldText(String json) {
        ITextComponent component = ITextComponent.Serializer.jsonToComponent(json.trim());
        String text = component.getFormattedText();
        if (TextFormat.clean(text).startsWith("{")) {
            text = ITextComponent.Serializer.jsonToComponent(TextFormat.clean(text)).getFormattedText();
        }
        return text;
    }
}
