package com.zenith.feature.queue.mcping.rawData;

import com.google.gson.annotations.SerializedName;

public class ForgeModListItem {

    @SerializedName("modid")
    private String modid;

    @SerializedName("version")
    private String version;

    public String getModid() {
        return modid;
    }

    public String getVersion() {
        return version;
    }
}
