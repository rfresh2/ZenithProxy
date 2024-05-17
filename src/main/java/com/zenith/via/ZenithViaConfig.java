package com.zenith.via;

import net.raphimc.vialoader.impl.viaversion.VLViaConfig;

import java.io.File;
import java.util.logging.LogManager;

public class ZenithViaConfig extends VLViaConfig {
    public ZenithViaConfig(final File configFile) {
        super(configFile, LogManager.getLogManager().getLogger("ViaVersion"));
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
