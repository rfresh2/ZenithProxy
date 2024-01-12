package com.zenith.via;

import net.raphimc.vialoader.ViaLoader;
import net.raphimc.vialoader.impl.platform.ViaBackwardsPlatformImpl;

import java.util.concurrent.atomic.AtomicBoolean;

public class ZenithViaInitializer {
    private final AtomicBoolean initialized = new AtomicBoolean(false);

    public void init() {
        if (this.initialized.get()) {
            return;
        }
        ViaLoader.init(
            null,
            new ZenithViaLoader(),
            null,
            null,
            ViaBackwardsPlatformImpl::new
        );
        this.initialized.set(true);
    }
}
