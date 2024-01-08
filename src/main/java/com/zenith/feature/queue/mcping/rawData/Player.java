package com.zenith.feature.queue.mcping.rawData;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Player {
    @SerializedName("name")
    private String name;
    @SerializedName("id")
    private String id;
    public Player() {}
}
