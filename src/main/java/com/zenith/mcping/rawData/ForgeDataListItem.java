package com.zenith.mcping.rawData;

import com.google.gson.annotations.SerializedName;

public class ForgeDataListItem {
    @SerializedName("modId")
    private String modid;

    @SerializedName("modmarker")
    private String version;

    public String getModid() {
        return modid;
    }

    public String getVersion() {
        return version;
    }
}
