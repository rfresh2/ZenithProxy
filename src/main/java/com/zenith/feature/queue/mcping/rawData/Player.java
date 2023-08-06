package com.zenith.feature.queue.mcping.rawData;

import com.google.gson.annotations.SerializedName;

public class Player {

    @SerializedName("name")
    private String name;
    @SerializedName("id")
    private String id;

    public String getName() {
        return this.name;
    }

    public String getId() {
        return this.id;
    }
}
