package com.zenith.via.platform;

import com.viaversion.viabackwards.api.ViaBackwardsPlatform;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.velocity.util.LoggerWrapper;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.logging.Logger;

public class MCProxyViaBackwardsPlatform implements ViaBackwardsPlatform {
    private java.util.logging.Logger logger = new LoggerWrapper(LoggerFactory.getLogger("ViaBackwards"));
    private final File configDirectory;

    public MCProxyViaBackwardsPlatform(final File configDirectory) {
        this.configDirectory = configDirectory;
    }

    public void initViaBackwards() {
        Via.getManager().addEnableListener(() -> this.init(getDataFolder()));
    }

    @Override
    public Logger getLogger() {
        return logger;
    }

    @Override
    public void disable() {
        // Not possible
    }

    @Override
    public File getDataFolder() {
        return configDirectory;
    }
}
