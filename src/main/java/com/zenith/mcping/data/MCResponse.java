package com.zenith.mcping.data;

import com.google.gson.annotations.SerializedName;
import com.zenith.mcping.rawData.Players;
import com.zenith.mcping.rawData.Version;

class MCResponse {
    @SerializedName("players")
    Players players;
    @SerializedName("version")
    Version version;
    @SerializedName("favicon")
    String favicon;
}
