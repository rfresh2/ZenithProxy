package com.zenith.feature.queue.mcping.rawData;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class Version {
    @SerializedName("name")
    private String name;
    @SerializedName("protocol")
    private int protocol = Integer.MIN_VALUE; // Don't use -1 as this has special meaning
}
