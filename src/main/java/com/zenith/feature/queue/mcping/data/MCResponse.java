package com.zenith.feature.queue.mcping.data;

import com.zenith.feature.queue.mcping.rawData.Players;
import com.zenith.feature.queue.mcping.rawData.Version;

import java.util.regex.Pattern;

public record MCResponse(Players players, Version version, String favicon, String description) {

    private static final Pattern STRIP_PATTERN = Pattern.compile("(?<!<@)[&§](?i)[0-9a-fklmnorx]");

    public MCResponse(Players players, Version version, String favicon, String description) {
        this.description = stripMinecraft(description);
        this.favicon = favicon;
        this.players = players;
        this.version = version;
    }

    public static String stripMinecraft(String input) {
        return input == null ? "" : STRIP_PATTERN.matcher(input).replaceAll("").trim();
    }
}
