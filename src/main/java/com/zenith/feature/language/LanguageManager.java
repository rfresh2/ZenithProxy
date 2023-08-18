package com.zenith.feature.language;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.Map;

public class LanguageManager {
    private final ObjectMapper objectMapper;
    private Map<String, String> languageDataMap = Collections.emptyMap();

    public LanguageManager() {
        this.objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        init();
    }

    private void init() {
        try {
            this.languageDataMap = objectMapper.readValue(
                getClass().getResourceAsStream("/pc/1.12/language.json"),
                new TypeReference<Map<String, String>>() {});
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Map<String, String> getLanguageDataMap() {
        return this.languageDataMap;
    }
}
