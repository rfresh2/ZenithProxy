package com.zenith.feature.queue.mcping.rawData;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;

import java.util.List;

@Getter
public class Player2b2t {
    @SerializedName("name")
    private String name;
    @SerializedName("id")
    private List<Integer> id;
}
