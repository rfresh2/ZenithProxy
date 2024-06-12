package com.zenith.generator;

import com.google.gson.reflect.TypeToken;
import com.zenith.DataGenerator;
import it.unimi.dsi.fastutil.objects.ObjectSet;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Language implements Generator {
    public static final Pattern PATTERN = Pattern.compile("%((\\d+)\\$)?s");
    // filtering out entries that will likely not be received by chat or title packets
    // runtime memory optimization
    static ObjectSet<String> prefixes = ObjectSet.of(
        "advancement",
        "advancements",
        "argument",
        "build",
        "chat",
        "clear",
        "command",
        "commands",
        "death",
        "disconnect",
        "entity",
        "gameMode",
        "item_modifier",
        "multiplayer",
        "parsing",
        "particle",
        "recipe",
        "record",
        "sleep",
        "slot"
    );

    @Override
    public void generate() {
        try {
            String rawJson = new String(Language.class.getResourceAsStream("/assets/minecraft/lang/en_us.json").readAllBytes(), StandardCharsets.UTF_8);
            Map<String, String> json = (Map<String, String>) DataGenerator.gson.fromJson(rawJson, TypeToken.getParameterized(Map.class, String.class, String.class));
            for (var it = json.entrySet().iterator(); it.hasNext(); ) {
                var entry = it.next();
                var prefix = entry.getKey().split("\\.")[0];
                if (!prefixes.contains(prefix)) {
                    it.remove();
                }
            }

            // Transform MC's language map values into java MessageFormat objects
            json.replaceAll((k, v) -> convertToMessageFormat(v));

            byte[] bytes = DataGenerator.gson.toJson(json).getBytes(StandardCharsets.UTF_8);
            Files.write(
                DataGenerator.outputFile("language.json").toPath(),
                bytes,
                StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING
            );
            DataGenerator.LOG.info("Dumped language.json");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // https://docs.oracle.com/javase/8/docs/api/java/text/MessageFormat.html
    private String convertToMessageFormat(String input) {
        // Escape single quotes
        String escapedInput = input.replace("'", "''");

        // Replace positional format codes like %2$s
        Matcher matcher = PATTERN.matcher(escapedInput);
        StringBuilder sb = new StringBuilder();
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
        return sb.toString();
    }
}
