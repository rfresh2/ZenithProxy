package com.zenith.mcping.data;

import com.google.gson.annotations.SerializedName;
import com.zenith.mcping.rawData.Description;
import com.zenith.mcping.rawData.ForgeModInfo;
import com.zenith.mcping.rawData.Players;
import com.zenith.mcping.rawData.Version;

public class ForgeResponse {

    @SerializedName("description")
    private Description description;

    @SerializedName("players")
    private Players players;

    @SerializedName("version")
    private Version version;

    @SerializedName("modinfo")
    private ForgeModInfo modinfo;

    public FinalResponse toFinalResponse() {
        version.setName(version.getName());
        return new FinalResponse(players, version, "", description.getText());
    }

    public Description getDescription() {
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
