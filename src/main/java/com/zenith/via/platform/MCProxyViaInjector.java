package com.zenith.via.platform;

import com.viaversion.viaversion.api.platform.ViaInjector;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.viaversion.viaversion.libs.gson.JsonObject;

import static com.zenith.Shared.CONFIG;

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
    public int getServerProtocolVersion() throws RuntimeException {
        int protocolVersion = CONFIG.client.viaversion.protocolVersion;
        if (!ProtocolVersion.isRegistered(protocolVersion)) {
            throw new RuntimeException("Unknown protocol version: " + protocolVersion
                   + ". See https://wiki.vg/Protocol_version_numbers#Versions_after_the_Netty_rewrite for valid versions."
                   + " Example: the protocolVersion for 1.19.4 is 762");
        }
        return protocolVersion;
    }

    @Override
    public JsonObject getDump() {
        // only used for debug
        return new JsonObject();
    }
}
