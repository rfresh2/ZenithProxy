package com.zenith.mcping.rawData;

import com.google.gson.annotations.SerializedName;

public class ForgeData {

    @SerializedName("mods")
    private ForgeDataListItem[] modList;

    public int getNMods() {
        return modList.length;
    }

    public ForgeDataListItem[] getModList() {
        return modList;
    }
}
