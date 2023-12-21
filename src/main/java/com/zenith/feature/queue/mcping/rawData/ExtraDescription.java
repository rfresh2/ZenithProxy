package com.zenith.feature.queue.mcping.rawData;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class ExtraDescription {

    @SerializedName("extra")
    private Extra[] extra = new Extra[0];
    @SerializedName("text")
    private String text = "";
}
