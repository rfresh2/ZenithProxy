package com.zenith.feature.queue.mcping.rawData;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class Extra {

    @SerializedName("color")
    private String color;
    @SerializedName("bold")
    private boolean bold;
    @SerializedName("text")
    private String text;

}
