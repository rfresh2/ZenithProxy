package com.zenith.feature.queue.mcping.rawData;

import com.google.gson.annotations.SerializedName;

public class ModpackData {
    @SerializedName("projectID")
    private String projectID;

    @SerializedName("name")
    private String name;

    @SerializedName("version")
    private String version;

    @SerializedName("versionID")
    private String versionID;

    @SerializedName("isMetadata")
    private boolean isMetadata;

    public String getProjectID() {
        return projectID;
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public String getVersionID() {
        return versionID;
    }

    public boolean isMetadata() {
        return isMetadata;
    }
}
