package com.zenith.via;

import com.viaversion.viaversion.platform.NoopInjector;
import com.viaversion.viaversion.platform.ViaCodecHandler;

public class ZenithViaInjector extends NoopInjector {

    @Override
    public String getEncoderName() {
        return ViaCodecHandler.NAME;
    }

    @Override
    public String getDecoderName() {
        return ViaCodecHandler.NAME;
    }
}
