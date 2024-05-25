package com.zenith.feature.queue.mcping.data;

import com.zenith.feature.queue.mcping.rawData.Players;
import com.zenith.feature.queue.mcping.rawData.Version;
import lombok.Getter;

import java.util.regex.Pattern;

@Getter
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

    public String getFavIcon() {
        return favicon;
    }
}
