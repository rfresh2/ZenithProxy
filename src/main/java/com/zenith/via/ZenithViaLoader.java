package com.zenith.via;

import com.github.steveice10.mc.protocol.codec.MinecraftCodec;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.protocol.version.VersionProvider;
import net.raphimc.vialoader.impl.viaversion.VLLoader;

import static com.zenith.Shared.CONFIG;

public class ZenithViaLoader extends VLLoader {

    @Override
    public void load() {
        Via.getManager().getProviders().use(VersionProvider.class, (connection) ->
            connection.isClientSide()
                ? CONFIG.client.viaversion.protocolVersion
                : MinecraftCodec.CODEC.getProtocolVersion());
    }
}
