package com.zenith.feature.language;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.Getter;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.zenith.Shared.OBJECT_MAPPER;

public class LanguageManager {
    public static final Pattern PATTERN = Pattern.compile("%((\\d+)\\$)?s");
    @Getter
    private Map<String, MessageFormat> languageDataMap = new HashMap<>();

    public LanguageManager() {
        init();
    }

    private void init() {
        try {
            Map<String, String> dataMap = OBJECT_MAPPER.readValue(
                getClass().getResourceAsStream("/pc/1.20.4/language.json"),
                new TypeReference<Map<String, String>>() {
                });
            dataMap.forEach((key, value) -> {
                try {
                    languageDataMap.put(key, convertToMessageFormat(value));
                } catch (final Exception e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    private MessageFormat convertToMessageFormat(String input) {
        // Escape single quotes
        String escapedInput = input.replace("'", "''");

        // Replace positional format codes like %2$s
        Matcher matcher = PATTERN.matcher(escapedInput);
        StringBuffer sb = new StringBuffer();
        int lastIndex = -1;
        while (matcher.find()) {
            int argumentIndex = 0;
            // If the first group is not null, it means a number was found
            if(matcher.group(1) != null) {
                // Check if the second group exists before trying to access it
                String group2 = matcher.group(2);
                if (group2 != null) {
                    argumentIndex = Integer.parseInt(group2) - 1; // The argument index is given by the number preceding the $ sign, minus 1 because indices are 0-based
                    lastIndex = argumentIndex;
                }
            } else {
                argumentIndex = ++lastIndex; // increment lastIndex and use it
            }
            matcher.appendReplacement(sb, "{" + argumentIndex + "}");
        }
        matcher.appendTail(sb);

        // Create MessageFormat object
        return new MessageFormat(sb.toString());
    }
}
