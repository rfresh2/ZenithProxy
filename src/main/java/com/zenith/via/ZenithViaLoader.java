package com.zenith.via;

import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.viaversion.viaversion.api.protocol.version.VersionProvider;
import net.raphimc.vialoader.impl.viaversion.VLLoader;
import org.geysermc.mcprotocollib.protocol.codec.MinecraftCodec;

import static com.zenith.Shared.CONFIG;

public class ZenithViaLoader extends VLLoader {

    @Override
    public void load() {
        Via.getManager().getProviders().use(VersionProvider.class, (connection) ->
            connection.isClientSide()
                ? ProtocolVersion.getProtocol(CONFIG.client.viaversion.protocolVersion)
                : ProtocolVersion.getProtocol(MinecraftCodec.CODEC.getProtocolVersion()));
    }
}
