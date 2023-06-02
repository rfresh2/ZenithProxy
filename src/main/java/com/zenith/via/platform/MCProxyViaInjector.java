package com.zenith.via.platform;

import com.viaversion.viaversion.api.platform.ViaInjector;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.viaversion.viaversion.libs.gson.JsonObject;

public class MCProxyViaInjector implements ViaInjector {

    public MCProxyViaInjector() {
    }

    @Override
    public void inject() throws Exception {

    }

    @Override
    public void uninject() throws Exception {
        throw new RuntimeException("can't uninject via");
    }

    @Override
    public int getServerProtocolVersion() throws Exception {
        // todo: config property
        return ProtocolVersion.v1_19_4.getVersion();
    }

    @Override
    public JsonObject getDump() {
        // only used for debug
        return new JsonObject();
    }
}
