package com.zenith.via;

import com.viaversion.viabackwards.protocol.v1_20_5to1_20_3.provider.TransferProvider;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.viaversion.viaversion.api.protocol.version.VersionProvider;
import com.zenith.Proxy;
import net.raphimc.vialoader.impl.viaversion.VLLoader;
import org.geysermc.mcprotocollib.protocol.codec.MinecraftCodec;

import static com.zenith.Shared.*;

public class ZenithViaLoader extends VLLoader {

    @Override
    public void load() {
        Via.getManager().getProviders().use(VersionProvider.class, (connection) ->
            connection.isClientSide()
                ? ProtocolVersion.getProtocol(CONFIG.client.viaversion.protocolVersion)
                : ProtocolVersion.getProtocol(MinecraftCodec.CODEC.getProtocolVersion()));
        Via.getManager().getProviders().use(TransferProvider.class, (connection, host, port) -> {
            if (connection.isClientSide()) {
                CLIENT_LOG.info("ViaVersion requested client transfer to {}:{}", host, port);
                Proxy.getInstance().disconnect();
                Proxy.getInstance().connect(host, port);
            } else {
                SERVER_LOG.info("ViaVersion requested server connection transfer to {}:{}", host, port);
                SERVER_LOG.info("...but we can't transfer players");
                // ???
            }
        });
    }
}
