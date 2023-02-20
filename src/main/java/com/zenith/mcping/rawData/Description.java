package com.zenith.mcping.rawData;

import com.google.gson.annotations.SerializedName;

public class Description {
    @SerializedName("text")
    private String text;

    public String getText() {
        return this.text;
    }
}
