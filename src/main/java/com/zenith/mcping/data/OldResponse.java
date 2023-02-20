package com.zenith.mcping.data;

import com.google.gson.annotations.SerializedName;

public class OldResponse extends MCResponse {
    @SerializedName("description")
    private String description;

    public FinalResponse toFinalResponse() {
        return new FinalResponse(players, version, favicon, description);
    }
}
