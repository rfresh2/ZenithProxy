package com.zenith.feature.queue.mcping.rawData;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class Players {
    @SerializedName("max")
    private int max;
    @SerializedName("online")
    private int online;
    @SerializedName("sample")
    private List<Player> sample;

    public int getMax() {
        return this.max;
    }

    public void setMax(int max) {
        this.max = max;
    }

    public int getOnline() {
        return this.online;
    }

    public void setOnline(int online) {
        this.online = online;
    }

    public List<Player> getSample() {
        return this.sample == null ? new ArrayList<>() : this.sample;
    }

    public void setSample(List<Player> sample) {
        this.sample = sample;
    }
}
