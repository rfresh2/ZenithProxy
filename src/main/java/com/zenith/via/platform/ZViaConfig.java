package com.zenith.via.platform;

import com.viaversion.viaversion.configuration.AbstractViaConfig;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ZViaConfig extends AbstractViaConfig {
    public ZViaConfig(File configFile) {
        super(configFile);
    }

    @Override
    protected void handleConfig(Map<String, Object> config) {

    }

    @Override
    public List<String> getUnsupportedOptions() {
        // todo: come back and check this
        return Collections.emptyList();
    }
}
