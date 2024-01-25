package com.zenith.util;

import javax.annotation.Nullable;

public class LaunchConfig {
    public boolean auto_update = true;
    public boolean auto_update_launcher = true;
    public String release_channel = "java.1.20.1";
    public @Nullable String getMcVersion() {
        try {
            return release_channel.substring(release_channel.indexOf('.') + 1);
        } catch (final Exception e) {
            return null;
        }
    }
    public String version = "0.0.0";
    public String local_version = "0.0.0";
    public String repo_owner = "rfresh2";
    public String repo_name = "ZenithProxy";
}
