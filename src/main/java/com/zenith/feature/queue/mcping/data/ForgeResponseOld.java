package com.zenith.feature.queue.mcping.data;

import com.google.gson.annotations.SerializedName;
import com.zenith.feature.queue.mcping.rawData.ForgeModInfo;
import com.zenith.feature.queue.mcping.rawData.Players;
import com.zenith.feature.queue.mcping.rawData.Version;

public class ForgeResponseOld {

    @SerializedName("description")
    private String description;

    @SerializedName("players")
    private Players players;

    @SerializedName("version")
    private Version version;

    @SerializedName("modinfo")
    private ForgeModInfo modinfo;

    public FinalResponse toFinalResponse() {
        return new FinalResponse(players, version, "", description);
    }

    public String getDescription() {
        return description;
    }

    public Players getPlayers() {
        return players;
    }

    public Version getVersion() {
        return version;
    }

    public ForgeModInfo getModinfo() {
        return modinfo;
    }
}
