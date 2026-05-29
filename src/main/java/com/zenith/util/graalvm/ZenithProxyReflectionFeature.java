package com.zenith.util.graalvm;

import java.util.List;

public class ZenithProxyReflectionFeature extends AbstractGraalVMReflectionFeature {
    public ZenithProxyReflectionFeature() {
        super(List.of(
            "com.viaversion.nbt",
            "com.viaversion.viabackwards.api",
            "com.viaversion.viabackwards.protocol",
            "com.viaversion.viarewind.api",
            "com.viaversion.viarewind.protocol",
            "com.viaversion.viaversion.api",
            "com.viaversion.viaversion.protocols",
            "com.zenith.database.dto",
            "com.zenith.event",
            "com.zenith.feature.api",
            "com.zenith.feature.queue.mcping",
            "com.zenith.terminal",
            "com.zenith.util.config",
            "com.zenith.via"
        ));
    }
}
