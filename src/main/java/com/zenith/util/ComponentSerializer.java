package com.zenith.util;

import lombok.experimental.UtilityClass;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.flattener.ComponentFlattener;
import net.kyori.adventure.translation.GlobalTranslator;

import java.util.Locale;

import static net.kyori.adventure.text.serializer.gson.GsonComponentSerializer.gson;
import static net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacyAmpersand;

@UtilityClass
public final class ComponentSerializer {

    public static String serialize(Component component) {
        return gson().serialize(component);
    }

    public static Component deserialize(String string) {
        // todo: apply translation here?
        if (string.contains("§")) return legacyAmpersand().deserialize(string);
        return gson().deserialize(string);
    }

    public static String toRawString(Component component) {
        final StringBuilder builder = new StringBuilder();
        ComponentFlattener.basic().flatten(translate(component), builder::append);
        return builder.toString();
    }

    public static Component translate(Component component) {
        return GlobalTranslator.render(component, Locale.ENGLISH);
    }
}
