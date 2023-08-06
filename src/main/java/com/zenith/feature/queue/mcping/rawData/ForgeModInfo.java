package com.zenith.feature.queue.mcping.rawData;

import com.google.gson.annotations.SerializedName;

public class ForgeModInfo {

    @SerializedName("type")
    private String type;

    @SerializedName("modList")
    private ForgeModListItem[] modList;

    public int getNMods() {
        return modList.length;
    }

    public ForgeModListItem[] getModList() {
        return modList;
    }
}
