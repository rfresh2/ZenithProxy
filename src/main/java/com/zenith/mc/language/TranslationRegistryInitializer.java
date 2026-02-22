package com.zenith.mc.language;

import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.translation.GlobalTranslator;
import net.kyori.adventure.translation.TranslationStore;
import tools.jackson.databind.node.ObjectNode;

import java.text.MessageFormat;
import java.util.Locale;

import static com.zenith.mc.MCGlobals.OBJECT_MAPPER;

@UtilityClass
public class TranslationRegistryInitializer {
    @SneakyThrows
    public static void registerAllTranslations() {
        var translationRegistry = TranslationStore.messageFormat(Key.key("minecraft"));
        var node = (ObjectNode) OBJECT_MAPPER.readTree(TranslationRegistryInitializer.class.getResourceAsStream("/mcdata/language.smile"));
        for (var key : node.propertyNames()) {
            String value = node.get(key).asString();
            translationRegistry.register(key, Locale.ENGLISH, new MessageFormat(value));
        }
        GlobalTranslator.translator().addSource(translationRegistry);
    }
}
