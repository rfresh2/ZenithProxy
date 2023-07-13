package com.zenith.feature.queue.mcping.data;

import com.google.gson.annotations.SerializedName;
import com.zenith.feature.queue.mcping.rawData.ExtraDescription;

public class ExtraResponse extends MCResponse {

    @SerializedName("description")
    private ExtraDescription description = new ExtraDescription();

    public FinalResponse toFinalResponse() {
        return new FinalResponse(players, this.version, favicon, description.getText());
    }

}
