package com.zenith.feature.queue.mcping.rawData;

import com.google.gson.annotations.SerializedName;

public class ExtraDescription {

    @SuppressWarnings("MismatchedReadAndWriteOfArray")
    @SerializedName("extra")
    private Extra[] extra = new Extra[0];


    public String getText() {
        StringBuilder s = new StringBuilder();
        for (Extra e : extra) {
            s.append(e.getText());
        }
        return s.toString();
    }
}
