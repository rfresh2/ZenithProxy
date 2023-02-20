package com.zenith.mcping.data;

import com.zenith.mcping.rawData.Players;
import com.zenith.mcping.rawData.Version;

import java.util.regex.Pattern;

public class FinalResponse extends MCResponse {

    private static final Pattern STRIP_PATTERN = Pattern.compile("(?<!<@)[&§](?i)[0-9a-fklmnorx]");
    private final String description;

    public FinalResponse(Players players, Version version, String favicon, String description) {
        this.description = stripMinecraft(description);
        this.favicon = favicon;
        this.players = players;
        this.version = version;
    }

    public static String stripMinecraft(String input) {
        return input == null ? "" : STRIP_PATTERN.matcher(input).replaceAll("").trim();
    }

    public Players getPlayers() {
        if (players == null) {
            return new Players();
        }
        return players;
    }

    public Version getVersion() {
        return version;
    }

    public String getDescription() {
        return description;
    }

    public String getFavIcon() {
        return favicon;
    }
}
