package com.zenith.via;

import net.raphimc.vialoader.impl.viaversion.VLViaConfig;

import java.io.File;

public class ZenithViaConfig extends VLViaConfig {
    public ZenithViaConfig(final File configFile) {
        super(configFile);
    }

    @Override
    public int getMaxPPS() {
        return -1;
    }

    @Override
    public int getTrackingPeriod() {
        return -1;
    }
}
