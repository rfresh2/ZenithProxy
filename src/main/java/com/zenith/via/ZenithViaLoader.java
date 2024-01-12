package com.zenith.via;

import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.protocol.version.VersionProvider;
import net.raphimc.vialoader.impl.viaversion.VLLoader;

import static com.zenith.Shared.CONFIG;

public class ZenithViaLoader extends VLLoader {

    @Override
    public void load() {
        Via.getManager().getProviders().use(VersionProvider.class, (connection) -> CONFIG.client.viaversion.protocolVersion);
    }

    @Override
    public void unload() {
    }

}
