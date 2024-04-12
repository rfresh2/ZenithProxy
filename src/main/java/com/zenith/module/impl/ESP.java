package com.zenith.module.impl;

import com.zenith.feature.esp.GlowingEntityMetadataPacketHandler;
import com.zenith.module.Module;
import com.zenith.network.registry.PacketHandlerCodec;
import com.zenith.network.registry.PacketHandlerStateCodec;
import com.zenith.network.registry.ZenithHandlerCodec;
import com.zenith.network.server.ServerConnection;
import org.geysermc.mcprotocollib.protocol.data.ProtocolState;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundSetEntityDataPacket;

import static com.zenith.Shared.CONFIG;

public class ESP extends Module {
    private final PacketHandlerCodec codec;

    public ESP() {
        codec = PacketHandlerCodec.builder()
            .setId("esp")
            .setPriority(1000)
            .state(ProtocolState.GAME, PacketHandlerStateCodec.<ServerConnection>builder()
                .registerOutbound(ClientboundSetEntityDataPacket.class, new GlowingEntityMetadataPacketHandler())
                .build())
            .build();
    }

    @Override
    public void subscribeEvents() {}

    @Override
    public boolean shouldBeEnabled() {
        return CONFIG.server.extra.esp.enable;
    }

    @Override
    public void onEnable() {
        ZenithHandlerCodec.SERVER_REGISTRY.register(codec);
    }

    @Override
    public void onDisable() {
        ZenithHandlerCodec.SERVER_REGISTRY.unregister(codec);
    }
}
