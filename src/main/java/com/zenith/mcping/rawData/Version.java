package com.zenith.mcping.rawData;

import com.google.gson.annotations.SerializedName;

public class Version {
    @SerializedName("name")
    private String name;
    @SerializedName("protocol")
    private int protocol = Integer.MIN_VALUE; // Don't use -1 as this has special meaning

    public String getName() {
        return this.name;
    }

    public void setName(String a) {
        name = a;
    }

    public int getProtocol() {
        return protocol;
    }

    public void setProtocol(int protocol) {
        this.protocol = protocol;
    }
}
