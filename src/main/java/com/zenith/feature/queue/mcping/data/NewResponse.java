package com.zenith.feature.queue.mcping.data;

import com.google.gson.annotations.SerializedName;
import com.zenith.feature.queue.mcping.rawData.*;

public class NewResponse extends MCResponse {

    @SerializedName("description")
    private final Description description;
    private String loader;

    @SerializedName("forgeData")
    private ForgeData forgeData;

    @SerializedName("modpackData")
    private ModpackData modpackData;

    public NewResponse() {
        description = new Description();
        players = new Players();
        version = new Version();
    }

    public void setVersion(String a) {
        version.setName(a);
    }

    public ForgeData getForgeData() {
        return forgeData;
    }

    public ModpackData getModpackData() {
        return modpackData;
    }

    public String getLoader() {
        return loader;
    }

    public Description getDescription() {
        return this.description;
    }

    public FinalResponse toFinalResponse() {
        loader = "idc";
        return new FinalResponse(players, version, favicon, description.getText());
    }

}
