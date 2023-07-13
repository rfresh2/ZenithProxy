package com.zenith.feature.queue.mcping.data;

import com.google.gson.annotations.SerializedName;
import com.zenith.feature.queue.mcping.rawData.Players;
import com.zenith.feature.queue.mcping.rawData.Version;

class MCResponse {
    @SerializedName("players")
    Players players;
    @SerializedName("version")
    Version version;
    @SerializedName("favicon")
    String favicon;
}
