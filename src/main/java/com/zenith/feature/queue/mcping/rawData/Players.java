package com.zenith.feature.queue.mcping.rawData;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Data
public class Players {
    @Getter
    @SerializedName("max")
    private int max;
    @Getter
    @SerializedName("online")
    private int online;
    @SerializedName("sample")
    private List<Player> sample = new ArrayList<>();
}
