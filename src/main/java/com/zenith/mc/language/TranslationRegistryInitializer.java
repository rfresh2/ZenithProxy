package com.zenith.mc.language;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.translation.GlobalTranslator;
import net.kyori.adventure.translation.TranslationRegistry;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.Map;

import static com.zenith.Shared.OBJECT_MAPPER;

@UtilityClass
public class TranslationRegistryInitializer {
    @SneakyThrows
    public static void registerAllTranslations() {
        TranslationRegistry translationRegistry = TranslationRegistry.create(Key.key("minecraft"));
        Map<String, String> dataMap = OBJECT_MAPPER.readValue(
            TranslationRegistryInitializer.class.getResourceAsStream("/mcdata/language.json"),
            new TypeReference<Map<String, String>>() {});
        dataMap.forEach((key, value) -> translationRegistry.register(key, Locale.ENGLISH, new MessageFormat(value)));
        GlobalTranslator.translator().addSource(translationRegistry);
    }
}
