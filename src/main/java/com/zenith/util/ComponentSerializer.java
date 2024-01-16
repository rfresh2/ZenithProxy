package com.zenith.util;

import de.themoep.minedown.adventure.MineDown;
import lombok.experimental.UtilityClass;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.flattener.ComponentFlattener;
import net.kyori.adventure.text.serializer.ansi.ANSIComponentSerializer;
import net.kyori.adventure.translation.GlobalTranslator;
import net.kyori.adventure.translation.TranslationRegistry;
import net.kyori.ansi.ColorLevel;

import java.util.Locale;
import java.util.function.Consumer;

import static net.kyori.adventure.text.serializer.gson.GsonComponentSerializer.gson;
import static net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacyAmpersand;

@UtilityClass
public final class ComponentSerializer {
    private static final ComponentFlattener componentFlattener = ComponentFlattener.basic().toBuilder()
        .complexMapper(TranslatableComponent.class, ComponentSerializer::translatableMapper)
        .build();
    private static final ANSIComponentSerializer ansiComponentSerializer;

    static { // fixes no ansi colors being serialized on dumb terminals
        final String TERM = System.getenv("TERM"); // this should be set on unix systems
        // must be set before creating ansi serializer
        if (TERM == null) {
            String colorLevel = "indexed16";
            final String intellij = System.getenv("IDEA_INITIAL_DIRECTORY");
            final String windowsTerminal = System.getenv("WT_SESSION");
            final String cmd = System.getenv("PROMPT");
            if (intellij != null || windowsTerminal != null || cmd != null)
                colorLevel = "truecolor";
            System.setProperty(ColorLevel.COLOR_LEVEL_PROPERTY, colorLevel);
        }
        ansiComponentSerializer = ANSIComponentSerializer.builder()
            .flattener(ComponentFlattener.basic()
                           .toBuilder()
                           .complexMapper(TranslatableComponent.class, ComponentSerializer::translatableMapper)
                           .build())
            .build();
    }

    public static String serializeJson(Component component) {
        return gson().serialize(component);
    }

    public static Component deserialize(String string) {
        if (string.contains("§")) return legacyAmpersand().deserialize(string);
        return gson().deserialize(string);
    }

    public static String serializeAnsi(Component component) {
        return ansiComponentSerializer.serialize(component);
    }

    public static String serializePlain(Component component) {
        var builder = new StringBuilder();
        componentFlattener.flatten(component, builder::append);
        return builder.toString();
    }

    public static Component minedown(String message, String... replacements) {
        return new MineDown(message)
            .urlDetection(false) // this uses a url matching regex by default that adds mem usage and isn't needed
            .replace(replacements)
            .toComponent();
    }

    private static void translatableMapper(TranslatableComponent translatableComponent, Consumer<Component> componentConsumer) {
        for (var source : GlobalTranslator.translator().sources()) {
            if (source instanceof TranslationRegistry registry && registry.contains(translatableComponent.key())) {
                componentConsumer.accept(GlobalTranslator.render(translatableComponent, Locale.ENGLISH));
                return;
            }
        }
        var fallback = translatableComponent.fallback();
        if (fallback == null) return;
        for (var source : GlobalTranslator.translator().sources()) {
            if (source instanceof TranslationRegistry registry && registry.contains(fallback)) {
                componentConsumer.accept(GlobalTranslator.render(Component.translatable(fallback), Locale.ENGLISH));
                return;
            }
        }
    }

}
