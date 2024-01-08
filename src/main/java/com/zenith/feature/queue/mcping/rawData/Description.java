package com.zenith.feature.queue.mcping.rawData;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class Description {
    @SerializedName("text")
    private String text;
}
